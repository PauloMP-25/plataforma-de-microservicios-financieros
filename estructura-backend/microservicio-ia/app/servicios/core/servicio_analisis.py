"""
servicios/servicio_analisis.py  ·  v4 — IA Centrada en Datos (LUKA)
══════════════════════════════════════════════════════════════════════════════
Orquestador principal: recibe la petición HTTP del router, coordina el flujo
completo y retorna la RespuestaModulo lista para serializar.

Flujo por cada módulo:
  1. Obtiene el historial de transacciones del microservicio-nucleo-financiero.
  2. Normaliza el JSON a DataFrame con preparador_datos.
  3. Valida que haya datos suficientes.
  4. Llama al módulo correspondiente del Motor Analítico → InsightAnalitico.
  5. Pasa el InsightAnalitico al CoachIA → consejo (o None si Gemini falla).
  6. Registra el evento en microservicio-auditoria (no bloqueante).
  7. Retorna RespuestaModulo — siempre completa, con o sin consejo.

Garantía de degradación elegante:
  Si Gemini falla (cuota agotada, API Key inválida, timeout, etc.),
  la RespuestaModulo se retorna igual con consejo=None y estado_coach
  indicando el motivo. El insight, gráfico y kpi SIEMPRE están presentes.

Este archivo es el único que conoce tanto al Motor como al Coach.
Los routers solo llaman a este servicio; nunca hablan directamente con Gemini
ni con Pandas.

Patrón de cada método público:
    ejecutar_X(peticion, ip_origen) -> RespuestaModulo
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

from datetime import datetime
import logging
from typing import Optional

from dateutil.relativedelta import relativedelta

from app.libreria_comun.excepciones.base import LukaException, ValidacionError
from app.libreria_comun.modelos.eventos import EventoAuditoriaDTO
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.mensajeria.publicador_auditoria import publicador_auditoria
from app.configuracion import Configuracion, obtener_configuracion
from app.modelos.esquemas import (
    PeticionClasificar,
    PeticionConFiltroFecha,
    PeticionSimularEscenario,
    PeticionSimularMeta,
    RespuestaModulo,
    TipoMovimiento,
    EstadoCoach,
)
from app.servicios.analitica import motor_ia
from app.servicios.ia.coach_ia import CoachIA
from app.servicios.modulos.gasto_hormiga import GastoHormigaService

from app.utilidades.preparador_datos import (
    json_a_dataframe,
    validar_datos_suficientes,
)
from app.clientes.luka_clients import (
    obtener_cliente_financiero,
    obtener_cliente_perfil
)

logger = logging.getLogger(__name__)


class ServicioAnalisis:
    """
    Orquestador de los 10 módulos de análisis de LUKA.

    Instanciar una sola vez (singleton en el router).
    Todas sus dependencias son inyectadas en __init__ para facilitar testing.
    """

    def __init__(self) -> None:
        self._config: Configuracion = obtener_configuracion()
        self._cliente_financiero = obtener_cliente_financiero()
        self._cliente_perfil = obtener_cliente_perfil()
        self._coach = CoachIA()
        self._gasto_hormiga_service = GastoHormigaService()
        self._publicador = publicador_auditoria

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 1 — Autoclasificación
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_clasificar(
        self,
        peticion: PeticionClasificar,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Clasifica o valida la categoría de una transacción específica
        comparándola contra el historial del usuario.
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
        )

        insight = motor_ia.analizar_clasificacion(
            df=df,
            config=self._config,
            descripcion=peticion.transaccion.descripcion,
            categoria_actual=peticion.transaccion.categoria_actual,
            monto=peticion.transaccion.monto,
        )

        # Obtener perfil para personalización del Coach
        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)

        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=None,
        )

        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="CLASIFICACION_EJECUTADA",
            detalles=f"Transacción: '{peticion.transaccion.descripcion}' | "
                     f"Categoría actual: {peticion.transaccion.categoria_actual}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 2 — Predicción de Gastos
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_predecir_gastos(
        self,
        peticion: PeticionConFiltroFecha,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Proyecta el gasto del próximo mes usando regresión lineal o media móvil
        sobre el historial del usuario.
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
            mes=peticion.mes,
            anio=peticion.anio,
        )

        self._verificar_datos(df, minimo_filas=3, tipo=TipoMovimiento.GASTO)

        insight, grafico = motor_ia.analizar_prediccion_gastos(
            df=df,
            config=self._config,
        )

        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=grafico,
        )

        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="PREDICCION_GASTOS_EJECUTADA",
            detalles=f"Proyección calculada: S/ {insight.hallazgos.get('gasto_proyectado', 0):,.2f}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 3 — Detección de Anomalías
    # ══════════════════════════════════════════════════════════════════════════

async def ejecutar_detectar_anomalias(self,peticion: PeticionConFiltroFecha,ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
    
    # 1. Calculamos el rango: Mes solicitado + 6 meses atrás
    mes_final = peticion.mes or datetime.now().month
    anio_final = peticion.anio or datetime.now().year
    fecha_fin = datetime(anio_final, mes_final, 1) + relativedelta(months=1) - relativedelta(seconds=1)
    # Restamos 6 meses para tener historial de referencia
    fecha_inicio = datetime(anio_final, mes_final, 1) - relativedelta(months=6)

    # 2. Obtenemos el DataFrame con el rango extendido
    # Nota: Usamos el cliente financiero con las fechas calculadas
    respuesta_json = self._cliente_financiero.obtener_historial_transacciones(
        usuario_id=peticion.usuario_id,
        token=peticion.token,
        tamanio=peticion.tamanio_pagina,
        desde=fecha_inicio.isoformat(),
        hasta=fecha_fin.isoformat()
    )
    df = json_a_dataframe(respuesta_json)

    # 3. Verificación de datos mínimos para el cálculo estadístico
    self._verificar_datos(df, minimo_filas=5, tipo=TipoMovimiento.GASTO)

    # 4. Llamada al motor (Ahora los nombres coinciden)
    insight, grafico = motor_ia.analizar_anomalias(
        df=df,
        config=self._config,
    )

    # 5. Generar respuesta con el Coach (Gemini)
    perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
    respuesta = self._coach.generar_respuesta(
        usuario_id=peticion.usuario_id,
        insight=insight,
        perfil=perfil,
        grafico=grafico,
    )

    # 6. Auditoría
    await self._auditar(
        usuario_id=peticion.usuario_id,
        accion="ANOMALIAS_DETECTADAS",
        detalles=f"Anomalías: {insight.hallazgos.get('total_anomalias', 0)} en rango 6 meses",
        ip_origen=ip_origen,
    )
    
    return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 4 — Optimización de Suscripciones / Gastos Hormiga
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_optimizar_suscripciones(
        self,
        peticion: PeticionConFiltroFecha,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Detecta gastos pequeños y recurrentes ('gastos hormiga') y calcula
        su impacto mensual y anual acumulado.
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
            mes=peticion.mes,
            anio=peticion.anio,
        )

        self._verificar_datos(df, minimo_filas=1, tipo=TipoMovimiento.GASTO)

        insight, grafico = motor_ia.analizar_suscripciones(
            df=df,
            config=self._config,
        )

        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=grafico,
        )

        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="SUSCRIPCIONES_ANALIZADAS",
            detalles=f"Impacto anual estimado: S/ {insight.hallazgos.get('impacto_anual_estimado', 0):,.2f}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 5 — Capacidad de Ahorro
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_capacidad_ahorro(
        self,
        peticion: PeticionConFiltroFecha,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Calcula la capacidad de ahorro real del universitario y la compara
        con la regla 50/30/20.
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
            mes=peticion.mes,
            anio=peticion.anio,
        )

        self._verificar_datos(df, minimo_filas=1)

        insight, grafico = motor_ia.analizar_capacidad_ahorro(
            df=df,
            config=self._config,
        )

        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=grafico,
        )

        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="CAPACIDAD_AHORRO_CALCULADA",
            detalles=f"Ahorro real: {insight.hallazgos.get('porcentaje_ahorro_real', 0)}% | "
                     f"Cumple 50/30/20: {insight.hallazgos.get('cumple_regla_50_30_20', False)}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 6 — Simulación de Meta
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_simular_meta(
        self,
        peticion: PeticionSimularMeta,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Proyecta el tiempo necesario para alcanzar una meta de ahorro específica
        basándose en la capacidad de ahorro real del historial.
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
        )

        self._verificar_datos(df, minimo_filas=1)

        insight, grafico = motor_ia.analizar_simular_meta(
            df=df,
            config=self._config,
            nombre_meta=peticion.nombre_meta,
            monto_objetivo=peticion.monto_objetivo,
            monto_actual_ahorrado=peticion.monto_actual_ahorrado,
            aporte_mensual_deseado=peticion.aporte_mensual_deseado,
        )

        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=grafico,
        )

        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="META_SIMULADA",
            detalles=f"Meta: '{peticion.nombre_meta}' | "
                     f"Objetivo: S/ {peticion.monto_objetivo:,.2f} | "
                     f"Meses proyectados: {insight.hallazgos.get('meses_para_alcanzar', '?')}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 7 — Estacionalidad
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_estacionalidad(
        self,
        peticion: PeticionConFiltroFecha,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Detecta patrones cíclicos de gasto a lo largo de los meses.
        Ideal para planificar períodos de alto gasto (inicio de ciclo, fiestas, etc.).
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
            mes=peticion.mes,
            anio=peticion.anio,
        )

        self._verificar_datos(df, minimo_filas=2)

        insight, grafico = motor_ia.analizar_estacionalidad(
            df=df,
            config=self._config,
        )

        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=grafico,
        )

        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="ESTACIONALIDAD_ANALIZADA",
            detalles=f"CV estacional: {insight.hallazgos.get('coeficiente_variacion_pct', 0)}% | "
                     f"Mes mayor gasto: {insight.hallazgos.get('mes_mayor_gasto', '?')}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 8 — Presupuesto Dinámico
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_presupuesto_dinamico(
        self,
        peticion: PeticionConFiltroFecha,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Genera un presupuesto semanal y diario realista desglosado por categoría,
        calculado desde el comportamiento histórico real del universitario.
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
            mes=peticion.mes,
            anio=peticion.anio,
        )

        self._verificar_datos(df, minimo_filas=1, tipo=TipoMovimiento.GASTO)

        insight, grafico = motor_ia.analizar_presupuesto_dinamico(
            df=df,
            config=self._config,
        )

        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=grafico,
        )

        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="PRESUPUESTO_GENERADO",
            detalles=f"Presupuesto semanal: S/ {insight.hallazgos.get('presupuesto_semanal_total', 0):,.2f} | "
                     f"Días restantes del mes: {insight.hallazgos.get('dias_restantes_mes', '?')}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 9 — Simulación de Escenario
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_simular_escenario(
        self,
        peticion: PeticionSimularEscenario,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Calcula el impacto financiero de un cambio hipotético
        (nuevo gasto, nueva suscripción, aumento de ingreso, etc.).
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
            mes=peticion.mes,
            anio=peticion.anio,
        )

        self._verificar_datos(df, minimo_filas=1)

        insight, grafico = motor_ia.analizar_escenario(
            df=df,
            config=self._config,
            descripcion_escenario=peticion.descripcion_escenario,
            monto_cambio=peticion.monto_cambio,
            tipo_cambio=peticion.tipo_cambio,
            recurrente=peticion.recurrente,
        )

        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=grafico,
        )

        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="ESCENARIO_SIMULADO",
            detalles=f"Escenario: '{peticion.descripcion_escenario}' | "
                     f"Impacto mensual: S/ {insight.hallazgos.get('impacto_mensual', 0):,.2f}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÓDULO 10 — Reporte Completo
    # ══════════════════════════════════════════════════════════════════════════

    async def ejecutar_reporte_completo(
        self,
        peticion: PeticionConFiltroFecha,
        ip_origen: str = "127.0.0.1",
    ) -> RespuestaModulo:
        """
        Genera el reporte ejecutivo mensual completo: KPIs, alertas,
        oportunidades y score de salud financiera.
        """
        df = self._obtener_dataframe(
            usuario_id=peticion.usuario_id,
            token=peticion.token,
            tamanio=peticion.tamanio_pagina,
            mes=peticion.mes,
            anio=peticion.anio,
        )

        self._verificar_datos(df, minimo_filas=1)

        insight, grafico = motor_ia.analizar_reporte_completo(
            df=df,
            config=self._config,
        )

        perfil = self._cliente_perfil.obtener_perfil_usuario(peticion.usuario_id, peticion.token)
        respuesta = self._coach.generar_respuesta(
            usuario_id=peticion.usuario_id,
            insight=insight,
            perfil=perfil,
            grafico=grafico,
        )

        score = insight.hallazgos.get("salud_financiera_score", 0)
        clasificacion = insight.hallazgos.get("clasificacion_salud", "")
        await self._auditar(
            usuario_id=peticion.usuario_id,
            accion="REPORTE_COMPLETO_GENERADO",
            detalles=f"Score salud financiera: {score} ({clasificacion}) | "
                     f"Período: {insight.periodo_analizado}",
            ip_origen=ip_origen,
        )
        return respuesta

    # ══════════════════════════════════════════════════════════════════════════
    # MÉTODOS DE SOPORTE INTERNOS
    # ══════════════════════════════════════════════════════════════════════════

    def _obtener_dataframe(
        self,
        usuario_id: str,
        token: str,
        tamanio: int = 200,
        mes: Optional[int] = None,
        anio: Optional[int] = None,
    ):
        """
        Consulta el historial al microservicio-nucleo-financiero
        y lo normaliza a DataFrame usando el preparador de datos.

        Centraliza la llamada HTTP para que ningún módulo
        acceda directamente al cliente financiero.
        """
        logger.info(
            "[SERVICIO] Obteniendo historial | usuario=%s | mes=%s | anio=%s",
            usuario_id, mes, anio,
        )
        respuesta_json = self._cliente_financiero.obtener_historial_transacciones(
            usuario_id=usuario_id,
            token=token,
            tamanio=tamanio,
            mes=mes,
            anio=anio,
        )
        return json_a_dataframe(respuesta_json)

    def _verificar_datos(
        self,
        df,
        minimo_filas: int = 1,
        tipo: Optional[TipoMovimiento] = None,
    ) -> None:
        """
        Valida que el DataFrame tenga suficientes datos para el análisis.
        Lanza ValidacionError si no los tiene.
        """
        valido, mensaje = validar_datos_suficientes(df, minimo_filas, tipo)
        if not valido:
            logger.warning("[SERVICIO] Datos insuficientes: %s", mensaje)
            raise ValidacionError(mensaje)

    async def _auditar(
        self,
        usuario_id: str,
        accion: str,
        detalles: str,
        ip_origen: str,
    ) -> None:
        """
        Registra el evento en microservicio-auditoria vía RabbitMQ de forma no bloqueante.
        """
        try:
            evento = EventoAuditoriaDTO(
                usuario_id=usuario_id,
                accion=accion,
                modulo="MS-IA",
                ip_origen=ip_origen,
                detalles=detalles
            )
            # Publicamos de forma asíncrona
            await self._publicador.publicar_evento(evento)
            
        except Exception as exc:
            logger.warning(
                "[SERVICIO] Auditoría vía RabbitMQ falló para accion=%s: %s",
                accion, str(exc),
            )

def obtener_servicio_analisis() -> ServicioAnalisis:
    """Proveedor para inyección de dependencias en FastAPI."""
    return ServicioAnalisis()
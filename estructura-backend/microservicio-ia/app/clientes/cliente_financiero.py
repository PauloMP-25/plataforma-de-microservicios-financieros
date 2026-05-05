"""
clientes/cliente_financiero.py
Cliente HTTP para comunicación con los microservicios Java.
Usa httpx con manejo de errores y timeouts.
"""

from datetime import datetime
from wsgiref import headers

import httpx
import logging
from typing import Optional, Dict, Any
from app.configuracion import obtener_configuracion

logger = logging.getLogger(__name__)
config = obtener_configuracion()


class ClienteNucleoFinanciero:
    """
    Cliente HTTP asíncrono para el microservicio-nucleo-financiero (Puerto 8085).
    Obtiene transacciones, resúmenes y categorías.
    """

    def __init__(self):
        self.url_base = config.url_nucleo_financiero
        self.timeout = httpx.Timeout(30.0, connect=10.0)

    def obtener_historial_transacciones(
        self,
        usuario_id: str,
        token: str,
        tamanio: int = 200,
        pagina: int = 0,
        mes: Optional[int] = None,
        anio: Optional[int] = None,
        tipo: Optional[str] = None,
    ) -> Dict[str, Any]:
        """
        Obtiene el historial enviando un rango de fechas compatible con 
        el LocalDateTime de Java.
        Endpoint: GET /api/v1/transacciones/historial
        """
        # ── 1. Construir el rango de fechas ──────────────────────────────────
        # Si recibimos mes y año, calculamos el primer y último día del mes
        desde_str = None
        hasta_str = None

        if mes and anio:
            # Primer día del mes a las 00:00:00
            fecha_inicio = datetime(anio, mes, 1, 0, 0, 0)
            desde_str = fecha_inicio.isoformat() 

            # Último día del mes (calculado para evitar errores de 30/31 días)
            if mes == 12:
                fecha_fin = datetime(anio + 1, 1, 1, 23, 59, 59)
            else:
                # Vamos al primer día del siguiente mes y restamos un segundo (lógica simple)
                fecha_fin = datetime(anio, mes + 1, 1, 0, 0, 0)
            
            hasta_str = fecha_fin.isoformat()

        # ── 2. Preparar Query Params ─────────────────────────────────────────
        params: Dict[str, Any] = {
            "usuarioId": usuario_id,
            "size": tamanio,
            "page": pagina,
        }
        
        if desde_str: params["desde"] = desde_str
        if hasta_str: params["hasta"] = hasta_str
        if tipo: params["tipo"] = tipo

        # Agregamos el encabezado de Authorization
        headers = {
            "Authorization": f"Bearer {token}"
        }
        
        url = f"{self.url_base}/api/v1/transacciones/historial"

        try:
            with httpx.Client(timeout=self.timeout) as cliente:
                respuesta = cliente.get(url, params=params, headers=headers)
                respuesta.raise_for_status()
                logger.info(
                    "[CLIENTE] Historial obtenido para usuarioId=%s — %d transacciones",
                    usuario_id,
                    len(respuesta.json().get("content", [])),
                )
                return respuesta.json()
        except httpx.ConnectError:
            logger.error("[CLIENTE] No se pudo conectar a microservicio-nucleo-financiero en %s", self.url_base)
            raise ConnectionError(
                f"No se puede conectar al microservicio financiero en {self.url_base}. "
                "Verifique que el servicio Java esté corriendo en el puerto 8085."
            )
        except httpx.TimeoutException:
            logger.error("[CLIENTE] Timeout al consultar historial para usuarioId=%s", usuario_id)
            raise TimeoutError("El microservicio financiero tardó demasiado en responder.")
        except httpx.HTTPStatusError as exc:
            if exc.response.status_code == 401:
                logger.error("[CLIENTE] Error 401: El token proporcionado no es válido o expiró")
            raise ValueError(f"Error del servidor financiero: {exc.response.status_code} — {exc.response.text}")

    def obtener_resumen_financiero(
        self,
        usuario_id: str,
        token: str,
        mes: Optional[int] = None,
        anio: Optional[int] = None,
    ) -> Dict[str, Any]:
        """
        Obtiene el resumen de ingresos/gastos/balance del usuario.
        Endpoint: GET /api/v1/transacciones/resumen
        """
        params: Dict[str, Any] = {"usuarioId": usuario_id}
        if mes:
            params["mes"] = mes
        if anio:
            params["anio"] = anio

        # Agregamos el encabezado de Authorization
        headers = {
            "Authorization": f"Bearer {token}"
        }
        url = f"{self.url_base}/api/v1/transacciones/resumen"

        try:
            with httpx.Client(timeout=self.timeout) as cliente:
                respuesta = cliente.get(url, params=params, headers=headers)
                respuesta.raise_for_status()
                return respuesta.json()
        except httpx.ConnectError:
            raise ConnectionError(
                f"No se puede conectar al microservicio financiero en {self.url_base}."
            )
        except httpx.HTTPStatusError as exc:
            if exc.response.status_code == 401:
                logger.error("[CLIENTE] Error 401: El token proporcionado no es válido o expiró")
            raise ValueError(f"Error del servidor financiero: {exc.response.status_code} — {exc.response.text}")

    def obtener_categorias(self) -> list:
        """
        Obtiene todas las categorías disponibles.
        Endpoint: GET /api/v1/financiero/categorias
        """
        url = f"{self.url_base}/api/v1/financiero/categorias"
        try:
            with httpx.Client(timeout=self.timeout) as cliente:
                respuesta = cliente.get(url)
                respuesta.raise_for_status()
                return respuesta.json()
        except httpx.ConnectError:
            raise ConnectionError(
                f"No se puede conectar al microservicio financiero en {self.url_base}."
            )


class ClienteAuditoria:
    """
    Cliente HTTP para reportar eventos al microservicio-auditoria (Puerto 8082).
    El envío es tolerante a fallos: si falla, solo se registra en log local.
    """

    def __init__(self):
        self.url_base = config.url_auditoria
        self.timeout = httpx.Timeout(5.0, connect=3.0)

    def reportar_evento(
        self,
        usuario_id: str,
        accion: str,
        detalles: str,
        ip_origen: str = "127.0.0.1",
    ) -> None:
        """
        Envía un evento de auditoría al microservicio-auditoria.
        Endpoint: POST /api/v1/auditoria/registrar
        No lanza excepción si el servicio no responde (no bloqueante).
        """
        payload = {
            "nombreUsuario": usuario_id,
            "accion": accion,
            "modulo": "MICROSERVICIO-IA-FINANCIERA",
            "ipOrigen": ip_origen,
            "detalles": detalles,
        }
        url = f"{self.url_base}/api/v1/auditoria/registrar"

        try:
            with httpx.Client(timeout=self.timeout) as cliente:
                respuesta = cliente.post(url, json=payload)
                if respuesta.is_success:
                    logger.debug(
                        "[AUDITORIA] Evento registrado: accion=%s usuario=%s",
                        accion,
                        usuario_id,
                    )
                else:
                    logger.warning(
                        "[AUDITORIA] Respuesta inesperada %d al registrar evento",
                        respuesta.status_code,
                    )
        except Exception as exc:
            #No propagamos: la auditoría nunca debe bloquear la respuesta de la IA
            logger.error(
                "[AUDITORIA] Fallo al enviar evento (no bloqueante): %s", str(exc)
            )
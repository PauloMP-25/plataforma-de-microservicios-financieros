# 🧠 Análisis Arquitectónico y Plan de Implementación — Módulo IA Coach

Este documento detalla la evaluación técnica y el plan por fases para evolucionar el motor de coaching financiero del microservicio de inteligencia artificial (`ms-ia`), integrando soporte para respuestas estructuradas mediante Gemini y persistencia de memoria histórica entre sesiones de usuario.

---

## 🔍 Evaluación del Estado Actual

Antes de plantear el plan, se identifican los siguientes puntos críticos en la arquitectura actual del microservicio:

### 🌟 Fortalezas
*   **Diseño Genérico:** El orquestador `procesar_modulo` es limpio, modular e independiente del tipo de módulo analizado.
*   **Separación de Responsabilidades:** Clara frontera entre el motor analítico de datos (Pandas), la lógica del coach (Gemini) y los módulos de análisis individuales.
*   **Caché Eficiente:** Implementación elegante de almacenamiento en caché vía Redis utilizando firmas de estado.

### ⚠️ Deudas Técnicas a Resolver
*   **Falta de Estructura en el Coach:** La API de Gemini actualmente retorna texto plano (`str`) sin estructura garantizada, lo que dificulta la integración o el parseo de datos en el cliente.
*   **Falta de Contexto Temporal:** No existe memoria persistente entre sesiones del usuario. Cada llamada se procesa como un evento aislado sin conocer consejos previos.
*   **Riesgo de Serialización:** La introducción del modelo `ConsejoEstructurado` puede romper la serialización hacia RabbitMQ y Redis si no se maneja de manera segura y transparente.

### 🚨 Riesgo Principal
El campo `consejo` de `RespuestaModulo` se serializa en múltiples componentes críticos: el método `a_dict_serializable()`, el patrón Outbox hacia las colas de RabbitMQ, y el `MotorReglasLocal` para fallbacks. El uso de un tipo híbrido como `Union[str, ConsejoEstructurado]` sin una estrategia de serialización explícita y robusta introducirá bugs silenciosos en producción.

---

## 🗺️ Plan de Implementación por Fases

El desarrollo se ejecutará de forma incremental a lo largo de 5 fases para garantizar la retrocompatibilidad y estabilidad del sistema.

### 🧱 Fase 1 — Contratos y Esquemas (Fundación)
*Objetivo:* Establecer los nuevos tipos de datos sin alterar la lógica de negocio activa de los módulos.

1.  **Definición de Esquema:** Crear el modelo Pydantic `ConsejoEstructurado` en `esquemas.py` con los 6 campos semánticos requeridos para la respuesta del coach.
2.  **Hibridación del Campo:** Modificar el atributo `RespuestaModulo.consejo` a `Optional[Union[str, ConsejoEstructurado]]`.
3.  **Robustecer Serialización:** Modificar `a_dict_serializable()` para detectar si `consejo` es una instancia de `ConsejoEstructurado`. En ese caso, invocar `.model_dump()` de manera explícita antes de la serialización JSON. Esto asegura que la base de datos Outbox y las colas de RabbitMQ manejen siempre diccionarios JSON nativos y previene fallos con tipos no primitivos.
4.  **Contrato con MotorReglasLocal:** Actualmente, el fallback para `GASTO_HORMIGA` retorna Markdown plano en `str`.
    *   Definir un método privado `_construir_fallback_estructurado()` dentro de `MotorReglasLocal` que devuelva un diccionario alineado con `ConsejoEstructurado`.
    *   Mantener la firma pública de `generar_fallback()`, pero añadir una condición específica para `GASTO_HORMIGA` que llame a este nuevo método privado y devuelva el objeto estructurado.

---

### 💾 Fase 2 — Persistencia de Memoria
*Objetivo:* Diseñar e implementar la infraestructura de almacenamiento persistente para el historial de coaching.

1.  **Definición de Modelo en Base de Datos:** En `modelos_db.py`, definir la tabla `IaHistorialCoaching`.
    *   Usar el tipo `JSON` de SQLAlchemy para las columnas `insight_calculado` y `consejo_solicitado`. Esto garantiza la compatibilidad dual: almacenamiento tipo `JSONB` nativo en PostgreSQL (producción) y JSON plano en SQLite (entorno de pruebas unitarias).
    *   Agregar un índice compuesto sobre `(usuario_id, modulo)` para asegurar consultas óptimas del historial más reciente.
2.  **Capa de Repositorio:** Crear `RepositorioHistorialCoaching` dentro de la carpeta `persistencia/` con dos métodos públicos:
    *   `obtener_ultimo(usuario_id, modulo)`: Consulta el último consejo estructurado registrado para el módulo dado.
    *   `guardar(registro)`: Registra la interacción en base de datos.
    *   *Nota:* Esta separación evita que el servicio principal (`servicio_analisis.py`) importe dependencias directas de SQLAlchemy, facilitando el mocking en pruebas.
3.  **Migración de BD:** Diseñar y ejecutar la migración correspondiente con Alembic para evitar inconsistencias en el esquema de base de datos de producción.

---

### 🤖 Fase 3 — Evolución del CoachIA (Structured Outputs)
*Objetivo:* Extender la comunicación con Gemini para soportar esquemas estructurados de salida, sin afectar los módulos que dependen de texto plano.

1.  **Modificación de la Firma Interna:** Modificar el método privado `_llamar_gemini_api` para aceptar un parámetro opcional: `esquema_salida: Optional[Type[BaseModel]] = None`.
2.  **Bifurcación de Lógica:**
    *   *Si `esquema_salida` es `None`:* Comportamiento tradicional. Retorna el string plano y mantiene el prompt de Markdown por defecto.
    *   *Si `esquema_salida` está presente:* Configurar en el `GenerationConfig` de la llamada el parámetro `response_mime_type="application/json"` y `response_schema=esquema_salida`. En el retorno, procesar `json.loads(respuesta.text)`.
3.  **Propagación en el Método Público:** Actualizar `obtener_consejo_ia` para que acepte y propague `esquema_salida`.
4.  **Preservación de Políticas:** Mantener intacta la lógica de control de cuotas en Redis (el incremento diario se realiza en `_verificar_cuota_diaria` antes del llamado al API).
5.  **Mantenimiento de Fallbacks y Circuit Breaker:** Si el Circuit Breaker (`gemini_breaker`) se activa o el API falla, el sistema continuará devolviendo el fallback de `MotorReglasLocal`.
    *   *Atención:* Dado que `gemini_breaker.call()` envuelve una llamada sincrónica, propagar `esquema_salida` usando `functools.partial` o funciones lambda para no alterar la firma del callback esperada por `pybreaker`.

---

### 🎛️ Fase 4 — Orquestador con Memoria
*Objetivo:* Enlazar las capas de persistencia, lógica y presentación en `servicio_analisis.py` preservando la arquitectura abierta del monorepo.

1.  **Consulta Síncrona de Antecedentes:** Dentro del método `procesar_modulo` del orquestador:
    *   Antes de delegar los cálculos a Pandas, invocar síncronamente `RepositorioHistorialCoaching.obtener_ultimo(usuario_id, modulo_enum.value)`.
    *   Si existe un registro, inyectar el campo `consejo_solicitado` de la sesión previa en el diccionario `metricas` bajo la llave `_historial_previo`. Esto permite que los módulos analíticos consuman el historial sin modificar las firmas de sus métodos de cálculo.
2.  **Activación Condicional del Esquema:** Al invocar `self._coach.obtener_consejo_ia(...)`, aplicar el esquema estructurado selectivamente:
    ```python
    esquema = ConsejoEstructurado if modulo_enum == NombreModulo.GASTO_HORMIGA else None
    ```
    Los 9 módulos restantes mantendrán su flujo tradicional de string plano al recibir `None`.
3.  **Persistencia del Resultado:** Tras una respuesta exitosa, registrar la interacción en la base de datos de historial.
    *   Envolver este paso de persistencia en un bloque `try/except` de seguridad. Cualquier excepción de escritura en base de datos debe ser registrada como error en logs pero **no debe interrumpir** el flujo del usuario ni fallar la petición de análisis.
    *   Si el contexto utiliza un event loop asíncrono persistente, delegar la escritura con `asyncio.create_task()`. De lo contrario, ejecutar una llamada síncrona segura.

---

### 📝 Fase 5 — Actualización del Prompt de Gasto Hormiga
*Objetivo:* Modificar el módulo de detección de gastos hormiga para explotar la memoria histórica y guiar la generación hacia el nuevo esquema JSON.

1.  **Inyección de Contexto:** En `deteccion_gastos_hormiga.py` (método `orquestar_prompt`), verificar la existencia de la llave `_historial_previo` dentro de `metricas`.
    *   De existir, construir e inyectar un segmento `"CONTEXTO DE SESIÓN ANTERIOR"` detallando el último foco de fuga de capital y metas aconsejadas para que Gemini realice un contraste analítico continuo.
2.  **Instrucciones de Formato:** Remover indicaciones obsoletas como *"Responde en Markdown"*. Incorporar directrices explícitas sobre qué información analítica debe mapearse a cada campo del esquema JSON (`introduccion`, `analisis_ia`, etc.). Aunque Gemini está forzado a nivel API por el `response_schema`, el refuerzo en el prompt incrementa la precisión contextual y calidad del texto generado.

---

## 🌐 Consideraciones Transversales

*   **Estrategia de Testing:**
    *   **Fase 1 y Fase 2:** Desarrollar pruebas unitarias puras de datos y modelos. No requieren mockear el cliente de Gemini.
    *   **Fase 3:** Mockear la API `genai.GenerativeModel` para validar los comportamientos con y sin `response_schema`.
    *   **Fase 4:** Comprobar mediante assertions que al invocar módulos diferentes de `GASTO_HORMIGA`, el orquestador no realiza operaciones de lectura/escritura en `IaHistorialCoaching`.
*   **Orquestación y Despliegue:**
    *   Respetar la dependencia secuencial de las fases. La Fase 2 (Alembic) debe ejecutarse obligatoriamente antes de activar el código de la Fase 4.
    *   Se recomienda configurar un Feature Flag para el comportamiento estructurado de `GASTO_HORMIGA`, permitiendo desactivarlo en producción de manera inmediata si se detectan anomalías de consumo.
*   **Concurrencia en Consumidor RabbitMQ:**
    *   El `ConsumidorIA` se ejecuta mediante llamadas síncronas usando `asyncio.run()`. En caso de usar lógica asíncrona para guardar el historial en la Fase 4, validar la compatibilidad del event loop en el hilo daemon del consumidor de RabbitMQ para evitar colisiones de hilos.

---

## 💡 Estrategia Global: Técnicas Avanzadas de Prompting (Optimización de Tokens)

Para maximizar el margen de rentabilidad por suscripción y reducir el consumo de tokens de Gemini, todos los módulos de LUKA deben aplicar estrictamente las siguientes **5 técnicas de optimización**:

1. **Datos Pre-procesados (El core de la Fase 3)**: NUNCA enviar transacciones crudas a Gemini. Enviar exclusivamente los KPIs y Hallazgos analíticos ya calculados por Pandas (ej. promedios, variaciones, detección de fugas). Esto reduce los miles de tokens de entrada a solo decenas.
2. **Etiquetas XML (`<perfil>`, `<instrucciones>`)**: En lugar de usar separadores visuales que consumen múltiples tokens (ej. `═════════`), usar XML. Los LLMs están altamente entrenados para interpretar estas etiquetas estructuradas rápidamente, mejorando la segmentación semántica del contexto.
3. **Structured Outputs (Nativo Pydantic)**: No gastar tokens en el prompt explicando cómo estructurar el JSON. Se delega 100% al parámetro `response_schema` usando la integración nativa de Pydantic, forzando la salida sin "fluff".
4. **Role y Persona upfront**: Definir el rol ("Eres LUKA...") y el tono en la primera línea del prompt para setear la temperatura cognitiva del modelo inmediatamente.
5. **Reducción de "Fluff" y Zero-Shot**: Eliminar saludos o instrucciones conversacionales extensas ("Por favor", "Me gustaría que..."). Usar imperativos directos. Gracias a los esquemas estrictos, se omite el *Few-Shot* (dar ejemplos), utilizando *Zero-Shot* puro.

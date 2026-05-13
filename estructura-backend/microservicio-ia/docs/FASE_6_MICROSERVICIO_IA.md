# 🧠 Guía Maestra de Integración: Microservicio IA (LUKA-COACH V4) — Fase 6

Esta documentación define los contratos de datos (DTOs) y la lógica de inteligencia para el Frontend.

---

## 🏗️ 1. Arquitectura y Gobernanza
- **Gobernanza de Cuotas**: `FREE` (1/SEMANAL), `PRO` (5/SEMANAL), `PREMIUM` (10/SEMANAL).
- **Fallback**: Si Gemini falla, LUKA devuelve un consejo basado exclusivamente en las métricas de Numpy/Pandas.

---

## 🚀 2. Catálogo de Módulos e Integración

### 🕵️‍♂️ A. DETECCIÓN DE GASTO HORMIGA (`GASTO_HORMIGA`)
**Propósito:** Detectar fugas de capital por gastos pequeños recurrentes.
- **Mínimo:** 40 transacciones (20 mes actual / 20 mes anterior).
- **Tono:** `DIRECTO`

**📥 Petición (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "token": "jwt-token",
  "mes": 5,
  "anio": 2026
}
```

**📤 Respuesta (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "modulo": "GASTO_HORMIGA",
  "consejo": "Paulo, vamos al grano....",
  "estado_coach": "EXITOSO",
  "hallazgos": {
    "total_gastos_hormiga": 360.50,
    "principal_gasto_hormiga": "Cafetería",
    "variacion_vs_mes_anterior": 15.2,
    "proyeccion_fuga_anual": 4326.00
  },
  "usando_fallback": false
}
```

**🛡️ Respuesta en caso de Fallback (Gemini caído):**
> "LUKA detectó S/ 360.50 en gastos pequeños este mes (un 15.2% más que el mes pasado). Tu principal fuga es 'Cafetería'. ¡Ojo con los gastos hormiga!"

**💬 Respuesta Real de Gemini (Producción):**
> "Paulo, vamos al grano. Tus gastos en 'Cafetería' han subido un **20%** este mes. Lo que ves como S/ 12.00 diarios hoy, se traduce en una fuga de **S/ 4,320.00 al año**. Con ese dinero podrías comprarte la 'Laptop Gamer' que tanto quieres y aún te sobraría para los periféricos. Estás descuidando tu meta por una comodidad momentánea. Para empezar con fuerza, esta semana ponte el reto de llevar tu propio café en un termo al campus al menos tres días. Verás que ese pequeño cambio acelerará tu camino hacia esa nueva computadora y te dará la tranquilidad que necesitas para programar. ¡Deja de financiar el marketing de las grandes cadenas y empieza a financiar tu herramienta de trabajo!"

---

### 🔮 B. PREDICCIÓN DE GASTOS (`PREDECIR_GASTOS`)
**Propósito:** Proyectar el balance del próximo mes.
- **Mínimo:** 50 transacciones.
- **Tono:** `FORMAL`

**📥 Petición (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "token": "jwt-token"
}
```

**📤 Respuesta (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "modulo": "PREDECIR_GASTOS",
  "consejo": "Estimado Paulo, proyectamos egresos de S/ 1,850...",
  "estado_coach": "EXITOSO",
  "hallazgos": {
    "gasto_proyectado": 1850.00,
    "ingreso_esperado": 2000.00,
    "margen_seguridad": 7.5,
    "nivel_riesgo": "MODERADO"
  },
  "usando_fallback": false
}
```

**🛡️ Respuesta en caso de Fallback:**
> "Proyección LUKA: Gastos S/ 1,850 vs Ingresos S/ 2,000. Margen de seguridad: 7.5%. Riesgo Moderado detectado."

**💬 Respuesta Real de Gemini (Producción):**
> "Estimado Paulo Moron, tras realizar el análisis econométrico de su historial transaccional, proyectamos que sus egresos para el próximo periodo ascenderán a **S/ 1,850.00**. Dado que sus ingresos mensuales se sitúan en S/ 2,000.00, su margen de maniobra es del **7.5%**, lo cual se considera un nivel de riesgo moderado ante contingencias. Le recomendamos formalmente priorizar la constitución de un fondo de reserva equivalente a tres meses de gastos. Evite comprometerse con nuevas obligaciones financieras durante el próximo trimestre para asegurar la viabilidad de su meta principal de la 'Laptop Gamer' sin comprometer sus necesidades básicas."

---

### 🎯 C. SIMULACIÓN DE META (`SIMULAR_META`)
**Propósito:** Validador de viabilidad real para compras importantes basándose en tu capacidad de ahorro mensual.
- **Mínimo:** 30 transacciones.
- **Tono:** `MOTIVADOR`

**📥 Petición (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "meta_nombre": "Laptop Gamer",
  "meta_monto": 3500.00,
  "meta_ahorro_previo": 500.00,
  "fecha_objetivo": "2026-12-31"
}
```

**📤 Respuesta (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "modulo": "SIMULAR_META",
  "consejo": "¡Paulo, tu meta es totalmente viable!...",
  "estado_coach": "EXITOSO",
  "hallazgos": {
    "meses_para_lograrlo": 6.5,
    "capacidad_ahorro_mensual": 450.00,
    "viabilidad_fecha_objetivo": true,
    "ahorro_faltante": 3000.00
  },
  "usando_fallback": false
}
```

**🛡️ Respuesta en caso de Fallback:**
> "Meta: Laptop Gamer. Faltan S/ 3,000. Capacidad de ahorro: S/ 450/mes. Tiempo estimado: 6.5 meses. Viable según fecha objetivo."

**💬 Respuesta Real de Gemini (Producción):**
> "¡Paulo, tu meta de la 'Laptop Gamer' es **TOTALMENTE VIABLE** y estás más cerca de lo que crees! Con tu capacidad de ahorro actual de S/ 450.00 al mes y tu ahorro previo de S/ 500.00, en aproximadamente **6.5 meses** estarás estrenando equipo. Pero espera, he analizado tus finanzas y si logras optimizar solo un poco tus gastos de ocio, podrías subir ese aporte a S/ 550.00 y tenerla en solo 5 meses. ¡Imagina la potencia de ese procesador trabajando para ti medio año antes! Mantén el enfoque, cada sol ahorrado hoy es un frame más por segundo en tu nueva computadora. ¡Tú puedes!"

---

### 🎮 D. RETO DE AHORRO DINÁMICO (`RETO_AHORRO_DINAMICO`)
**Propósito:** Misiones interactivas de 7 días para ludificar el ahorro y vencer hábitos de gasto específicos.
- **Mínimo:** 20 transacciones.
- **Tono:** `GAMER`

**📥 Petición (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "frecuencia": "SEMANAL"
}
```

**📤 Respuesta (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "modulo": "RETO_AHORRO_DINAMICO",
  "consejo": "¡Misión: Operación Termo! Evita café fuera...",
  "estado_coach": "EXITOSO",
  "hallazgos": {
    "estado_reto": "ACTIVO",
    "id_reto": 45,
    "categoria_objetivo": "Restaurantes",
    "progreso_temporal": 45,
    "monto_limite": 50.00
  },
  "usando_fallback": false
}
```

**🛡️ Respuesta en caso de Fallback:**
> "Reto Activo: Reducir gasto en Restaurantes. Progreso temporal: 45%. Meta: No exceder S/ 50.00 esta semana."

**💬 Respuesta Real de Gemini (Producción):**
> "¡Misión: Operación Cocina en Casa! 🏆 Paulo, he detectado que tu 'Enemigo Final' de esta semana son los Restaurantes. Tu misión, si decides aceptarla, es evitar comer fuera por los próximos 7 días. Si lo logras, habrás salvado **S/ 85.00** para tu fondo de la 'Laptop Gamer'. Tu barra de progreso ya está activa en tu Dashboard al **45%**. ¿Aceptas el reto, Jugador 1?"

**Respuesta de LUKA (Veredicto de Éxito):**
> "¡MISIÓN CUMPLIDA, PAULO! 🏆 Has superado el reto 'Operación Cocina en Casa'. Al evitar comer fuera esta semana, has ahorrado **S/ 85.00** que ya están en tu fondo para la laptop. Tu racha actual es de **1 reto ganado**. ¿Listo para el siguiente nivel?"
---

### 🔄 E. ANÁLISIS DE HÁBITOS (`HABITOS_FINANCIEROS`)
**Propósito:** Identifica patrones de conducta (días de mayor gasto y categorías frecuentes) para corregir comportamientos por impulso.
- **Mínimo:** 20 transacciones.
- **Tono:** `AMIGABLE`

**📥 Petición (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "token": "jwt-token",
  "frecuencia": "SEMANAL"
}
```

**📤 Respuesta (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "modulo": "HABITOS_FINANCIEROS",
  "consejo": "¡Hola Paulo! He notado que tus Sábados...",
  "estado_coach": "EXITOSO",
  "hallazgos": {
    "frecuencia_analizada": "SEMANAL",
    "dia_mayor_gasto": "Saturday",
    "categoria_mas_frecuente": "Restaurantes",
    "total_transacciones_periodo": 18
  },
  "usando_fallback": false
}
```

**🛡️ Respuesta en caso de Fallback:**
> "Análisis de Hábitos: Tu día de mayor gasto es el Sábado. La categoría más frecuente es 'Restaurantes' con 18 movimientos esta semana."

**💬 Respuesta Real de Gemini (Producción):**
> "¡Hola Paulo! He notado que tus Sábados a las 6 PM son el momento donde tu billetera más sufre, especialmente en 'Restaurantes'. Parece que el fin de semana te invita a celebrar, ¡y eso está bien!, pero esos pequeños impulsos están frenando tu meta de la **Laptop Gamer**. 
> 
> **Hábito Atómico:** Prueba la 'Regla de las 48 horas': si ves algo que quieres comprar un sábado, espérate al lunes. Si aún lo quieres, cómpralo. Verás cómo el 80% de esos antojos desaparecen solos. ¡Tú tienes el control!"

---

### 🕵️‍♂️ F. ANÁLISIS DE ESTILO DE VIDA (`ESTILO_VIDA`)
**Propósito:** Define una "Personalidad Financiera" agrupando gastos en clusters de estilo de vida (Tech, Foodie, Wellness, etc.).
- **Mínimo:** 20 transacciones.
- **Tono:** `OBSERVADOR & INGENIOSO`

**📥 Petición (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "token": "jwt-token"
}
```

**📤 Respuesta (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "modulo": "ANALISIS_ESTILO_VIDA",
  "consejo": "ver EL SIGUIENTE PUNTO RESPUESTA FALLBACK Y REAL",
  "estado_coach": "EXITOSO",
  "hallazgos": {
    "cluster_dominante": "FOODIE",
    "porcentaje_dominancia": 65.4,
    "total_analizado": 1250.00,
    "personalidad_detectada": "El Foodie Explorador"
  },
  "usando_fallback": false
}
```

**🛡️ Respuesta en caso de Fallback:**
> "Perfil detectado: 'Foodie'. El 65.4% de tus gastos variables se concentran en Restaurantes y Cafeterías."

**💬 Respuesta Real de Gemini (Producción):**
> "Paulo, tras analizar tus movimientos, te he bautizado como **'El Foodie Explorador'**. Tienes un paladar exigente: el 65% de tus gastos no fijos se van en descubrir nuevos sabores en restaurantes y barras de café. 
> 
> **Valor de Salud:** Como tu perfil es gastronómico, podrías ahorrar un **15%** mensual si aprovechas los días de promociones bancarias en tus locales favoritos o si te pones un presupuesto semanal de 'salidas' fijo. Ese ahorro extra de S/ 120.00 aceleraría tu meta de la **Laptop Gamer** en casi un mes. ¡Sigue explorando, pero con estrategia!"

---

### 📊 G. REPORTE COMPLETO (`REPORTE_COMPLETO`)
**Propósito:** Auditoría estratégica anual y cálculo del Score de Salud Financiera.
- **Mínimo:** 60 transacciones.
- **Tono:** `ANALÍTICO`

**📥 Petición (JSON):**
```json
{ "usuario_id": "uuid-123" }
```

**📤 Respuesta (JSON):**
```json
{
  "usuario_id": "uuid-123",
  "modulo": "REPORTE_COMPLETO",
  "consejo": "Tu Score LUKA es 78/100...",
  "estado_coach": "EXITOSO",
  "hallazgos": {
    "score_salud": 78,
    "balance_anual": 2450.00,
    "total_ingresos": 15000.00,
    "total_gastos": 12550.00,
    "categoria_critica": "Ocio"
  },
  "usando_fallback": false
}
```

**🛡️ Respuesta en caso de Fallback:**
> "Reporte Anual: Balance S/ +2,450.00. Score de Salud: 78/100. Categoría de mayor gasto: Ocio. Gestión Estable."

**💬 Respuesta Real de Gemini (Producción):**
> "Paulo, tu **Score LUKA es 78/100**. Has mantenido un crecimiento constante en tus ahorros desde el 1 de enero. Tu balance anual positivo de S/ 2,450.00 indica una gestión responsable, aunque detectamos un punto crítico en Marzo. Eres un 'Ahorrador Estratégico'. Tu gestión es superior al 80% de los usuarios de tu perfil. Mantén este ritmo y cerrarás el año con la solvencia necesaria para todas tus metas y estarás estrenando esa nueva laptop antes de lo previsto."

---

### 🏷️ H. AUTO-CLASIFICACIÓN ON-THE-FLY (`AUTO_CLASIFICACION`)
**Propósito:** Sugerencia inteligente de 3 categorías en tiempo real.
- **Activación:** Requiere obligatoriamente `notas` o `etiquetas`.
- **Cuotas Semanales:** `PRO: 10 consultas` | `PREMIUM: 5 consultas`.
- **Canal Principal:** RabbitMQ (`q.ia.clasificacion`).
- **Canal Alternativo:** REST (`POST /api/v1/ia/clasificar-transaccion`).
- **Latencia:** Optimizada con Gemini Flash ( < 1.5s).

**📥 Petición (JSON):**
```json
{
  "id_temporal": "temp-456",
  "tipo_movimiento": "GASTO",
  "notas": "Cena con amigos en Pizza Hut",
  "etiquetas": "viernes, ocio"
}
```

**📤 Respuesta (JSON):**
```json
{
  "id_temporal": "temp-456",
  "sugerencias": ["Restaurantes", "Alimentación", "Ocio"],
  "usando_fallback": false
}
```

**🛡️ Respuesta en caso de Fallback:**
> `["General", "Otros", "Varios"]`

---

## ⚠️ 3. Tabla de Errores y Excepciones

| Código HTTP | Código IA | Descripción | Acción sugerida |
| :--- | :--- | :--- | :--- |
| **429** | `CUOTA_EXCEDIDA` | El usuario alcanzó su límite diario según su plan. | UI: Mostrar modal de Upgrade. |
| **400** | `HIST_INSUF` | No llega al mínimo de transacciones (TXs) requerido. | UI: Mostrar "Registra N movimientos más". |
| **503** | `CIRCUIT_OPEN` | El motor de IA está en mantenimiento o saturado. | UI: Mostrar "LUKA está meditando". |
| **200** | `FALLBACK` | Error interno. Se usa una respuesta de seguridad. | UI: Transparente para el usuario. |

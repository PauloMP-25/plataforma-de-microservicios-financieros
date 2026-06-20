# 📖 Manual de Endpoints Backend - LUKA APP (Postman Format)

Este documento detalla todos los endpoints expuestos por los microservicios, mostrando la estructura exacta de los JSON de entrada (Input) y salida (Output) basados en los DTOs reales para facilitar la integración por parte del equipo Frontend.

> Instrucciones: Cambia el estado de `[ ] Pendiente` a `[X] LISTO` en la primera columna cuando el endpoint haya sido integrado exitosamente en el frontend. El Output JSON representa el contenido interno (la propiedad `datos`) del envoltorio genérico `ResultadoApi`.

---

## 🚦 API Gateway (BFF Dashboard)

### `GET /api/v1/dashboard/resumen`
**Descripción:** Endpoint BFF unificado para obtener el perfil de usuario, resumen de KPIs y transacciones recientes.
**Parámetros:** `refresh` (query, boolean), `X-Usuario-Id` (header), `Authorization` (header)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "perfil": { "nombres": "Paulo", "apellidos": "Moron" },
  "resumen": { "saldoTotal": 1500.00, "ingresosMes": 3000.00 },
  "recientes": [ { "id": "1", "monto": 50.00, "concepto": "Compra" } ]
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/dashboard/graficos`
**Descripción:** Endpoint BFF para obtener los datos de gráficos SVG (flujo de caja e ingresos/egresos).
**Parámetros:** `refresh` (query, boolean), `X-Usuario-Id` (header), `Authorization` (header)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "ingresosVsEgresos": { "ingresos": 3000, "egresos": 2500 }
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/dashboard/analitica-avanzada`
**Descripción:** Endpoint BFF para Analítica Avanzada (Dashboard V2) con filtros dinámicos.
**Parámetros:** `fechaInicio`, `fechaFin`, `metodoPago`, `tipoMovimiento` (query), `X-Usuario-Id`, `Authorization` (header)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "resumen": {
    "desde": "2026-06-01T00:00:00Z",
    "hasta": "2026-06-30T23:59:59Z",
    "tasaAhorro": 22.5,
    "gastoPromedioDiario": 65.20,
    "cumplimientoPresupuesto": 68.4,
    "proyeccionFinDeMes": 1850.00
  },
  "flujoCaja": [
    { "mes": "Ene", "ingresos": 3000, "gastos": 2500 }
  ],
  "distribucionGastos": [
    { "categoria": "Alimentación", "total": 800, "porcentaje": 35, "color": "#f59e0b" }
  ],
  "heatmap": [
    { "dia": "Lunes", "intensidad": 3 }
  ],
  "metas": [
    { "nombre": "Fondo de Emergencia", "objetivo": 10000, "actual": 6500, "porcentaje": 65, "color": "#10b981" }
  ],
  "comparativa": [
    { "mes": "Ene", "actual": 2500, "anterior": 2300 }
  ],
  "transaccionesMetodo": [
    { "metodo": "Tarjeta", "cantidad": 25, "color": "#3b82f6" }
  ]
}</code></pre></td>
  </tr>
</table>

---

## 👤 Microservicio Usuario

### `POST /api/v1/auth/login`
**Descripción:** Valida credenciales e inicia sesión.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "correo": "paulo@luka-financial.com",
  "password": "adminUTP123$"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "tokenAcceso": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "d3b07384-d113-4a0b-8083-d922a901ba8d",
  "tipoToken": "Bearer",
  "expiraEn": 3600000,
  "refreshExpiraEn": 86400000,
  "idUsuario": "uuid-1234",
  "nombreUsuario": "paulo_admin",
  "roles": ["ROLE_ADMIN"]
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/auth/registrar`
**Descripción:** Registra un nuevo usuario y envía OTP.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "nombreUsuario": "paulo_moron",
  "correo": "paulo@luka-financial.com",
  "password": "Password123!",
  "confirmarPassword": "Password123!"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">"d3b07384-d113-4a0b-8083-d922a901ba8d"</code></pre></td>
  </tr>
</table>

### `PUT /api/v1/auth/activar`
**Descripción:** Activa cuenta usando OTP.
**Parámetros:** `correo` (query, string), `codigoOtp` (query, string, opcional), `telefono` (query, string, opcional)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">"OK"</code></pre></td>
  </tr>
</table>

### `POST /api/v1/auth/refrescar-token`
**Descripción:** Renueva el access token.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "refreshToken": "token-uuid-123"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "tokenAcceso": "eyJhbG...",
  "refreshToken": "nuevo-refresh-uuid",
  "tipoToken": "Bearer",
  "expiraEn": 3600000,
  "refreshExpiraEn": 86400000,
  "idUsuario": "uuid",
  "nombreUsuario": "paulo",
  "roles": ["ROLE_FREE"]
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/admin/usuarios`
**Descripción:** Búsqueda dinámica paginada de usuarios (Solo ADMIN).
**Parámetros:** `habilitado` (boolean, opcional), `rol` (string, opcional), `texto` (string, opcional), `pagina` (int), `tamanio` (int)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">[
  {
    "id": "uuid",
    "nombreUsuario": "paulo",
    "correo": "paulo@luka-financial.com",
    "habilitado": true
  }
]</code></pre></td>
  </tr>
</table>

---

## 👥 Microservicio Cliente

### `PUT /api/v1/clientes/perfil/{usuarioId}`
**Descripción:** Actualiza los datos personales del cliente.
**Parámetros:** `usuarioId` (path, UUID)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "dni": "12345678",
  "nombres": "Paulo",
  "apellidos": "Moron",
  "genero": "MASCULINO",
  "edad": 25,
  "telefono": "+51999999999",
  "fotoPerfilUrl": "https://link.com/foto.jpg",
  "pais": "Perú",
  "ciudad": "Lima"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "dni": "12345678",
  "nombres": "Paulo",
  "apellidos": "Moron",
  "genero": "MASCULINO",
  "edad": 25,
  "telefono": "+51999999999",
  "fotoPerfilUrl": "https://link.com/foto.jpg",
  "pais": "Perú",
  "ciudad": "Lima",
  "datosCompletos": true
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/clientes/perfil/{usuarioId}`
**Descripción:** Consulta los datos personales.
**Parámetros:** `usuarioId` (path, UUID)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "dni": "12345678",
  "nombres": "Paulo",
  "apellidos": "Moron",
  "genero": "MASCULINO",
  "edad": 25,
  "telefono": "+51999999999",
  "fotoPerfilUrl": "https://link.com/foto.jpg",
  "pais": "Perú",
  "ciudad": "Lima",
  "datosCompletos": true
}</code></pre></td>
  </tr>
</table>

### `PUT /api/v1/clientes/perfil-financiero/{usuarioId}`
**Descripción:** Crea o actualiza perfil financiero.
**Parámetros:** `usuarioId` (path, UUID)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "ocupacion": "Ingeniero de Software",
  "ingresoMensual": 5000.00,
  "estiloVida": "AHORRATIVO",
  "tonoIA": "MOTIVADOR"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "ocupacion": "Ingeniero de Software",
  "ingresoMensual": 5000.00,
  "estiloVida": "AHORRATIVO",
  "tonoIA": "MOTIVADOR"
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/clientes/metas`
**Descripción:** Crea una nueva meta de ahorro.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "nombre": "Fondo de Emergencia",
  "montoObjetivo": 10000.00,
  "montoActual": 1500.00,
  "fechaLimite": "2026-12-31",
  "proposito": "Seguridad financiera"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "uuid-meta",
  "nombre": "Fondo de Emergencia",
  "montoObjetivo": 10000.00,
  "montoActual": 1500.00,
  "porcentajeProgreso": 15.00,
  "fechaLimite": "2026-12-31",
  "completada": false,
  "proposito": "Seguridad financiera"
}</code></pre></td>
  </tr>
</table>

---

## 🛡️ Microservicio Auditoría

### `GET /api/v1/auditoria/accesos`
**Descripción:** Recupera la lista paginada de todos los eventos de acceso registrados en el sistema.
**Parámetros:** `pagina` (int), `tamanio` (int)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">[
  {
    "usuarioId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "ipOrigen": "192.168.1.10",
    "navegador": "Mozilla/5.0",
    "estado": "EXITO",
    "detalleError": null,
    "fecha": "2026-06-20T10:00:00",
    "correlationId": "corr-12345"
  }
]</code></pre></td>
  </tr>
</table>

### `GET /api/v1/auditoria/registros`
**Descripción:** Consulta el histórico detallado de auditoría para el Frontend.
**Parámetros:** `modulo` (String, opcional), `pagina` (int), `tamanio` (int)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "usuarioId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "emailUsuario": "usuario@ejemplo.com",
    "accion": "Actualización de perfil",
    "modulo": "ms-usuario",
    "ipOrigen": "192.168.1.15",
    "correlationId": "corr-67890",
    "detalles": "Actualizó su número telefónico",
    "fechaHora": "2026-06-20"
  }
]</code></pre></td>
  </tr>
</table>

### `GET /api/v1/auditoria/transacciones/usuario/{usuarioId}`
**Descripción:** Obtiene el historial de cambios transaccionales realizados por un usuario específico.
**Parámetros:** `usuarioId` (UUID), `pagina` (int), `tamanio` (int)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">[
  {
    "usuarioId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "entidadId": "987e6543-e21b-34d5-c678-526614174001",
    "servicioOrigen": "ms-financiero",
    "entidadAfectada": "cuenta",
    "descripcion": "Creación de cuenta",
    "valorAnterior": "{}",
    "valorNuevo": "{\"saldo\":0}",
    "fecha": "2026-06-20"
  }
]</code></pre></td>
  </tr>
</table>

### `GET /api/v1/seguridad/verificar-ip/{ip}`
**Descripción:** Verifica el estado actual de una dirección IP frente a la lista negra.
**Parámetros:** `ip` (String)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "ip": "192.168.1.10",
  "bloqueada": true,
  "motivo": "Múltiples fallos de autenticación",
  "fechaExpiracion": "2026-06-21T10:00:00"
}</code></pre></td>
  </tr>
</table>

---

## ✉️ Microservicio Mensajería

### `POST /api/v1/mensajeria/otp/generar`
**Descripción:** Genera y envía un código OTP al canal elegido por el usuario.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "usuarioId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "usuario@ejemplo.com",
  "telefono": "+51987654321",
  "tipo": "EMAIL",
  "proposito": "ACTIVACION_CUENTA"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "exito": true,
  "mensaje": "Código enviado correctamente",
  "tipo": "EMAIL"
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/mensajeria/otp/validar-activacion`
**Descripción:** Valida el OTP en el flujo de activación de cuenta.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "usuarioId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "codigo": "123456"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "exito": true,
  "mensaje": "Cuenta validada exitosamente"
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/mensajeria/otp/validar-recuperacion`
**Descripción:** Valida el OTP para el flujo de recuperación de contraseña.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "usuarioId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "codigo": "654321"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">"3fa85f64-5717-4562-b3fc-2c963f66afa6"</code></pre></td>
  </tr>
</table>

### `GET /api/v1/mensajeria/otp/buscar`
**Descripción:** Busca códigos de verificación OTP de forma dinámica cruzando filtros.
**Parámetros:** `usuarioId`, `proposito`, `usado`, `inicio`, `fin`, `pagina`, `tamanio`

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "usuarioId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "email": "usuario@ejemplo.com",
      "telefono": "+51987654321",
      "tipo": "EMAIL",
      "proposito": "ACTIVACION_CUENTA",
      "fechaCreacion": "2026-06-20T10:00:00",
      "fechaExpiracion": "2026-06-20T10:10:00",
      "usado": false,
      "fechaUso": null
    }
  ]
}</code></pre></td>
  </tr>
</table>

---

## 💰 Microservicio Núcleo Financiero

### `POST /api/v1/financiero/categorias`
**Descripción:** Registra una nueva categoría en el sistema.
**Parámetros:** Ninguno (Recibe body)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "nombre": "Ventas",
  "descripcion": "Ingresos por ventas de productos",
  "icono": "shopping-cart",
  "tipo": "INGRESO"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "nombre": "Ventas",
  "descripcion": "Ingresos por ventas de productos",
  "icono": "shopping-cart",
  "tipo": "INGRESO"
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/financiero/transacciones`
**Descripción:** Registra un movimiento financiero individual (Ingreso/Egreso).
**Parámetros:** Ninguno (Recibe body)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
  "nombreCliente": "Cliente XYZ",
  "monto": 150.00,
  "tipo": "INGRESO",
  "categoriaId": "123e4567-e89b-12d3-a456-426614174001",
  "metodoPago": "EFECTIVO",
  "etiquetas": "venta,octubre",
  "descripcion": "Pago de servicios",
  "fechaTransaccion": "2023-10-25T10:00:00"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "transaccion-uuid",
  "nombreCliente": "Supermercado XYZ",
  "monto": 150.00,
  "tipo": "GASTO",
  "categoria": "Alimentación",
  "categoriaIcono": "restaurant",
  "fechaTransaccion": "2026-06-20T10:00:00",
  "metodoPago": "TARJETA_DEBITO",
  "etiquetas": "venta,octubre",
  "descripcion": "Pago de servicios",
  "estado": "Completado"
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/financiero/transacciones/resumen`
**Descripción:** Obtiene un resumen consolidado de las finanzas en un periodo determinado.
**Parámetros:** `usuarioId` (Requerido), `mes` (Opcional), `anio` (Opcional)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "desde": "2026-06-01T00:00:00",
  "hasta": "2026-06-30T23:59:59",
  "totalIngresos": 5000.00,
  "totalGastos": 2500.00,
  "balance": 2500.00,
  "cantidadIngresos": 15,
  "cantidadGastos": 8
}</code></pre></td>
  </tr>
</table>

---

## 💳 Microservicio Pago

### `POST /api/v1/pagos/checkout`
**Descripción:** Inicia el proceso de suscripción creando una sesión de Stripe Checkout.
**Parámetros:** Ninguno (Recibe body)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "plan": "PRO"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "pagoId": "123e4567-e89b-12d3-a456-426614174099",
  "urlCheckout": "https://checkout.stripe.com/pay/cs_test_...",
  "plan": "PRO",
  "monto": 45.90,
  "moneda": "PEN"
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/pagos/mi-suscripcion`
**Descripción:** Devuelve el estado actual de la suscripción del usuario.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "plan": "PRO",
  "estado": "COMPLETADO",
  "monto": 45.90,
  "moneda": "PEN",
  "fechaVencimiento": "2023-11-25T10:00:00",
  "activo": true
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/pagos/admin/resumen`
**Descripción:** Genera un resumen financiero general para el panel de administración.
**Parámetros:** Ninguno

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">{
  "totalTransacciones": 156,
  "ingresosTotales": 7160.40,
  "transaccionesPorEstado": {
    "COMPLETADO": 200,
    "PENDIENTE": 50
  },
  "suscripcionesPorPlan": {
    "PRO": 150,
    "BASIC": 100
  }
}</code></pre></td>
  </tr>
</table>

---

## 🔔 Microservicio Suscripciones

### `POST /api/v1/suscripciones`
**Descripción:** Crea una nueva suscripción de usuario.
**Parámetros:** `SolicitudCrearSuscripcion` en el body.

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
  "nombre": "Netflix Premium",
  "monto": 45.90,
  "metodoPago": "TARJETA",
  "tipoEstrategia": "MENSUAL",
  "fechaInicio": "2026-06-20",
  "fechaVencimiento": "2026-07-20"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "suscripcion-uuid",
  "nombre": "Netflix",
  "monto": 15.99,
  "estado": "ACTIVA",
  "metodoPago": "TARJETA_CREDITO",
  "fechaInicio": "2026-06-20",
  "fechaVencimiento": "2026-07-20",
  "fechaUltimoPago": "2026-06-20",
  "tipoEstrategia": "CALENDARIO"
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/suscripciones/usuario/{usuarioId}`
**Descripción:** Obtiene el listado paginado y filtrado de todas las suscripciones de un usuario.
**Parámetros:** `usuarioId` (UUID en la ruta), `estado`, `metodoPago`, `fechaVencimientoAntes`, `pagina`, `tamanio` (query).

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">[
  {
    "id": "suscripcion-uuid",
    "nombre": "Netflix",
    "monto": 15.99,
    "estado": "ACTIVA",
    "metodoPago": "TARJETA_CREDITO",
    "fechaInicio": "2026-06-20",
    "fechaVencimiento": "2026-07-20",
    "fechaUltimoPago": "2026-06-20",
    "tipoEstrategia": "CALENDARIO"
  }
]</code></pre></td>
  </tr>
</table>

### `POST /api/v1/suscripciones/{id}/pagar`
**Descripción:** Registra manualmente el pago de una suscripción.
**Parámetros:** `id` (UUID en la ruta), `Idempotency-Key` (Header).

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "monto": 45.90,
  "metodoPago": "TARJETA",
  "fechaPago": "2026-06-20"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "pago-uuid",
  "suscripcionId": "suscripcion-uuid",
  "transaccionId": "transaccion-uuid",
  "monto": 15.99,
  "fechaPago": "2026-06-20",
  "estado": "EXITOSO"
}</code></pre></td>
  </tr>
</table>

---

## 🧠 Microservicio IA

### `POST /api/v1/ia/gasto-hormiga`
**Descripción:** Analiza y detecta gastos hormiga y suscripciones olvidadas.
**Parámetros:** `PeticionConFiltroFecha` en el body.

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "usuario_id": "usr-123",
  "token": "eyJhbG...",
  "mes": 6,
  "anio": 2026,
  "frecuencia": "SEMANAL",
  "tamanio_pagina": 200
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "GASTO_HORMIGA",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "El usuario presenta un aumento en sus gastos de café diarios...",
    "analisis_ia": "Hola, veo que tus gastos...",
    "conexion_emocional": "Reducir esto te acercaría a...",
    "plan_accion_titulo": "Plan Hormiga Cero",
    "plan_accion_pasos": ["Paso 1", "Paso 2", "Paso 3", "Paso 4", "Paso 5"],
    "comentario_positivo": "¡Lo estás haciendo genial!"
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": {
    "modulo": "GASTO_HORMIGA",
    "total_transacciones_analizadas": 45,
    "total_ingresos": 5000.0,
    "total_gastos": 2500.0,
    "balance_neto": 2500.0,
    "promedio_gasto_mensual": 1250.0,
    "promedio_ingreso_mensual": 2500.0,
    "hallazgos": {},
    "nivel_alerta": "BAJO",
    "periodo_analizado": "Junio 2026"
  },
  "grafico": null,
  "kpi": null
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/simular-meta`
**Descripción:** Simula el tiempo estimado y sugiere ajustes para alcanzar una meta de ahorro.
**Parámetros:** `PeticionSimularMeta` en el body.

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "usuario_id": "usr-123",
  "token": "eyJhbG...",
  "nombre_meta": "Nueva Laptop",
  "monto_objetivo": 4500.0,
  "monto_actual_ahorrado": 500.0,
  "aporte_mensual_deseado": 300.0
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "SIMULAR_META",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": null,
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": {
    "modulo": "SIMULAR_META",
    "total_transacciones_analizadas": 0,
    "total_ingresos": 0.0,
    "total_gastos": 0.0,
    "balance_neto": 0.0,
    "promedio_gasto_mensual": 0.0,
    "promedio_ingreso_mensual": 0.0,
    "hallazgos": {
      "meses_restantes": 14
    },
    "nivel_alerta": "BAJO",
    "periodo_analizado": "Proyección Futura"
  },
  "grafico": null,
  "kpi": null
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/clasificar-transaccion`
**Descripción:** Valida y sugiere automáticamente la categoría correcta de una transacción.
**Parámetros:** `SolicitudClasificacionDTO` en el body.

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "id_temporal": "tx-123",
  "tipo_movimiento": "GASTO",
  "etiquetas": "Salida, Noche",
  "descripcion": "Restaurante La Lucha"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_temporal": "tx-123",
  "sugerencias": [
    {
      "categoria": "Comida",
      "icono": "utensils"
    },
    {
      "categoria": "Ocio",
      "icono": "film"
    }
  ],
  "usando_fallback": false
}</code></pre></td>
  </tr>
</table>

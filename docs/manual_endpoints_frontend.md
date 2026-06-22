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

## 💬 7. Mensajería y Códigos OTP (`OtpService` → `ms-mensajeria`)

* **Prefijo en API Gateway:** `/api/v1/mensajeria/otp`
* **Microservicio destino:** `ms-mensajeria` (Puerto interno: `8084`)

| Método   | Endpoint                                      | Descripción                            | Body / Parámetros               | FUNCIONAL |
|:-------- |:--------------------------------------------- |:-------------------------------------- |:------------------------------- |:---------:|
| **POST** | `/api/v1/mensajeria/otp/generar`              | Envía código OTP de validación         | `SolicitudGenerarCodigo` (JSON) | `[x]`     |
| **POST** | `/api/v1/mensajeria/otp/validar-activacion`   | Valida código de registro de cuenta    | `SolicitudValidarCodigo` (JSON) | `[x]`     |
| **GET**  | `/api/v1/mensajeria/otp/validar-recuperacion` | Valida código de recuperación de clave | Params: `usuarioId`, `codigo`   | `[ ]`     |
| **POST** | `/api/v1/mensajeria/otp/validar-limite`       | Valida código por alteración de límite | `SolicitudGenerarCodigo` (JSON) | `[x]`     |

---

## 📁 8. Auditoría (`AuditoriaService` → `ms-auditoria`)

* **Prefijo en API Gateway:** `/api/v1/auditoria`
* **Microservicio destino:** `ms-auditoria` (Puerto interno: `8082`)

| Método   | Endpoint                              | Descripción                              | Body / Parámetros                                               | FUNCIONAL |
|:-------- |:------------------------------------- |:---------------------------------------- |:--------------------------------------------------------------- |:---------:|
| **POST** | `/api/v1/auditoria/accesos`           | Guarda registro de login del usuario     | `AuditoriaAccesoRequestDTO` (JSON)                              | `[x]`     |
| **GET**  | `/api/v1/auditoria/accesos`           | Lista accesos (Administrador)            | Params: `pagina`, `tamanio`                                     | `[x]`     |
| **GET**  | `/api/v1/auditoria/verificar-ip/{ip}` | Verifica si la IP tiene restricciones    | Ninguno                                                         | `[x]`     |
| **POST** | `/api/v1/auditoria/transacciones`     | Registra auditoría de cambios monetarios | `AuditoriaTransaccionalRequestDTO` (JSON)                       | `[x]`     |
| **GET**  | `/api/v1/auditoria/transacciones`     | Lista auditoría de transacciones         | Params: `servicioOrigen`, `desde`, `hasta`, `pagina`, `tamanio` | `[x]`     |
| **POST** | `/api/v1/auditoria/registrar`         | Registra evento general en el log        | `RegistroAuditoriaRequestDTO` (JSON)                            | `[x]`     |
| **GET**  | `/api/v1/auditoria/registros`         | Lista logs de eventos de auditoría       | Params: `modulo`, `nivel`, `pagina`, `tamanio`                  | `[x]`     |

---

## 💳 9. Monetización y Pagos (`SuscripcionPremium` → `ms-pago`)

* **Prefijo en API Gateway:** `/api/v1/pagos`
* **Microservicio destino:** `ms-pago` (Puerto interno: `8087`)

| Método   | Endpoint                      | Descripción                                    | Body / Parámetros                  | FUNCIONAL |
|:-------- |:----------------------------- |:---------------------------------------------- |:---------------------------------- |:---------:|
| **POST** | `/api/v1/pagos/checkout`      | Inicia sesión de checkout Stripe (Plan Premium)| `SolicitudPagoDTO` (JSON)          | `[x]`     |
| **GET**  | `/api/v1/pagos/mi-suscripcion`| Obtiene estado de la suscripción del cliente   | Ninguno                            | `[x]`     |
| **POST** | `/api/v1/pagos/webhook`       | Recibe notificaciones asíncronas de Stripe     | Payload bruto de Stripe            | `[x]`     |

---

## 📅 10. Suscripciones y Gastos Recurrentes (`SuscripcionService` → `ms-suscripciones`)

* **Prefijo en API Gateway:** `/api/v1/suscripciones`
* **Microservicio destino:** `ms-suscripciones` (Puerto interno: `8088`)

| Método   | Endpoint                                   | Descripción                                     | Body / Parámetros                  | FUNCIONAL |
|:-------- |:------------------------------------------ |:----------------------------------------------- |:---------------------------------- |:---------:|
| **POST** | `/api/v1/suscripciones`                    | Crea una nueva suscripción de servicios         | `SolicitudCrearSuscripcion` (JSON) | `[x]`     |
| **GET**  | `/api/v1/suscripciones/{id}`               | Consulta una suscripción por ID                 | Ninguno                            | `[x]`     |
| **GET**  | `/api/v1/suscripciones/usuario/{usuarioId}`| Lista suscripciones paginadas del usuario       | Params: `pagina`, `tamanio`, etc.  | `[x]`     |
| **POST** | `/api/v1/suscripciones/{id}/pagar`         | Registra pago manual (Header: Idempotency-Key)  | `SolicitudRegistrarPago` (JSON)    | `[x]`     |
| **POST** | `/api/v1/suscripciones/{id}/cancelar`      | Cancela suscripción recurrente                  | Ninguno                            | `[x]`     |
| **PUT**  | `/api/v1/suscripciones/{id}`               | Actualiza detalles de la suscripción            | `SolicitudEditarSuscripcion` (JSON)| `[x]`     |

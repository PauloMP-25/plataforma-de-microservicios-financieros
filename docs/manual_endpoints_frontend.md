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

### `POST /api/v1/auth/solicitar-otp`
**Descripción:** Solicita un nuevo código OTP para la activación de cuenta.
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
  "email": "usuario@ejemplo.com",
  "telefono": "+51999999999",
  "tipo": "EMAIL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">"OTP_ENVIADO"</code></pre></td>
  </tr>
</table>

### `POST /api/v1/auth/logout`
**Descripción:** Cierra la sesión del usuario actual e invalida el token.
**Parámetros:** `Authorization` (header)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">"Sesión cerrada"</code></pre></td>
  </tr>
</table>

### `POST /api/v1/auth/recuperar-solicitar`
**Descripción:** Inicia el flujo de recuperación de contraseña enviando un OTP.
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
  "correo": "paulo@luka-financial.com"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">"SOLICITUD_PROCESADA"</code></pre></td>
  </tr>
</table>

### `POST /api/v1/auth/recuperar-confirmar`
**Descripción:** Establece una nueva contraseña tras validar el código de recuperación OTP.
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
  "correo": "paulo@luka-financial.com",
  "codigoOtp": "123456",
  "nuevoPassword": "NewPassword123!",
  "confirmarPassword": "NewPassword123!"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">"PASSWORD_RESETEADO"</code></pre></td>
  </tr>
</table>

### `PUT /api/v1/auth/cambiar-password`
**Descripción:** Permite a un usuario autenticado cambiar su contraseña actual.
**Parámetros:** `Authorization` (header)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "passwordActual": "OldPassword123!",
  "nuevoPassword": "NewPassword123!",
  "confirmarPassword": "NewPassword123!"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">"PASSWORD_ACTUALIZADO"</code></pre></td>
  </tr>
</table>

### `DELETE /api/v1/auth/mi-cuenta`
**Descripción:** Desactiva lógicamente la cuenta del usuario autenticado.
**Parámetros:** `Authorization` (header)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><pre><code class="language-json">"CUENTA_ELIMINADA"</code></pre></td>
  </tr>
</table>

### `GET /api/v1/auth/me`
**Descripción:** Obtiene los datos del usuario actual y refresca su token JWT.
**Parámetros:** `Authorization` (header)

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
  "tokenAcceso": "eyJhbG...",
  "refreshToken": "refresh-uuid",
  "tipoToken": "Bearer",
  "expiraEn": 3600000,
  "refreshExpiraEn": 86400000,
  "idUsuario": "uuid",
  "nombreUsuario": "paulo",
  "roles": ["ROLE_FREE"]
}</code></pre></td>
  </tr>
</table>

### `PUT /api/v1/datos-personales/telefono/{usuarioId}`
**Descripción:** Sincroniza el número de teléfono verificado de un usuario.
**Parámetros:** `usuarioId` (path, UUID), `telefono` (query, string)

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

### `POST /api/v1/clientes/perfil/inicial`
**Descripción:** Crea el perfil básico vacío la primera vez.
**Parámetros:** `usuarioId` (query, UUID)

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
  "dni": null,
  "nombres": null,
  "apellidos": null,
  "genero": null,
  "edad": null,
  "telefono": null,
  "fotoPerfilUrl": null,
  "pais": null,
  "ciudad": null,
  "datosCompletos": false
}</code></pre></td>
  </tr>
</table>

### `DELETE /api/v1/clientes/perfil/{usuarioId}`
**Descripción:** Elimina la cuenta del usuario de forma lógica o física.
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
    <td valign="top"><em>(Vacío - 204 No Content)</em></td>
  </tr>
</table>

### `GET /api/v1/clientes/perfil-financiero/{usuarioId}`
**Descripción:** Obtiene los datos financieros base del cliente.
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
  "ocupacion": "Ingeniero de Software",
  "ingresoMensual": 5000.00,
  "estiloVida": "AHORRATIVO",
  "tonoIA": "MOTIVADOR"
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/clientes/metas`
**Descripción:** Lista el historial completo de metas de ahorro con paginación.
**Parámetros:** `page` (query, int), `size` (query, int)

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
      "id": "uuid-meta",
      "nombre": "Viaje a Cancún",
      "montoObjetivo": 2000.00,
      "montoActual": 2000.00,
      "porcentajeProgreso": 100.00,
      "fechaLimite": "2026-11-29",
      "completada": true
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/clientes/metas/activas`
**Descripción:** Lista únicamente las metas que aún no se han completado.
**Parámetros:** `page` (query, int), `size` (query, int)

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
      "id": "uuid-meta-2",
      "nombre": "Laptop",
      "montoObjetivo": 3000.00,
      "montoActual": 1000.00,
      "porcentajeProgreso": 33.33,
      "fechaLimite": "2026-12-31",
      "completada": false
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/clientes/metas/{metaId}`
**Descripción:** Obtiene el detalle de una meta específica.
**Parámetros:** `metaId` (path, UUID)

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

### `PUT /api/v1/clientes/metas/{metaId}`
**Descripción:** Edita la información general de una meta.
**Parámetros:** `metaId` (path, UUID)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "nombre": "Fondo de Emergencia Editado",
  "montoObjetivo": 12000.00,
  "montoActual": 1500.00,
  "fechaLimite": "2027-12-31",
  "proposito": "Seguridad"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "uuid-meta",
  "nombre": "Fondo de Emergencia Editado",
  "montoObjetivo": 12000.00,
  "montoActual": 1500.00,
  "porcentajeProgreso": 12.50,
  "fechaLimite": "2027-12-31",
  "completada": false,
  "proposito": "Seguridad"
}</code></pre></td>
  </tr>
</table>

### `PATCH /api/v1/clientes/metas/{metaId}/progreso`
**Descripción:** Actualiza únicamente el monto actual ahorrado.
**Parámetros:** `metaId` (path, UUID)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "montoActual": 2000.00
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "uuid-meta",
  "nombre": "Fondo de Emergencia Editado",
  "montoObjetivo": 12000.00,
  "montoActual": 2000.00,
  "porcentajeProgreso": 16.67,
  "fechaLimite": "2027-12-31",
  "completada": false,
  "proposito": "Seguridad"
}</code></pre></td>
  </tr>
</table>

### `DELETE /api/v1/clientes/metas/{metaId}`
**Descripción:** Elimina una meta de ahorro.
**Parámetros:** `metaId` (path, UUID)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><em>(Vacío - 204 No Content)</em></td>
  </tr>
</table>

### `POST /api/v1/clientes/limites`
**Descripción:** Establece un nuevo límite de gasto global (presupuesto).
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
  "montoLimite": 2500.00,
  "periodo": "MENSUAL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "uuid-limite",
  "montoLimite": 2500.00,
  "montoGastado": 0.00,
  "periodo": "MENSUAL",
  "fechaInicio": "2026-06-01",
  "fechaFin": "2026-06-30",
  "activo": true
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/clientes/limites/activo`
**Descripción:** Obtiene el límite de gasto actual del cliente.
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
  "id": "uuid-limite",
  "montoLimite": 2500.00,
  "montoGastado": 1200.00,
  "periodo": "MENSUAL",
  "fechaInicio": "2026-06-01",
  "fechaFin": "2026-06-30",
  "activo": true
}</code></pre></td>
  </tr>
</table>

### `PATCH /api/v1/clientes/limites`
**Descripción:** Modifica el monto del límite de gasto que está activo actualmente.
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
  "montoLimite": 3000.00
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "uuid-limite",
  "montoLimite": 3000.00,
  "montoGastado": 1200.00,
  "periodo": "MENSUAL",
  "fechaInicio": "2026-06-01",
  "fechaFin": "2026-06-30",
  "activo": true
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/clientes/limites`
**Descripción:** Lista el historial de límites pasados.
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
    <td valign="top"><pre><code class="language-json">[
  {
    "id": "uuid-limite-viejo",
    "montoLimite": 2000.00,
    "montoGastado": 1900.00,
    "periodo": "MENSUAL",
    "fechaInicio": "2026-05-01",
    "fechaFin": "2026-05-31",
    "activo": false
  }
]</code></pre></td>
  </tr>
</table>

### `DELETE /api/v1/clientes/limites`
**Descripción:** Desactiva o borra el límite de gasto actual.
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
    <td valign="top"><em>(Vacío - 204 No Content)</em></td>
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

### `GET /api/v1/financiero/categorias`
**Descripción:** Obtiene la lista de categorías.
**Parámetros:** `tipo` (Opcional, 'INGRESO' o 'GASTO')

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
    "nombre": "Ventas",
    "descripcion": "Ingresos por ventas de productos",
    "icono": "shopping-cart",
    "tipo": "INGRESO"
  }
]</code></pre></td>
  </tr>
</table>

### `POST /api/v1/financiero/transacciones/lote`
**Descripción:** Registra varias transacciones a la vez.
**Parámetros:** Ninguno (Recibe body)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">[
  {
    "usuarioId": "123e4567-e89b-12d3-a456-426614174000",
    "nombreCliente": "Cliente XYZ",
    "monto": 150.00,
    "tipo": "INGRESO",
    "categoriaId": "123e4567-e89b-12d3-a456-426614174001",
    "metodoPago": "EFECTIVO",
    "etiquetas": "venta,octubre",
    "descripcion": "Pago de servicios",
    "fechaTransaccion": "2023-10-25T10:00:00"
  }
]</code></pre></td>
    <td valign="top"><pre><code class="language-json">[
  {
    "id": "transaccion-uuid",
    "nombreCliente": "Cliente XYZ",
    "monto": 150.00,
    "tipo": "INGRESO",
    "categoria": "Ventas",
    "categoriaIcono": "shopping-cart",
    "fechaTransaccion": "2023-10-25T10:00:00",
    "metodoPago": "EFECTIVO",
    "etiquetas": "venta,octubre",
    "descripcion": "Pago de servicios",
    "estado": "Completado"
  }
]</code></pre></td>
  </tr>
</table>

### `GET /api/v1/financiero/transacciones/historial`
**Descripción:** Historial paginado con filtros opcionales.
**Parámetros:** `usuarioId` (UUID), `tipo`, `categoriaId`, `mes`, `anio`, `pagina`, `tamanio` (query)

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
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}</code></pre></td>
  </tr>
</table>

### `GET /api/v1/financiero/transacciones/{id}`
**Descripción:** Obtiene el detalle de una transacción por ID.
**Parámetros:** `id` (path, UUID)

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

### `PUT /api/v1/financiero/transacciones/{id}`
**Descripción:** Actualiza los detalles de una transacción registrada.
**Parámetros:** `id` (path, UUID)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "nombreCliente": "Supermercado XYZ",
  "monto": 200.00,
  "tipo": "GASTO",
  "categoriaId": "123e4567-e89b-12d3-a456-426614174001",
  "metodoPago": "TARJETA_CREDITO",
  "etiquetas": "compra",
  "descripcion": "Pago de servicios actualizado",
  "fechaTransaccion": "2026-06-20T10:00:00"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id": "transaccion-uuid",
  "nombreCliente": "Supermercado XYZ",
  "monto": 200.00,
  "tipo": "GASTO",
  "categoria": "Alimentación",
  "categoriaIcono": "restaurant",
  "fechaTransaccion": "2026-06-20T10:00:00",
  "metodoPago": "TARJETA_CREDITO",
  "etiquetas": "compra",
  "descripcion": "Pago de servicios actualizado",
  "estado": "Completado"
}</code></pre></td>
  </tr>
</table>

### `DELETE /api/v1/financiero/transacciones/{id}`
**Descripción:** Elimina una transacción específica por ID.
**Parámetros:** `id` (path, UUID)

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><em>(Vacío)</em></td>
    <td valign="top"><em>(Vacío - 204 No Content)</em></td>
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

### `POST /api/v1/ia/predecir-gastos`
**Descripción:** Predice los gastos del próximo mes y advierte sobre déficit potencial.
**Parámetros:** `PeticionConFiltroFechaDTO` en el body.

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
  "mes": 6,
  "anio": 2026,
  "frecuencia": "MENSUAL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "PREDECIR_GASTOS",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "El riesgo de insolvencia es extremadamente alto. Con un ingreso de S/ 3,500.00 y gastos proyectados de S/ 17,427.48, el déficit de S/ 13,927.48 es insostenible.",
    "analisis_tendencia": "¡Hola, Cesar! LUKA aquí, tu estratega financiero. Mira, la proyección para el próximo mes nos muestra un panorama que necesita nuestra atención. Tus gastos proyectados se disparan a S/ 17,427.48...",
    "impacto_meta": "Con esta tendencia, tu emocionante viaje a Japón, que ya lleva un 15% de progreso, se ve seriamente comprometido.",
    "recomendacion_matematica": "Para evitar el déficit proyectado y al menos igualar tus ingresos, necesitas reducir tus gastos proyectados en al menos S/ 13,927.48.",
    "mensaje_motivacional": "Sé que suena fuerte, pero estoy aquí para ayudarte a retomar el control y redirigir tu camino hacia tus metas. ¡Juntos podemos hacerlo!"
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": { "modulo": "PREDECIR_GASTOS" }
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/habitos-financieros`
**Descripción:** Analiza la psicología conductual del usuario cruzando variables temporales y de categoría.
**Parámetros:** `PeticionConFiltroFechaDTO` en el body.

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
  "mes": 6,
  "anio": 2026,
  "frecuencia": "MENSUAL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "HABITOS_FINANCIEROS",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "Fallback activado. Generando respuesta basada en reglas predefinidas y estadísticas de Pandas.",
    "score_salud_habitos": 5,
    "etiquetas_internas": ["fallback", "riesgo_medio"],
    "nota_interna_coach": "Revisar hábitos de la categoría dominante en la próxima sesión.",
    "analisis_patron": "Hola cesar paulo, aquí tienes un resumen rápido. Observamos que tu mayor actividad de gastos es el día Monday, enfocada en la categoría 'Alimentación'.",
    "habito_atomico_sugerido": "Asigna un límite fijo semanal para esa categoría y transfiere el excedente a tu cuenta de ahorro al inicio de la semana.",
    "mensaje_motivacional": "¡Vas por buen camino! Sigue manteniendo ingresos superiores a tus gastos."
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": { "modulo": "HABITOS_FINANCIEROS" }
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/estilo-vida`
**Descripción:** Analiza el estilo de vida del usuario clasificándolo en un Arquetipo Financiero.
**Parámetros:** `PeticionConFiltroFechaDTO` en el body.

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
  "mes": 6,
  "anio": 2026,
  "frecuencia": "MENSUAL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "ANALISIS_ESTILO_VIDA",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "Se detecta un fuerte enfoque en el cluster EXPLORER (15.9%) y un interés secundario en WELLNESS (5.2%).",
    "score_salud_estilo": 6,
    "etiquetas_internas": ["Viajero", "Aventurero", "Bienestar"],
    "nota_interna_coach": "Revisar equilibrio entre gastos de exploración actuales y ahorro para Japón.",
    "arquetipo": "El Explorador Consciente",
    "significado_arquetipo": "Significa que te encanta descubrir nuevos lugares y experiencias...",
    "descripcion_perfil": "¡Hola, Cesar! Qué gusto verte por aquí. Analizando tus movimientos, veo que eres una persona que valora mucho las experiencias y el bienestar.",
    "consejo_tactico": "Para seguir explorando sin afectar tu meta, ¿qué tal si buscas aventuras más cercanas o escapadas de fin de semana que no requieran grandes inversiones?",
    "alineacion_meta": "Tu estilo de vida explorador está muy alineado con tu meta de viajar a Japón...",
    "mensaje_estilo_vida": "Sigue explorando el mundo y cuidando de ti, Cesar. Cada paso consciente te acerca a tu próxima gran aventura."
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": { "modulo": "ANALISIS_ESTILO_VIDA" }
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/reporte-completo`
**Descripción:** Genera un Informe Consolidado de nivel ejecutivo con todos los indicadores anteriores.
**Parámetros:** `PeticionConFiltroFechaDTO` en el body.

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
  "mes": 6,
  "anio": 2026,
  "frecuencia": "MENSUAL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "REPORTE_COMPLETO",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "El usuario, Cesar, tiene un perfil de ingeniero de sistemas con un ingreso mensual de S/ 3,500.00. Su meta activa es 'viaje a japon' con un progreso del 15%...",
    "analisis_score": "¡Cesar, tu Score de Salud de 100/100 es simplemente excelente! Esto significa que tus finanzas están en una posición muy sólida...",
    "impacto_meta": "¡Qué buen balance, Cesar! Con S/ 14145.66 acumulados, estás construyendo una base fantástica para tu 'viaje a japon'...",
    "veredicto_final": "Cesar, lo que va del año 2026 ha sido simplemente espectacular para tus finanzas. Tu Score de Salud perfecto y un balance acumulado de más de S/ 14,000 demuestran una gestión financiera ejemplar.",
    "mensaje_motivacional": "¡Cesar, tu disciplina y esfuerzo están dando frutos increíbles! Sigue con esa energía y enfoque, porque cada decisión inteligente te acerca más a tus sueños."
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": { "modulo": "REPORTE_COMPLETO" }
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/comprobador-evolucion`
**Descripción:** Transforma el historial de los últimos meses en evidencia de evolución financiera.
**Parámetros:** `PeticionComparacionDTO` en el body.

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
  "mesA": 5,
  "anioA": 2026,
  "mesB": 6,
  "anioB": 2026
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "COMPROBADOR_EVOLUCION",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "El usuario Cesar Paulo muestra un incremento notable en el IVG del periodo B debido a un alza del 221% en Transporte y 100% en Suscripciones...",
    "veredicto_narrativo": "Cesar, tu Índice de Madurez Financiera muestra un retroceso debido a un incremento significativo de gastos en áreas no esenciales como Transporte y Suscripciones.",
    "recetas_medicas": [
      {
        "categoria": "Transporte",
        "diagnostico": "Aumento del 221% en el uso de taxis y transporte privado.",
        "posologia": ["1. Planificar viajes semanales y priorizar el uso de transporte público o compartido.", "2. Establecer un presupuesto semanal estricto para transporte privado de máximo S/ 50.", "3. Caminar o usar bicicleta para trayectos menores a 10 cuadras."],
        "pronostico": "Ahorro proyectado de S/ 1,211.48 en los próximos 3 meses."
      },
      {
        "categoria": "Suscripciones Streaming",
        "diagnostico": "Aumento del 100% en el gasto por duplicación de servicios activos.",
        "posologia": ["1. Listar todas las plataformas activas y evaluar el tiempo real de uso de cada una.", "2. Cancelar al menos una suscripción redundante o inactiva esta misma semana.", "3. Compartir cuentas familiares para dividir los costos de suscripción."],
        "pronostico": "Ahorro proyectado de S/ 358.58 en los próximos 3 meses."
      }
    ]
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": { "modulo": "COMPROBADOR_EVOLUCION" }
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/zona-entrenamiento`
**Descripción:** Transforma los KPIs analíticos en una rutina de "Workout Financiero".
**Parámetros:** `PeticionConFiltroFechaDTO` en el body.

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
  "mes": 6,
  "anio": 2026,
  "frecuencia": "MENSUAL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "ZONA_ENTRENAMIENTO",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "El usuario se encuentra en un estado financiero vulnerable ('UCI Financiera') debido a alta volatilidad. Propondré una rutina de entrenamiento intensivo de 3 pasos.",
    "estado_fisico": "UCI Financiera",
    "evaluacion_previa": "No se registran rutinas previas en el historial. Iniciamos periodo de adaptación.",
    "rutina": [
      {
        "nombre": "Cardio de Bolsillo",
        "descripcion": "Revisar detalladamente todos tus gastos menores a S/ 20 del mes pasado.",
        "duracion_dias": 30,
        "frecuencia": "1 vez por semana",
        "metrica_exito": "Identificar y registrar al menos 2 gastos totalmente innecesarios."
      },
      {
        "nombre": "Ayuno de Suscripciones",
        "descripcion": "Suspender temporalmente o cancelar un servicio de streaming que no hayas utilizado en los últimos 15 días.",
        "duracion_dias": 30,
        "frecuencia": "Única vez",
        "metrica_exito": "Una suscripción cancelada y confirmada en la app."
      },
      {
        "nombre": "Levantamiento de Ahorro",
        "descripcion": "Transferir de forma manual o automática S/ 10 a tu cuenta de ahorros al finalizar cada día.",
        "duracion_dias": 30,
        "frecuencia": "Diario",
        "metrica_exito": "Completar la transferencia diaria durante al menos 25 días del mes."
      }
    ]
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": { "modulo": "ZONA_ENTRENAMIENTO" }
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/reto-ahorro`
**Descripción:** Retos cortos configurados proceduralmente según el comportamiento reciente.
**Parámetros:** `PeticionConFiltroFechaDTO` en el body.

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
  "mes": 6,
  "anio": 2026,
  "frecuencia": "SEMANAL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "RETO_AHORRO_DINAMICO",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "El usuario Cesar tiene un ingreso de S/ 3,500.00 y una meta activa de 'viaje a japon' con 15% de progreso...",
    "titulo_mision": "Sabor a Japón: Ahorro Inteligente",
    "diagnostico": "¡Hola, Cesar! Hemos notado que tu categoría de alimentación es un área con un gran potencial de ahorro...",
    "estrategia": "Para lograrlo, te propongo dos reglas de oro: Primero, establece un presupuesto semanal fijo para alimentación y ¡no te salgas de él! Segundo, intenta preparar tus comidas en casa...",
    "mensaje_motivacional": "¡Vamos, Cesar! Tienes el potencial para hacer de este reto una realidad. ¡Imagina esos ahorros transformándose en experiencias inolvidables en Japón!"
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": { "modulo": "RETO_AHORRO_DINAMICO" }
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/espejo-tiempo`
**Descripción:** Se sitúa en un entorno inmersivo frente a dos futuros posibles de ahorro (Ahorro estático vs Ahorro optimizado).
**Parámetros:** `PeticionConFiltroFechaDTO` en el body.

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
  "mes": 6,
  "anio": 2026,
  "frecuencia": "MENSUAL"
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "id_respuesta": "123e4567-e89b-12d3-a456-426614174000",
  "usuario_id": "usr-123",
  "modulo": "ESPEJO_TEMPORAL",
  "fecha_generacion": "2026-06-20T10:00:00.000",
  "consejo": {
    "pensamiento_interno_ia": "Proyección de futuro basada en un ahorro mensual de S/ 7,060.33 actual vs S/ 7,443.20 optimizado...",
    "cartaContinuidad": "Hola Cesar Paulo. Si decides mantener tus hábitos de gasto actuales durante los próximos 12 meses, tu score financiero descenderá a 21 puntos. Aunque logres un ahorro mensual acumulado de S/ 7,060.33...",
    "cartaTransformacion": "Hola Cesar Paulo. Tomar la decisión de optimizar tus gastos no esenciales liberará un ahorro mensual de S/ 7,443.20. En 12 meses, esto representará un capital adicional acumulado de S/ 4,594.47..."
  },
  "estado_coach": "EXITOSO",
  "usando_fallback": false,
  "insight": { "modulo": "ESPEJO_TEMPORAL" }
}</code></pre></td>
  </tr>
</table>

### `POST /api/v1/ia/consultar`
**Descripción:** (Legacy) Consultas libres a la IA.
**Parámetros:** `SolicitudIaDTO` en el body.

<table width="100%">
  <tr>
    <th width="10%">Estado</th>
    <th width="42.5%">Solicitud (Input JSON)</th>
    <th width="42.5%">Respuesta Exitosa (Output JSON)</th>
  </tr>
  <tr>
    <td align="center" valign="middle"><b>[&nbsp;&nbsp;&nbsp;]<br>Pendiente</b></td>
    <td valign="top"><pre><code class="language-json">{
  "mensaje": "Hola",
  "historial": []
}</code></pre></td>
    <td valign="top"><pre><code class="language-json">{
  "mensaje": "¡Hola! ¿En qué puedo ayudarte hoy?"
}</code></pre></td>
  </tr>
</table>


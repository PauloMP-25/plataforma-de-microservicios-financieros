# 📖 Manual de Endpoints - Integración Frontend a Backend

Este manual sirve como referencia técnica para mapear las rutas llamadas desde la aplicación Angular hacia los microservicios del ecosistema **LUKA**, a través de la pasarela **API Gateway (puerto 8080)**.

---

## 🔐 1. Autenticación (`AuthService` → `ms-usuario`)

* **Prefijo en API Gateway:** `/api/v1/auth`
* **Microservicio destino:** `ms-usuario` (Puerto interno: `8081`)

| Método   | Endpoint                           | Descripción                          | Body / Parámetros                              | FUNCIONAL |
|:-------- |:---------------------------------- |:------------------------------------ |:---------------------------------------------- |:---------:|
| **POST** | `/api/v1/auth/login`               | Inicia sesión del usuario            | `SolicitudLogin` (JSON)                        | `[x]`     |
| **POST** | `/api/v1/auth/registrar`           | Registra un nuevo usuario            | `SolicitudRegistro` (JSON)                     | `[x]`     |
| **PUT**  | `/api/v1/auth/activar/{usuarioId}` | Activa la cuenta con código OTP      | Params: `codigoOtp`, `telefono` (opcional)     | `[x]`     |
| **POST** | `/api/v1/auth/solicitar-otp`       | Solicita reenvío de OTP              | `email`, `tipo` ('EMAIL'/'SMS'/'WHATSAPP')     | `[x]`     |
| **POST** | `/api/v1/auth/recuperar-solicitar` | Solicita link/código de recuperación | `{ email }` (JSON)                             | `[ ]`     |
| **POST** | `/api/v1/auth/recuperar-confirmar` | Confirma OTP y cambia password       | Params: `registroId`, `codigoOtp`. Body: `dto` | `[ ]`     |
| **PUT**  | `/api/v1/auth/cambiar-password`    | Cambia la contraseña (autenticado)   | `SolicitudCambioPassword` (JSON)               | `[ ]`     |
| **POST** | `/api/v1/auth/logout`              | Cierra la sesión en el backend       | Ninguno                                        | `[x]`     |

---

## 👤 2. Perfil de Cliente (`ClientePerfilService` → `ms-cliente`)

* **Prefijos en API Gateway:** `/api/v1/clientes/perfil`, `/api/v1/clientes/perfil-financiero`
* **Microservicio destino:** `ms-cliente` (Puerto interno: `8083`)

| Método     | Endpoint                                         | Descripción                               | Body / Parámetros                  | FUNCIONAL |
|:---------- |:------------------------------------------------ |:----------------------------------------- |:---------------------------------- |:---------:|
| **POST**   | `/api/v1/clientes/perfil/inicial`                | Crea el perfil en blanco al registrarse   | Query: `usuarioId={usuarioId}`     | `[x]`     |
| **GET**    | `/api/v1/clientes/perfil/{usuarioId}`            | Consulta los datos personales del cliente | Ninguno                            | `[x]`     |
| **PUT**    | `/api/v1/clientes/perfil/{usuarioId}`            | Actualiza datos personales del cliente    | `SolicitudDatosPersonales` (JSON)  | `[x]`     |
| **DELETE** | `/api/v1/clientes/perfil/{usuarioId}`            | Elimina el perfil y cuenta                | Ninguno                            | `[ ]`     |
| **GET**    | `/api/v1/clientes/perfil-financiero/{usuarioId}` | Obtiene el perfil financiero actual       | Ninguno                            | `[ ]`     |
| **PUT**    | `/api/v1/clientes/perfil-financiero/{usuarioId}` | Actualiza el perfil financiero            | `SolicitudPerfilFinanciero` (JSON) | `[ ]`     |

---

## 🎯 3. Metas y Presupuestos (`ClienteMetasLimitesService` / `PresupuestoService` → `ms-cliente`)

* **Prefijos en API Gateway:** `/api/v1/clientes/metas`, `/api/v1/clientes/limites`
* **Microservicio destino:** `ms-cliente` (Puerto interno: `8083`)

| Método     | Endpoint                                   | Descripción                                   | Body / Parámetros              | FUNCIONAL |
|:---------- |:------------------------------------------ |:--------------------------------------------- |:------------------------------ |:---------:|
| **POST**   | `/api/v1/clientes/metas`                   | Crea una nueva meta de ahorro                 | `SolicitudMetaAhorro` (JSON)   | `[ ]`     |
| **GET**    | `/api/v1/clientes/metas`                   | Lista todas las metas de ahorro               | Ninguno (Mock fallback activo) | `[ ]`     |
| **GET**    | `/api/v1/clientes/metas/activas`           | Lista las metas activas vigentes              | Ninguno                        | `[ ]`     |
| **GET**    | `/api/v1/clientes/metas/{metaId}`          | Detalle de una meta específica                | Ninguno                        | `[ ]`     |
| **PATCH**  | `/api/v1/clientes/metas/{metaId}/progreso` | Actualiza el ahorro actual de la meta         | `{ montoActual }` (JSON)       | `[ ]`     |
| **DELETE** | `/api/v1/clientes/metas/{metaId}`          | Elimina la meta de ahorro                     | Ninguno                        | `[ ]`     |
| **POST**   | `/api/v1/clientes/limites`                 | Crea un límite de gasto mensual (presupuesto) | `SolicitudLimiteGasto` (JSON)  | `[ ]`     |
| **GET**    | `/api/v1/clientes/limites/activo`          | Obtiene el límite activo del mes              | Ninguno                        | `[ ]`     |
| **PATCH**  | `/api/v1/clientes/limites`                 | Modifica el límite activo actual              | `SolicitudLimiteGasto` (JSON)  | `[ ]`     |
| **GET**    | `/api/v1/clientes/limites`                 | Historial de límites/presupuestos del cliente | Ninguno                        | `[ ]`     |
| **DELETE** | `/api/v1/clientes/limites`                 | Elimina/desactiva el límite mensual activo    | Ninguno                        | `[ ]`     |

---

## 💸 4. Transacciones y Categorías (`FinancieroService` / `Transacciones` → `ms-nucleo-financiero`)

* **Prefijos en API Gateway:** `/api/v1/financiero/transacciones`, `/api/v1/financiero/categorias`
* **Microservicio destino:** `ms-nucleo-financiero` (Puerto interno: `8085`)

| Método     | Endpoint                                     | Descripción                                | Body / Parámetros                                                              | FUNCIONAL |
|:---------- |:-------------------------------------------- |:------------------------------------------ |:------------------------------------------------------------------------------ |:---------:|
| **POST**   | `/api/v1/financiero/transacciones`           | Registra una transacción (ingreso/gasto)   | `TransaccionRequestDTO` (JSON)                                                 | `[x]`     |
| **POST**   | `/api/v1/financiero/transacciones/lote`      | Registra múltiples transacciones a la vez  | `TransaccionRequestDTO[]` (JSON)                                               | `[ ]`     |
| **GET**    | `/api/v1/financiero/transacciones/historial` | Historial paginado con filtros de búsqueda | Params: `usuarioId`, `tipo`, `categoriaId`, `mes`, `anio`, `pagina`, `tamanio` | `[ ]`     |
| **GET**    | `/api/v1/financiero/transacciones/{id}`      | Detalle de una transacción por ID          | Ninguno                                                                        | `[ ]`     |
| **PUT**    | `/api/v1/financiero/transacciones/{id}`      | Actualiza los datos de la transacción      | `TransaccionRequestDTO` (JSON)                                                 | `[ ]`     |
| **DELETE** | `/api/v1/financiero/transacciones/{id}`      | Elimina la transacción del sistema         | Ninguno                                                                        | `[ ]`     |
| **GET**    | `/api/v1/financiero/transacciones/resumen`   | Sumatorias de ingresos, egresos y balance  | Params: `usuarioId`, `mes` (opcional), `anio` (opcional)                       | `[x]`     |
| **GET**    | `/api/v1/financiero/categorias`              | Lista categorías para ingresos/gastos      | Params: `tipo` ('INGRESO' / 'GASTO')                                           | `[x]`     |
| **POST**   | `/api/v1/financiero/categorias`              | Crea una categoría personalizada           | `CategoriaRequestDTO` (JSON)                                                   | `[ ]`     |

---

## 📊 5. Agregador Dashboard BFF (`DashboardStateService` → `api-gateway` BFF)

* **Prefijo en API Gateway:** `/api/v1/dashboard`
* **Microservicio destino:** **API Gateway (BFF Controller)** (Puerto interno: `8080`)

| Método  | Endpoint                     | Descripción                                                 | Headers Requeridos                                    | FUNCIONAL |
|:------- |:---------------------------- |:----------------------------------------------------------- |:----------------------------------------------------- |:---------:|
| **GET** | `/api/v1/dashboard/resumen`  | Datos del perfil, KPIs acumulados y transacciones recientes | `X-Usuario-Id` (UUID), `Authorization` (Bearer token) | `[ ]`     |
| **GET** | `/api/v1/dashboard/graficos` | Puntos para gráfico de flujo de caja e ingresos/egresos     | `X-Usuario-Id` (UUID), `Authorization` (Bearer token) | `[ ]`     |

---

## 🤖 6. Consultas e Inteligencia Artificial (`IaService` → `ms-ia`)

* **Prefijo en API Gateway:** `/api/v1/ia`
* **Microservicio destino:** `ms-ia` (Puerto interno: `8086`)

| Método   | Endpoint                            | Descripción                                 | Body / Parámetros                  | FUNCIONAL |
|:-------- |:----------------------------------- |:------------------------------------------- |:---------------------------------- |:---------:|
| **POST** | `/api/v1/ia/consultar`              | Consulta genérica al coach financiero       | `SolicitudIaDTO` (JSON)            | `[ ]`     |
| **POST** | `/api/v1/ia/gasto-hormiga`          | Análisis e identificación de gastos hormiga | `PeticionConFiltroFechaDTO` (JSON) | `[ ]`     |
| **POST** | `/api/v1/ia/predecir-gastos`        | Proyecciones matemáticas de gastos futuros  | `PeticionConFiltroFechaDTO` (JSON) | `[ ]`     |
| **POST** | `/api/v1/ia/habitos-financieros`    | Análisis conductual sobre hábitos de gasto  | `PeticionConFiltroFechaDTO` (JSON) | `[ ]`     |
| **POST** | `/api/v1/ia/estilo-vida`            | Evaluación de estilo de vida del cliente    | `PeticionConFiltroFechaDTO` (JSON) | `[ ]`     |
| **POST** | `/api/v1/ia/reporte-completo`       | Compendio global de asesoría e IA           | `PeticionConFiltroFechaDTO` (JSON) | `[ ]`     |
| **POST** | `/api/v1/ia/simular-meta`           | Simula plan de ahorro y tiempo estimado     | `PeticionSimularMetaDTO` (JSON)    | `[ ]`     |
| **POST** | `/api/v1/ia/reto-ahorro`            | Genera retos de ahorro inteligentes         | `PeticionConFiltroFechaDTO` (JSON) | `[ ]`     |
| **POST** | `/api/v1/ia/clasificar-transaccion` | Clasifica un gasto en base a su descripción | `SolicitudClasificacionDTO` (JSON) | `[ ]`     |

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

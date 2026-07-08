# CGC Report

_Generated: 2026-06-22 19:41 UTC_

_Scoped to repository: `D:/CURSOS/7MO/INTEGRADOR/plataforma-de-microservicios-financieros`_


## God Nodes — Highest Fan-In
_These nodes are called from many places. High fan-in increases risk: a change here affects every caller._

| Kind | Name | File | In-degree |
| --- | --- | --- | --- |
|  | publicarAcceso | mensajeria/PublicadorAuditoria.java | 14 |
|  | EventoContextoActualizado | eventos/EventoContextoActualizado.java | 10 |
|  | publicarEventoSeguridad | mensajeria/PublicadorAuditoria.java | 9 |
|  | crearRespuestaError | excepciones/ManejadorGlobalExcepciones.java | 8 |
|  | publicarTransaccionExitosa | mensajeria/PublicadorAuditoria.java | 7 |
|  | formatMoneda | services/perfil-reporte.service.ts | 6 |
|  | obtenerListaMockActual | services/metas-utility.service.ts | 6 |
|  | ExcepcionAccesoDenegadoSuscripcion | excepciones/ExcepcionAccesoDenegadoSuscripcion.java | 6 |
|  | findByUsuarioIdAndActivoTrue | repositorios/LimiteGastoRepositorio.java | 6 |
|  | findByUsuarioId | repositorios/DatosPersonalesRepositorio.java | 5 |
|  | cargarDatos | services/perfil-financiero.service.ts | 5 |
|  | MensajeriaExternaException | excepciones/MensajeriaExternaException.java | 5 |
|  | publicarEventoExitoso | mensajeria/PublicadorAuditoria.java | 5 |
|  | findByUsuarioId | repositorios/IntentoValidacionRepository.java | 5 |
|  | publicarAcceso | mensajeria/PublicadorAuditoria.java | 5 |


## Most Complex Functions
_Cyclomatic complexity > 10 is a refactoring candidate._

| Function | File | Cyclomatic Complexity |
| --- | --- | --- |
| cargarDatos | services/perfil-financiero.service.ts | 43 |
| cargarDatos | services/ingresos-state.service.ts | 34 |
| calcularRacha | header/header.ts | 30 |
| procesar_modulo | core/servicio_analisis.py | 29 |
| procesar_comparacion | core/servicio_analisis.py | 27 |
| getResumen | services/Financiero.service.ts | 26 |
| cargarDatos | services/gastos-state.service.ts | 26 |
| getAhorroSugeridoInfo | meta-form-page/meta-form-page.ts | 24 |
| cargarResumen | services/dashboard-state.service.ts | 22 |
| listarMetas | services/cliente-metas-limites.service.ts | 19 |
| guardarGasto | gastos-page/gastos-page.ts | 18 |
| json_a_dataframe | utilidades/preparador_datos.py | 17 |
| listarMetasActivas | services/cliente-metas-limites.service.ts | 17 |
| get_dashboard_graficos | routers/dashboard.py | 14 |
| validarFormConfig | services/perfil-wizard.service.ts | 13 |


## Potential Dead Code
_Functions with zero callers (not guaranteed dead — may be entry points or called via reflection)._

| Function | File |
| --- | --- |
| main | gateway/ApiGatewayApplication.java |
| reactiveRedisTemplateBoolean | configuracion/ConfiguracionRedis.java |
| reactiveRedisTemplateString | configuracion/ConfiguracionRedis.java |
| securityWebFilterChain | configuracion/ConfiguracionSeguridadGateway.java |
| userKeyResolver | configuracion/ConfiguracionSeguridadGateway.java |
| loadBalancedWebClientBuilder | configuracion/ConfiguracionWebClient.java |
| ControladorDashboardBFF | controladores/ControladorDashboardBFF.java |
| getGraficos | controladores/ControladorDashboardBFF.java |
| getResumen | controladores/ControladorDashboardBFF.java |
| parseJsonSilently | controladores/ControladorDashboardBFF.java |
| fallback | controladores/FallbackController.java |
| filter | filtros/FiltroBloqueoIp.java |
| getOrder | filtros/FiltroBloqueoIp.java |
| filter | filtros/FiltroCuotaIA.java |
| getOrder | filtros/FiltroCuotaIA.java |
| filter | filtros/FiltroTrazabilidadGlobal.java |
| getOrder | filtros/FiltroTrazabilidadGlobal.java |
| filter | seguridad/FiltroJwtGlobal.java |
| getOrder | seguridad/FiltroJwtGlobal.java |
| SeguridadClient | seguridad/SeguridadClient.java |


## Suggested Cypher Queries
_Copy these into `execute_cypher_query` to explore further._

### Callers of a specific function
```cypher
MATCH (caller)-[:CALLS]->(fn:Function {name: 'yourFunctionName'})
RETURN caller.name, caller.path LIMIT 20
```

### Class hierarchy for a specific class
```cypher
MATCH path = (c:Class {name: 'YourClass'})-[:INHERITS*]->(parent)
RETURN [n IN nodes(path) | n.name] AS hierarchy
```

### Most-injected Spring beans
```cypher
MATCH ()-[:INJECTS]->(bean:Class)
RETURN bean.name, count(*) AS injection_count
ORDER BY injection_count DESC LIMIT 10
```

### All external library dependencies
```cypher
MATCH (m:MavenModule)-[:USES_LIBRARY]->(lib:ExternalLibrary)
RETURN m.artifact_id, lib.group_id, lib.artifact_id, lib.version
ORDER BY lib.artifact_id
```

### CALLS edges with low confidence (potential mis-resolutions)
```cypher
MATCH (a)-[c:CALLS]->(b)
WHERE c.confidence_label = 'AMBIGUOUS'
RETURN a.name, b.name, c.resolution_tier, a.path LIMIT 20
```

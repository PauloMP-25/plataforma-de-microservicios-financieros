# 🎯 **COMANDOS - Luka Ecosystem**

Centro de atajos para desarrollo completo del backend y frontend.

---

## 📋 **Estructura de Carpeta**

```
/media/paulo/datos/proyecto-desarrollo-web-integrado/comando/
├── Makefile                    # Comandos unificados
├── dev-frontend.sh            # Inicia Angular 17
├── start-grupo-1.sh           # API Gateway + Auditoría
├── start-grupo-2.sh           # Usuario + Cliente + Mensajería
├── start-grupo-3.sh           # Financiero + IA + Suscripciones + Pagos
├── luka-utils.sh              # Herramientas de utilidad
└── README.md                  # Este archivo
```

---

## 🚀 **OPCIÓN 1: Con MAKE (RECOMENDADO)**

### **Inicializar Microservicios por Grupos**

```bash
# Grupo 1: API Gateway + Auditoría (ejecutar primero)
make up-group-1

# Grupo 2: Usuario + Cliente + Mensajería (ejecutar después)
make up-group-2

# Grupo 3: Financiero + IA + Suscripciones + Pagos (ejecutar al último)
make up-group-3
```

### **Gestión General**

```bash
make status          # Ver estado de todos los contenedores
make logs            # Ver logs en tiempo real
make down            # Detener todos los contenedores
make clean           # Limpiar volúmenes (⚠️  destructivo)
make frontend        # Inicia Angular
make help            # Ver todos los comandos
```

---

## 🐚 **OPCIÓN 2: Scripts Directos**

### **Levantar Microservicios**

```bash
cd "/media/paulo/datos/proyecto-desarrollo-web-integrado/comando"

# Grupo 1 (ejecutar primero)
./start-grupo-1.sh

# Grupo 2 (ejecutar después)
./start-grupo-2.sh

# Grupo 3 (ejecutar al último)
./start-grupo-3.sh
```

### **Frontend**

```bash
./dev-frontend.sh
```

---

## 🛠️ **OPCIÓN 3: Utilidades**

```bash
./luka-utils.sh dashboard    # Dashboard completo del sistema
./luka-utils.sh logs <servicio>  # Ver logs de un servicio
./luka-utils.sh health       # Verificar salud de servicios
```

---

## 📊 **Jerarquía de Arranque (IMPORTANTE)**

Los grupos **DEBEN levantarse en orden**, cada uno depende del anterior:

```
┌─────────────────────────────────────────────────────────────┐
│  GRUPO 1: API Gateway + Auditoría (PRIMERO)                │
│  ├─ PostgreSQL (7 instancias)                              │
│  ├─ RabbitMQ                                               │
│  ├─ API Gateway (puerto 8080)                              │
│  └─ Microservicio Auditoría                                │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GRUPO 2: Usuario + Cliente + Mensajería (SEGUNDO)         │
│  ├─ Microservicio Usuario                                  │
│  ├─ Microservicio Cliente                                  │
│  └─ Microservicio Mensajería                               │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  GRUPO 3: Financiero + IA + Suscripciones + Pagos (TERCERO)│
│  ├─ Microservicio Financiero                               │
│  ├─ Microservicio IA                                       │
│  ├─ Microservicio Suscripciones                            │
│  └─ Microservicio Pagos                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 🌐 **Acceso a Servicios**

| Servicio | URL | Puerto |
|----------|-----|--------|
| API Gateway | http://localhost:8080 | 8080 |
| RabbitMQ Management | http://localhost:15672 | 15672 |
| PostgreSQL (Usuario) | localhost:5432 | 5432 |
| PostgreSQL (Auditoria) | localhost:5433 | 5433 |
| Frontend Angular | http://localhost:4200 | 4200 |

**Credenciales RabbitMQ:**
- Usuario: `guest`
- Contraseña: `guest`

---

## 💾 **Almacenamiento**

Todos los volúmenes Docker se almacenan en:
```
/media/paulo/datos/ (932 GB)
```

Volúmenes existentes:
- `vol-postgres-usuario`
- `vol-postgres-cliente`
- `vol-postgres-financiero`
- `vol-postgres-auditoria`
- `vol-postgres-mensajeria`
- `vol-postgres-ia`
- `vol-postgres-pagos`
- `vol-rabbitmq-data`
- `vol-rabbitmq-logs`

---

## 🔧 **Troubleshooting**

### **"Error: Grupo 1 no está ejecutándose"**
```bash
# Asegúrate de ejecutar primero
make up-group-1
```

### **Ver logs de un servicio**
```bash
make logs
# o
./luka-utils.sh logs api-gateway
```

### **Detener todo**
```bash
make down
```

### **Limpiar datos completamente**
```bash
make clean
```

---

## 📚 **Más Información**

- **Backend**: `estructura-backend/ESTANDAR_ARQUITECTURA_MICROSERVICIOS.md`
- **Frontend**: Ver `estructura-frontend/README.md`
- **Docker Compose**: `estructura-backend/docker/docker-compose.yml`

---

**Última actualización:** 2024  
**Versión:** 1.0  
**Partición:** `/media/paulo/datos/` (932 GB)

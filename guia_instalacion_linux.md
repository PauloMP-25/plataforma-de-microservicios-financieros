# 🚀 Guía de Instalación: Nuevo Entorno Linux (Luka App)

¡Felicidades por tomar la decisión de limpiar tu entorno! Con una única partición grande en tu SSD, te olvidarás para siempre de los problemas de espacio y permisos.

Guarda esta guía en tu disco mecánico (`Nuevo vol`) para que la tengas a la mano en cuanto instales tu nuevo Ubuntu. 

---

## 1. Actualización del Sistema Base
Abre tu terminal en el nuevo sistema y ejecuta esto primero:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl wget git build-essential apt-transport-https software-properties-common
```

## 2. Herramientas de Desarrollo (IDEs y Clientes)

### Visual Studio Code
```bash
sudo snap install code --classic
```

### Postman
```bash
sudo snap install postman
```

### GitHub Desktop (Vía repositorio oficial para Linux)
```bash
wget -qO - https://apt.packages.shiftkey.dev/gpg.key | gpg --dearmor | sudo tee /usr/share/keyrings/shiftkey-packages.gpg > /dev/null
sudo sh -c 'echo "deb [arch=amd64 signed-by=/usr/share/keyrings/shiftkey-packages.gpg] https://apt.packages.shiftkey.dev/ubuntu/ any main" > /etc/apt/sources.list.d/shiftkey-packages.list'
sudo apt update && sudo apt install -y github-desktop
```

### Apache NetBeans
```bash
sudo snap install netbeans --classic
```

## 3. Entornos de Programación (Java, Python, Node.js)

### Java 21 (Requerido para Spring Boot)
```bash
sudo apt install -y openjdk-21-jdk maven
# Verificar instalación
java -version
mvn -version
```

### Python 3.12 (Requerido para FastAPI y Gemini)
```bash
sudo apt install -y python3.12 python3.12-venv python3-pip
# Verificar instalación
python3.12 --version
```

### Node.js y Angular CLI (Requerido para Frontend)
```bash
# Instalar Node.js versión LTS (v20)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
# Instalar Angular CLI globalmente
sudo npm install -g @angular/cli
# Verificar instalación
node -v
ng version
```

### Comandos Globales (Opcional)
Si tenías alias configurados, recuerda agregarlos al final de tu archivo `~/.bashrc`. Ejemplo:
```bash
echo 'alias luka="cd /media/paulo/Nuevo\ vol/proyecto-desarrollo-web-integrado && ls -la"' >> ~/.bashrc
source ~/.bashrc
```

## 4. Entorno Docker (¡Altamente Recomendado!)

Como Ingeniero, te recomiendo fuertemente usar Docker en lugar de instalar Postgres, Redis y RabbitMQ directamente en Linux. Esto mantendrá tu sistema nuevo **completamente limpio**. Podrás usar el script `./dev-backend.sh --produccion` y Docker descargará las bases de datos por ti.

```bash
# Instalar Docker
sudo apt install -y docker.io docker-compose
sudo systemctl enable docker
sudo systemctl start docker

# Darte permisos para usar Docker sin escribir "sudo"
sudo usermod -aG docker $USER
```
*(Nota: Tendrás que reiniciar tu computadora para que el permiso de Docker sin sudo haga efecto).*

## 5. Clientes de Bases de Datos (Para conectarte a Docker o Producción)

### pgAdmin 4 (Cliente para Postgres)
```bash
curl -fsS https://www.pgadmin.org/static/packages_pgadmin_org.pub | sudo gpg --dearmor -o /usr/share/keyrings/packages-pgadmin-org.gpg
sudo sh -c 'echo "deb [signed-by=/usr/share/keyrings/packages-pgadmin-org.gpg] https://ftp.postgresql.org/pub/pgadmin/pgadmin4/apt/$(lsb_release -cs) pgadmin4 main" > /etc/apt/sources.list.d/pgadmin4.list'
sudo apt update
sudo apt install -y pgadmin4-desktop
```

### RedisInsight (Cliente para Redis)
```bash
# Descarga e instala RedisInsight vía Snap o Flatpak
sudo snap install redisinsight
```

## 6. Google Antigravity (AGY)
Ya que tienes el archivo `.tar` en tu disco mecánico, los pasos en tu nuevo Linux serán:
1. Extraer el `.tar`.
2. Ejecutar su instalador interno o colocarlo en tu `$PATH` según las instrucciones oficiales.
3. Restaurar tu llave en `~/.gemini/config/`.

---

**Si de verdad quieres instalar Postgres, Redis y RabbitMQ de forma nativa**, usa estos comandos. Pero te advierto que consumirán RAM permanentemente en el fondo de tu sistema, a diferencia de Docker que solo los enciende cuando se los pides.
```bash
# Solo si no vas a usar Docker para las BDs
sudo apt install -y postgresql redis-server rabbitmq-server
```

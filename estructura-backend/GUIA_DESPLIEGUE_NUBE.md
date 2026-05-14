# 🚀 Guía: Despliegue de LUKA-COACH en la Nube (CI/CD)

Esta guía te permitirá mover el peso de la construcción de imágenes de tu laptop a la nube de GitHub.

## 1. Preparación en Docker Hub
1. Crea una cuenta en [hub.docker.com](https://hub.docker.com/).
2. Crea un **Personal Access Token** (PAT) en Settings -> Security.

## 2. Configuración en GitHub
En tu repositorio de GitHub, ve a **Settings -> Secrets and variables -> Actions** y añade:
* `DOCKERHUB_USERNAME`: Tu usuario de Docker Hub.
* `DOCKERHUB_TOKEN`: El token que generaste en el paso anterior.

## 3. El Workflow (La Magia)
Debes crear un archivo en `.github/workflows/deploy.yml` con este contenido (resumen):

```yaml
name: Deploy to Docker Hub
on:
  push:
    branches: [ main ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Build with Maven
        run: mvn clean package -DskipTests
      - name: Push to Docker Hub
        run: |
          docker build -t tu-usuario/ms-usuario ./microservicio-usuario
          docker push tu-usuario/ms-usuario
```

## 4. Beneficios
* **Laptop Fría:** Tu laptop solo descarga imágenes, no las construye.
* **Velocidad:** GitHub tiene internet de gigabits; descarga librerías en segundos.
* **Profesionalismo:** Cada vez que hagas `git push`, tu app se actualiza sola.

---
*Generado por Luka-Architect — 2026*

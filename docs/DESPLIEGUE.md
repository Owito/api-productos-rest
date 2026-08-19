# Despliegue en Koyeb

La aplicación se empaqueta con el `Dockerfile` de la raíz y se despliega en el plan gratuito de
[Koyeb](https://www.koyeb.com), que a diferencia de otras capas gratuitas **no duerme el servicio
por inactividad**. La base de datos sigue siendo la de Neon.

---

## Antes de empezar

- La rama `main` del repositorio está al día. Koyeb construye desde ahí.
- La cadena de conexión de Neon a mano. Está en el `.env` local, que **no** se versiona ni entra
  a la imagen (`.dockerignore` lo excluye).

## Camino corto: la consola de Koyeb

1. Entrar a [app.koyeb.com](https://app.koyeb.com) e iniciar sesión con GitHub.
2. **Create Service** → **Web Service** → **GitHub** → repositorio `Owito/api-productos-rest`,
   rama `main`.
3. **Builder:** elegir **Dockerfile**. Koyeb detecta el de la raíz; no hace falta indicar ruta.
4. **Instance:** `Free`. **Region:** `Washington, D.C.` (`was`), que es la más cercana a la base de
   datos de Neon en `us-east-2` y evita cruzar el Atlántico en cada consulta.
5. **Exposed port:** `8080`, protocolo HTTP, ruta `/`.
6. **Health check:** HTTP sobre el puerto `8080`, ruta `/actuator/health`.
   **Grace period: 300 segundos.** Este es el ajuste que más se olvida: la instancia gratuita tiene
   una décima de vCPU y la JVM tarda varios minutos en levantar ahí. Con el periodo de gracia por
   defecto, Koyeb mata el contenedor antes de que alcance a arrancar y entra en un ciclo de
   reinicios que parece un error de la aplicación sin serlo.
7. **Environment variables:**

   | Variable | Valor | Tipo |
   |---|---|---|
   | `SPRING_PROFILES_ACTIVE` | `neon` | Plain |
   | `DB_URL` | `jdbc:postgresql://<host>.neon.tech/neondb?sslmode=require` | Plain |
   | `DB_USERNAME` | `neondb_owner` | Plain |
   | `DB_PASSWORD` | la contraseña de Neon | **Secret** |
   | `APP_DATOS_DEMO` | `true` | Plain |

   `DB_PASSWORD` va como **Secret**, no como variable normal: así no queda visible en la
   configuración del servicio ni en los registros.

   No hay que definir `PORT`: Koyeb la inyecta y la aplicación la lee (`server.port: ${PORT:8080}`).

8. **Deploy.** La primera construcción tarda varios minutos porque descarga Gradle y las
   dependencias. Las siguientes reutilizan la caché de capas.

## Camino alterno: la CLI

```bash
koyeb service create api \
  --app api-productos-rest \
  --git github.com/Owito/api-productos-rest \
  --git-branch main \
  --git-builder docker \
  --instance-type free \
  --regions was \
  --ports 8080:http \
  --routes /:8080 \
  --checks 8080:http:/actuator/health \
  --env SPRING_PROFILES_ACTIVE=neon \
  --env DB_URL="jdbc:postgresql://<host>.neon.tech/neondb?sslmode=require" \
  --env DB_USERNAME=neondb_owner \
  --env DB_PASSWORD=@db-password \
  --env APP_DATOS_DEMO=true
```

`@db-password` referencia un secreto creado antes con
`koyeb secret create db-password --value "<contrasena>"`, para que no quede en el historial del
terminal.

---

## Verificar que quedó bien

Reemplazar `<url>` por el dominio que asigna Koyeb.

```bash
curl -i https://<url>/actuator/health          # {"status":"UP"}
curl https://<url>/api/v1/productos            # 18 productos
curl https://<url>/api/v1/productos/categorias # las ocho categorias
curl -o /dev/null -s -w "%{http_code}\n" https://<url>/productos   # 200, interfaz web
```

En el navegador: `/productos` (catálogo), `/creditos` y `/swagger-ui.html`.

## Qué esperar

- **La primera petición después del despliegue puede tardar.** Con una décima de vCPU la JVM
  arranca lento. Una vez arriba, el servicio se queda arriba: el plan gratuito de Koyeb no lo
  duerme.
- **La base se puebla sola.** `APP_DATOS_DEMO=true` siembra 18 productos si la tabla está vacía.
  Si alguien borra todo probando, el catálogo vuelve en el siguiente reinicio del servicio.
- **La API está abierta a propósito.** No hay autenticación: cualquiera puede crear, editar y
  borrar. Es una demostración con datos de ejemplo, y esa es la decisión tomada. Si algún día se
  publica algo que importe, esto tiene que cambiar antes.

## Actualizar

Koyeb queda enganchado a la rama `main`: cada `git push` dispara una construcción y un despliegue
nuevos. Para desplegar sin empujar código, **Redeploy** en la consola.

## Costos

Plan gratuito de Koyeb: un servicio con instancia `free`. Neon: capa gratuita, 0,5 GB de
almacenamiento. Ninguno de los dos pide tarjeta ni genera cobro con este uso.

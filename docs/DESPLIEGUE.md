# Despliegue en Render

La aplicación se empaqueta con el `Dockerfile` de la raíz y se despliega en el plan gratuito de
[Render](https://render.com), contra la misma base de datos de Neon.

**Servicio en producción:** <https://api-productos-rest.onrender.com>
(`srv-da2nrurl550s73cavaj0`, plano `exs-da2hrdlg1s2s73cppqbg`, región Ohio).

> Se evaluó Koyeb primero, porque su capa gratuita no dormía el servicio. **Dejó de ser viable:**
> la plataforma fue adquirida por Mistral y su consola ya no permite crear servicios. Render no
> tiene esa limitación de disponibilidad, pero sí duerme el servicio por inactividad, y eso se
> compensa con el workflow de mantenimiento descrito abajo.

---

## Antes de empezar

- La rama `main` está al día. Render construye desde ahí.
- La cadena de conexión de Neon a mano. Vive en el `.env` local, que **no** se versiona ni entra a
  la imagen (`.dockerignore` lo excluye).

## Crear el servicio

El repositorio trae un `render.yaml`, así que el camino corto es un plano:

1. Entrar a [dashboard.render.com](https://dashboard.render.com) e iniciar sesión con GitHub.
2. **New** → **Blueprint** → elegir el repositorio `Owito/api-productos-rest`.
3. Render lee `render.yaml` y propone el servicio ya configurado: Docker, plan gratuito, región
   Ohio, chequeo de salud en `/actuator/health` y las variables de entorno.
4. Pedirá **un solo valor**: `DB_PASSWORD`, porque está marcada con `sync: false` justamente para
   que la contraseña no viva en el repositorio. Pegar ahí la de Neon.
5. **Apply**. La primera construcción tarda varios minutos: descarga Gradle y las dependencias.
   Las siguientes reutilizan la caché de capas.

Si se prefiere el formulario manual (**New** → **Web Service**), la configuración equivalente es
runtime Docker, plan Free, región Ohio, rama `main`, health check `/actuator/health`, y las cinco
variables de entorno de `render.yaml`.

## Mantener el servicio despierto

El plan gratuito duerme el servicio tras 15 minutos sin tráfico y despertarlo tarda cerca de un
minuto, lo que arruina la primera impresión de quien entra a probar.

El repositorio incluye `.github/workflows/mantener-despierto.yml`, que lo pinga cada 10 minutos.
Para activarlo hay que crear **una variable de repositorio** en GitHub:

**Settings → Secrets and variables → Actions → Variables → New repository variable**

| Nombre | Valor |
|---|---|
| `URL_APP` | `https://api-productos-rest.onrender.com` |

Sin esa variable el workflow no falla: avisa y no hace nada. Ya está creada.

### Por qué el horario no es de 24 horas

Render da **750 horas de instancia al mes por cuenta, no por servicio**. Esta cuenta ya tiene otro
servicio en el plan gratuito, así que mantener este despierto todo el día consumiría la cuota
completa y dejaría al otro sin margen.

La ventana va de **7:00 a 23:00 de Bogotá**, que es cuando alguien va a estar probando. Fuera de
ese rango el servicio duerme y no consume horas. Son unas 480 horas al mes, que dejan espacio para
el otro servicio.

Si en algún momento este es el único servicio gratuito de la cuenta, se puede cambiar el `cron` del
workflow a `*/10 * * * *` y quedará despierto todo el día.

## Verificar que quedó bien

```bash
B=https://api-productos-rest.onrender.com
curl -i $B/actuator/health           # {"status":"UP"}
curl $B/api/v1/productos             # 18 productos
curl $B/api/v1/productos/categorias  # las ocho categorias
curl "$B/api/v1/productos?categoria=AUDIO"   # filtro por categoria
curl -o /dev/null -s -w "%{http_code}\n" $B/productos   # 200, interfaz web
```

En el navegador: `/productos` (catálogo), `/creditos` y `/swagger-ui.html`.

Verificado el 19 de agosto de 2026 contra el despliegue real: los cinco endpoints en 200, el
catálogo sembrado con 18 productos, el filtro por categoría, el ciclo completo POST/PUT/DELETE, y
el manejo de errores devolviendo 400 con datos inválidos, 404 con un id inexistente y 409 con un
nombre duplicado.

## Qué esperar

- **La primera petición después de un periodo dormido tarda cerca de un minuto.** Es el arranque en
  frío del plan gratuito, no un problema de la aplicación. Con el workflow activo casi nunca pasa
  dentro de la ventana horaria.
- **La base se puebla sola.** `APP_DATOS_DEMO=true` siembra 18 productos si la tabla está vacía. Si
  alguien borra todo probando, el catálogo vuelve en el siguiente arranque del servicio.
- **La API está abierta a propósito.** No hay autenticación: cualquiera puede crear, editar y
  borrar. Es una demostración con datos de ejemplo, y esa es la decisión tomada. Si algún día se
  publica algo que importe, esto tiene que cambiar antes.

## Actualizar

El servicio se creó conectando el **repositorio público por URL**, y no a través de la aplicación
de GitHub de Render. Se eligió así para no ampliar los permisos que Render tiene sobre la cuenta de
GitHub: su instalación solo alcanza cuatro repositorios y este no estaba entre ellos.

El costo de esa decisión es que **el auto-despliegue no funciona**: aunque `render.yaml` declare
`autoDeploy: true`, Render no puede instalar el webhook en un repositorio que no administra. Cada
actualización se dispara con **Manual Deploy** en el panel.

Para recuperar el auto-despliegue basta con agregar el repositorio a la instalación de Render en
GitHub (**Configure account** en la pantalla de conexión) y volver a crear el servicio conectándolo
desde la lista en vez de por URL.

## Costos

Render: plan gratuito, sin tarjeta. Neon: capa gratuita, 0,5 GB de almacenamiento. Ninguno de los
dos genera cobro con este uso.

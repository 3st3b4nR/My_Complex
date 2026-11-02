# Firebase Cloud Messaging para tareas

## Alcance y arquitectura

`notifications` conserva instalaciones web y un outbox durable en PostgreSQL.
El frontend registra una Firebase Installation ID (FID) solo después de que el
usuario pulsa **Activar notificaciones** y concede permiso. El dispatcher usa
Firebase Admin SDK detrás de un output port, aplica reintentos acotados y
revoca destinos que Firebase declara inválidos. La creación de tareas no
depende de la disponibilidad de FCM.

Esta entrega solo notifica asignaciones y reasignaciones. No incluye campana,
correo, recordatorios, panel del trabajador, aplicación móvil ni Firebase
In-App Messaging.

## Configuración en Firebase Console

1. Abra el proyecto controlado de Firebase y habilite Cloud Messaging.
2. Registre una aplicación web para MyComplex.
3. En Cloud Messaging, genere o seleccione la clave pública VAPID web.
4. Copie únicamente la configuración pública de la aplicación web a las
   variables locales indicadas abajo.
5. Para el backend, use una cuenta de servicio de mínimo privilegio disponible
   mediante Application Default Credentials. No descargue ni versione una
   clave como parte de esta implementación.

## Variables

- `FCM_ENABLED`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CREDENTIALS_PATH` (solo para el override de Docker)
- `GOOGLE_APPLICATION_CREDENTIALS`
- `FIREBASE_WEB_API_KEY`
- `FIREBASE_WEB_AUTH_DOMAIN`
- `FIREBASE_WEB_PROJECT_ID`
- `FIREBASE_WEB_STORAGE_BUCKET`
- `FIREBASE_WEB_MESSAGING_SENDER_ID`
- `FIREBASE_WEB_APP_ID`
- `FIREBASE_WEB_VAPID_KEY`
- `FCM_MAX_RETRIES`
- `FCM_RETRY_DELAY`
- `FCM_DISPATCH_INTERVAL`

Con `FCM_ENABLED=false` o credenciales ausentes el backend inicia normalmente,
la configuración web se publica como deshabilitada y los eventos no se marcan
como enviados.

El archivo versionado `compose.firebase.yaml` agrega el montaje únicamente
cuando se incluye de forma explícita. La ruta de Windows se recibe mediante
`FIREBASE_CREDENTIALS_PATH`; dentro del contenedor siempre se utiliza
`/run/secrets/firebase-service-account.json`. El montaje es de solo lectura y
Docker no crea silenciosamente un archivo ausente.

En PowerShell, habilite FCM así, reemplazando únicamente la ruta local:

```powershell
$env:FIREBASE_CREDENTIALS_PATH = (Resolve-Path 'C:\ruta-segura\firebase-service-account.json').Path
docker compose -f compose.yaml -f compose.firebase.yaml config -q
docker compose -f compose.yaml -f compose.firebase.yaml up -d backend gateway
```

Para volver al modo deshabilitado, no incluya el override:

```powershell
$env:FCM_ENABLED = 'false'
docker compose -f compose.yaml up -d --force-recreate backend gateway
Remove-Item Env:FIREBASE_CREDENTIALS_PATH -ErrorAction SilentlyContinue
```

No copie la cuenta de servicio dentro de la imagen. El archivo real y sus
valores no deben aparecer en logs, documentación, commits ni respuestas REST.
Cuando el override no se incluye, el archivo privado no es obligatorio y
`FCM_ENABLED=false` conserva el arranque normal.

## Prueba manual controlada

1. Use una cuenta de trabajador de prueba ya vinculada.
2. Abra MyComplex en `localhost` o HTTPS; fuera de localhost, los service
   workers y las notificaciones requieren un contexto seguro.
3. En selección de copropiedad pulse **Activar notificaciones** y conceda el
   permiso del navegador.
4. Compruebe que el registro técnico finalice sin mostrar el FID.
5. Desde una cuenta administradora cree una tarea de prueba para ese
   trabajador, sin información sensible.
6. Verifique por separado el estado del outbox y la aceptación de FCM. La
   aceptación de FCM no garantiza que el dispositivo haya mostrado el mensaje.
7. Use **Desactivar** para revocar la instalación controlada al terminar.

Las pruebas automatizadas sustituyen el puerto de entrega y nunca llaman FCM
real.

## Solución de problemas

- `disabled`: confirme las variables y las credenciales externas del backend.
- `blocked`: habilite el permiso desde la configuración del navegador.
- `NO_DESTINATION`: el usuario no tiene una instalación activa; debe activarla
  explícitamente.
- `invalid_destination`: el backend revoca automáticamente la instalación; el
  usuario debe activarla nuevamente.
- fallos reintentables: consulte las métricas y el runbook
  `observability/runbooks/fcm-delivery-failures.md` sin exponer identificadores.

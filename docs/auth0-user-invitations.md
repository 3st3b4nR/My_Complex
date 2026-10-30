# Configuración de invitaciones de usuarios con Auth0

Esta integración se configura fuera del repositorio. No almacene secretos en
archivos versionados ni exponga el token de Management API al frontend.

## Aplicación Machine-to-Machine

1. En Auth0 Dashboard, cree una aplicación de tipo **Machine to Machine**.
2. Autorícela para la **Auth0 Management API** del tenant.
3. Conceda únicamente los scopes `create:users` y `read:users`.
4. Confirme el nombre de la conexión de base de datos que utilizará MyComplex.
5. Habilite esa conexión para la aplicación web de MyComplex.
6. Configure un proveedor de correo en Auth0.
7. Personalice la plantilla **Change Password** como invitación para que la
   persona establezca su propia contraseña.

## Post-Login Action

Cree y despliegue manualmente una Action en el flujo Post Login. La Action no
contiene secretos de MyComplex y solo limpia la marca de invitación:

```javascript
exports.onExecutePostLogin = async (event, api) => {
  if (event.user.app_metadata?.needsInvitation) {
    api.user.setAppMetadata("needsInvitation", false);
  }
};
```

El backend marca el estado local como vinculado cuando recibe un JWT válido;
no depende de un webhook ni de la ejecución de esta Action.

## Variables del backend

Configure manualmente en el entorno del backend:

- `AUTH0_MANAGEMENT_DOMAIN`
- `AUTH0_MANAGEMENT_CLIENT_ID`
- `AUTH0_MANAGEMENT_CLIENT_SECRET`
- `AUTH0_MANAGEMENT_AUDIENCE`
- `AUTH0_DATABASE_CONNECTION`
- `AUTH0_APPLICATION_CLIENT_ID`

Opcionalmente puede ajustar:

- `AUTH0_MANAGEMENT_CONNECT_TIMEOUT`
- `AUTH0_MANAGEMENT_RESPONSE_TIMEOUT`
- `AUTH0_MANAGEMENT_TOKEN_EXPIRY_MARGIN`
- `AUTH0_PROVISIONING_TIMEOUT`
- `AUTH0_INVITATION_RESEND_COOLDOWN`

Reinicie el backend y pruebe primero con una dirección de correo controlada.
MyComplex crea la identidad con `email_verified=false`,
`app_metadata.needsInvitation=true` y el identificador local
`app_metadata.mycomplexUserId`. Los roles y permisos de copropiedad permanecen
exclusivamente en PostgreSQL.

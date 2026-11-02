# Gestión administrativa de tareas

El módulo `tasks` permite a un miembro activo con rol `PROPERTY_ADMIN` listar,
crear, editar, reasignar y cambiar el estado de tareas dentro de su
copropiedad. Cada consulta y mutación incluye `propertyId`; el backend vuelve a
validar la membresía administrativa y los trabajadores elegibles para impedir
acceso cruzado entre copropiedades.

## Endpoints

- `GET /api/properties/{propertyId}/tasks`
- `GET /api/properties/{propertyId}/tasks/{taskId}`
- `POST /api/properties/{propertyId}/tasks`
- `PUT /api/properties/{propertyId}/tasks/{taskId}`
- `PATCH /api/properties/{propertyId}/tasks/{taskId}/status`
- `GET /api/properties/{propertyId}/task-workers`

Los estados permitidos son `PENDING`, `IN_PROGRESS`, `COMPLETED` y
`CANCELLED`. Una tarea terminal no puede editarse. Las mutaciones usan versión
optimista y generan auditoría durable. La asignación y reasignación registran
un evento de notificación en la misma transacción de negocio.

`tasks` consume exclusivamente las APIs públicas de `users`, `notifications` y
`observability`; no accede a repositorios internos de otros módulos.

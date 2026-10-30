# Vinculación en el primer acceso autenticado

**Estado:** Proposed

## Contexto y problema

Cada request con JWT ejecuta el caso de uso que enlaza una identidad Auth0 con
el usuario local. Se evaluó evitar esa consulta mediante caché.

## Alternativas

- Mantener la comprobación idempotente por request.
- Caché local.
- Confiar en un dato enviado por Angular.

## Decisión propuesta

Mantener temporalmente el filtro. El subject proviene exclusivamente del JWT
validado. La persistencia hace una lectura y solo escribe al transicionar desde
`PROVISIONADO` o `INVITADO`; `VINCULADO` retorna sin escritura. No se introduce
caché porque una invalidación incompleta podría afectar seguridad y consistencia.

## Consecuencias y riesgos

La transición es segura e idempotente, pero cada request autenticado conserva
una consulta ligera. Si la medición de Fase 3 demuestra impacto, se diseñará un
mecanismo invalidable antes de optimizar.

## Evidencia y pruebas

`AuthenticatedUserLinkingFilter`, `LinkAuthenticatedUserService` y
`UserInvitationPersistenceIntegrationTest` cubren transición e idempotencia.

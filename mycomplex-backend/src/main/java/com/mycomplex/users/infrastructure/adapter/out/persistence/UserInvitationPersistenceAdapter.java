package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.exception.PropertyMemberNotFoundException;
import com.mycomplex.users.application.exception.PropertyMembersAccessDeniedException;
import com.mycomplex.users.application.exception.UserInvitationException;
import com.mycomplex.users.application.port.out.UserInvitationPersistencePort;
import com.mycomplex.users.domain.model.InvitationClaim;
import com.mycomplex.users.domain.model.PropertyMemberInvitation;
import com.mycomplex.users.domain.model.UserRegistrationStatus;

@Component
public class UserInvitationPersistenceAdapter
        implements UserInvitationPersistencePort {

    private final JdbcTemplate jdbcTemplate;
    private final AuthenticatedUserLinkPersistence authenticatedUserLink;
    private final UserInvitationStatePersistence invitationState;

    public UserInvitationPersistenceAdapter(
            JdbcTemplate jdbcTemplate,
            AuthenticatedUserLinkPersistence authenticatedUserLink,
            UserInvitationStatePersistence invitationState
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.authenticatedUserLink = authenticatedUserLink;
        this.invitationState = invitationState;
    }

    @Override
    @Transactional
    public InvitationClaim claim(
            UUID propertyId,
            UUID membershipId,
            String administratorSubject,
            boolean resend,
            Duration provisioningTimeout,
            Duration resendCooldown
    ) {
        lockActiveProperty(propertyId);
        verifyAdministrator(propertyId, administratorSubject);
        InvitationUserRow user = lockTargetUser(propertyId, membershipId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UserRegistrationStatus status = mapStatus(user.registrationStatus());

        return switch (status) {
            case PENDING -> {
                if (resend) {
                    throw invalidState("La identidad todavía no ha sido creada.");
                }
                markProvisioning(user.userId(), now);
                yield claim(user, UserRegistrationStatus.PROVISIONING,
                        InvitationClaim.Action.PROVISION);
            }
            case PROVISIONING -> {
                if (resend) {
                    throw invalidState("La identidad se está aprovisionando.");
                }
                if (user.lastAttemptAt() != null
                        && user.lastAttemptAt().isAfter(
                                now.minus(provisioningTimeout))) {
                    throw new UserInvitationException(
                            "INVITATION_IN_PROGRESS",
                            "La identidad se está aprovisionando. Inténtalo más tarde."
                    );
                }
                markProvisioning(user.userId(), now);
                yield claim(user, UserRegistrationStatus.PROVISIONING,
                        InvitationClaim.Action.PROVISION);
            }
            case PROVISIONED -> {
                if (user.lastAttemptAt() != null
                        && user.lastAttemptAt().isAfter(
                                now.minus(provisioningTimeout))) {
                    throw new UserInvitationException(
                            "INVITATION_IN_PROGRESS",
                            "Ya existe una solicitud en curso. Inténtalo más tarde."
                    );
                }
                markSending(user.userId(), now);
                yield claim(user, status,
                        InvitationClaim.Action.SEND_INVITATION);
            }
            case INVITED -> {
                if (!resend) {
                    yield claim(user, status, InvitationClaim.Action.NONE);
                }
                if (user.invitationSentAt() != null
                        && user.invitationSentAt().isAfter(
                                now.minus(resendCooldown))) {
                    throw new UserInvitationException(
                            "INVITATION_RESEND_TOO_SOON",
                            "Espera antes de reenviar la invitación."
                    );
                }
                markSending(user.userId(), now);
                yield claim(user, status,
                        InvitationClaim.Action.SEND_INVITATION);
            }
            case LINKED -> {
                if (resend) {
                    throw invalidState("La identidad ya se encuentra vinculada.");
                }
                yield claim(user, status, InvitationClaim.Action.NONE);
            }
        };
    }

    @Override
    @Transactional
    public void markProvisioned(UUID userId, String auth0Subject) {
        invitationState.markProvisioned(userId, auth0Subject);
    }

    @Override
    @Transactional
    public PropertyMemberInvitation markInvited(
            UUID userId,
            UUID membershipId
    ) {
        return invitationState.markInvited(userId, membershipId);
    }

    @Override
    @Transactional
    public void recordProvisioningFailure(UUID userId, String safeError) {
        invitationState.recordProvisioningFailure(userId, safeError);
    }

    @Override
    @Transactional
    public void recordInvitationFailure(UUID userId, String safeError) {
        invitationState.recordInvitationFailure(userId, safeError);
    }

    @Override
    @Transactional
    public void linkAuthenticatedUser(String auth0Subject) {
        authenticatedUserLink.link(auth0Subject);
    }

    private void lockActiveProperty(UUID propertyId) {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT id FROM copropiedades WHERE id = ? AND estado = 'ACTIVA' AND deleted_at IS NULL FOR UPDATE",
                    UUID.class, propertyId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new PropertyMembersAccessDeniedException();
        }
    }

    private void verifyAdministrator(
            UUID propertyId,
            String administratorSubject
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuarios_copropiedades membresia
                JOIN usuarios usuario ON usuario.id = membresia.usuario_id
                WHERE membresia.copropiedad_id = ?
                  AND usuario.auth0_subject = ?
                  AND usuario.estado = 'ACTIVO'
                  AND usuario.deleted_at IS NULL
                  AND membresia.rol = 'ADMIN_COPROPIEDAD'
                  AND membresia.estado = 'ACTIVA'
                  AND membresia.deleted_at IS NULL
                """,
                Integer.class, propertyId, administratorSubject
        );
        if (count == null || count == 0) {
            throw new PropertyMembersAccessDeniedException();
        }
    }

    private InvitationUserRow lockTargetUser(
            UUID propertyId,
            UUID membershipId
    ) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT usuario.id, usuario.correo, usuario.nombres,
                           usuario.apellidos, usuario.estado_registro,
                           usuario.auth0_subject,
                           membresia.id AS membership_id,
                           usuario.ultimo_intento_registro_at,
                           usuario.invitacion_enviada_at
                    FROM usuarios_copropiedades membresia
                    JOIN usuarios usuario ON usuario.id = membresia.usuario_id
                    WHERE membresia.id = ?
                      AND membresia.copropiedad_id = ?
                      AND membresia.estado = 'ACTIVA'
                      AND membresia.deleted_at IS NULL
                      AND usuario.estado = 'ACTIVO'
                      AND usuario.deleted_at IS NULL
                    FOR UPDATE OF usuario, membresia
                    """,
                    (resultSet, rowNumber) -> new InvitationUserRow(
                            resultSet.getObject("membership_id", UUID.class),
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("correo"),
                            resultSet.getString("nombres"),
                            resultSet.getString("apellidos"),
                            resultSet.getString("estado_registro"),
                            resultSet.getString("auth0_subject"),
                            toOffsetDateTime(resultSet.getTimestamp(
                                    "ultimo_intento_registro_at")),
                            toOffsetDateTime(resultSet.getTimestamp(
                                    "invitacion_enviada_at"))
                    ),
                    membershipId, propertyId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new PropertyMemberNotFoundException();
        }
    }

    private void markProvisioning(UUID userId, OffsetDateTime now) {
        jdbcTemplate.update(
                """
                UPDATE usuarios
                SET estado_registro = 'APROVISIONANDO',
                    intentos_registro = intentos_registro + 1,
                    ultimo_intento_registro_at = ?,
                    ultimo_error_registro = NULL,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ?
                """,
                now, userId
        );
    }

    private void markSending(UUID userId, OffsetDateTime now) {
        jdbcTemplate.update(
                """
                UPDATE usuarios
                SET intentos_registro = intentos_registro + 1,
                    ultimo_intento_registro_at = ?,
                    ultimo_error_registro = NULL,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = ?
                """,
                now, userId
        );
    }

    private InvitationClaim claim(
            InvitationUserRow user,
            UserRegistrationStatus status,
            InvitationClaim.Action action
    ) {
        return new InvitationClaim(
                user.membershipId(), user.userId(), user.email(),
                user.firstNames(), user.lastNames(), status,
                user.auth0Subject(), user.invitationSentAt(), action
        );
    }

    private UserInvitationException invalidState(String message) {
        return new UserInvitationException(
                "INVITATION_INVALID_STATE", message
        );
    }

    private UserRegistrationStatus mapStatus(String status) {
        return switch (status) {
            case "PENDIENTE" -> UserRegistrationStatus.PENDING;
            case "APROVISIONANDO" -> UserRegistrationStatus.PROVISIONING;
            case "PROVISIONADO" -> UserRegistrationStatus.PROVISIONED;
            case "INVITADO" -> UserRegistrationStatus.INVITED;
            case "VINCULADO" -> UserRegistrationStatus.LINKED;
            default -> throw new IllegalArgumentException(
                    "Unsupported registration status: " + status
            );
        };
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null
                ? null
                : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private record InvitationUserRow(
            UUID membershipId,
            UUID userId,
            String email,
            String firstNames,
            String lastNames,
            String registrationStatus,
            String auth0Subject,
            OffsetDateTime lastAttemptAt,
            OffsetDateTime invitationSentAt
    ) {
    }
}

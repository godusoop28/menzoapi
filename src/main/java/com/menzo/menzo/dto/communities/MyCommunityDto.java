package com.menzo.menzo.dto.communities;

/** GET /api/communities/my — una fila por comunidad a la que pertenece el usuario, con su
 * membresía embebida. Evita que el cliente tenga que pedir cada membresía por separado. */
public record MyCommunityDto(CommunitySummaryDto community, CommunityMembershipDto membership) {
}

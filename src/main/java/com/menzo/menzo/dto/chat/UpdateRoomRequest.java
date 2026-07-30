package com.menzo.menzo.dto.chat;

import jakarta.validation.constraints.Size;

/**
 * PATCH parcial: un campo en null significa "no cambiar"; para los de texto, "" (string vacío)
 * limpia el campo cuando el campo admite estar vacío (description, category, avatarUri,
 * coverUri, backgroundUri). name y topic no se pueden vaciar. Los booleanos/enteros usan sus
 * wrappers (Boolean/Integer) para poder distinguir "no enviado" de "false"/"0".
 *
 * Permisos (validados en ChatService, no solo en el cliente):
 * - name, description, topic, category, avatarUri, coverUri, backgroundUri,
 *   requiresApproval, allowMembersToInvite, maxMembers: OWNER o CO_HOST.
 * - listed (privacidad/visibilidad pública): solo OWNER.
 */
public record UpdateRoomRequest(
        @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 150) String topic,
        @Size(max = 40) String category,
        String avatarUri,
        String coverUri,
        String backgroundUri,
        Boolean requiresApproval,
        Boolean allowMembersToInvite,
        Boolean listed,
        Integer maxMembers) {
}

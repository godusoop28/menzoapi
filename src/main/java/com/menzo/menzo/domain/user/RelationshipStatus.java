package com.menzo.menzo.domain.user;

/** Amistad no es un sistema aparte: dos usuarios son amigos exactamente cuando cada uno sigue al
 * otro, derivado en cada lectura a partir de la tabla `follows` que ya existe — no hay una tabla
 * de amistades ni de solicitudes. */
public enum RelationshipStatus {
    SELF,
    NONE,
    FOLLOWING,
    FOLLOWS_YOU,
    FRIENDS;

    public static RelationshipStatus of(boolean isSelf, boolean followedByMe, boolean followsMe) {
        if (isSelf) return SELF;
        if (followedByMe && followsMe) return FRIENDS;
        if (followedByMe) return FOLLOWING;
        if (followsMe) return FOLLOWS_YOU;
        return NONE;
    }
}

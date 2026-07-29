package com.menzo.menzo.security.agora;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}

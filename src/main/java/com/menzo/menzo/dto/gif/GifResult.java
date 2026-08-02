package com.menzo.menzo.dto.gif;

/** Forma trimada de un resultado de Tenor — solo lo que el picker necesita para mostrar la
 * grilla e insertar el bloque `gif` (ver PostBlock). Nunca se reenvía el payload crudo de Tenor:
 * eso acopla a los clientes al esquema exacto de un proveedor externo y, si algún día se cambia
 * de proveedor de GIFs, obligaría a tocar ambos clientes en vez de solo este backend. */
public record GifResult(String id, String url, String previewUrl, Integer width, Integer height) {
}

package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.menzo.menzo.domain.post.PostBlock;
import com.menzo.menzo.exception.BadRequestException;

/**
 * Cubre {@link PostService#validateBlocks} y {@link PostService#deriveBodyFromBlocks} — la lógica
 * real detrás del editor de bloques del blog, sin necesidad de base de datos (mismo criterio ya
 * usado en ChatServiceInboxOrderTest): son funciones puras package-private + static, hechas así a
 * propósito para poder testearlas sin levantar todo el contexto de Spring.
 */
class PostServiceBlocksTest {

    private static PostBlock paragraph(String text) {
        return new PostBlock("b1", PostBlock.TYPE_PARAGRAPH, text, null, null);
    }

    private static PostBlock heading(String text) {
        return new PostBlock("b2", PostBlock.TYPE_HEADING, text, null, null);
    }

    private static PostBlock image(String url) {
        return new PostBlock("b3", PostBlock.TYPE_IMAGE, null, url, null);
    }

    @Test
    void aceptaUnaCombinacionValidaDeBloques() {
        List<PostBlock> blocks = List.of(
                heading("Un título"),
                paragraph("Un párrafo normal."),
                image("https://res.cloudinary.com/menzo/uploads/foto.jpg"),
                new PostBlock("b4", PostBlock.TYPE_DIVIDER, null, null, null));

        PostService.validateBlocks(blocks);
        // No debe tirar — llegar hasta acá ya es la aserción.
    }

    @Test
    void rechazaParrafoVacio() {
        List<PostBlock> blocks = List.of(paragraph("   "));
        assertThatThrownBy(() -> PostService.validateBlocks(blocks))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rechazaTituloDemasiadoLargo() {
        String tooLong = "x".repeat(151);
        assertThatThrownBy(() -> PostService.validateBlocks(List.of(heading(tooLong))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rechazaImagenSinUrlHttps() {
        assertThatThrownBy(() -> PostService.validateBlocks(List.of(image("/local/tmp/foo.jpg"))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rechazaImagenConUrlHttpNoSegura() {
        assertThatThrownBy(() -> PostService.validateBlocks(List.of(image("http://example.com/foo.jpg"))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rechazaMasDe40Bloques() {
        List<PostBlock> blocks = java.util.stream.IntStream.range(0, 41)
                .mapToObj(i -> paragraph("bloque " + i))
                .toList();
        assertThatThrownBy(() -> PostService.validateBlocks(blocks))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rechazaTipoDeBloqueDesconocido() {
        List<PostBlock> blocks = List.of(new PostBlock("b1", "embed", null, null, null));
        assertThatThrownBy(() -> PostService.validateBlocks(blocks))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void dividerNoNecesitaContenido() {
        PostService.validateBlocks(List.of(new PostBlock("b1", PostBlock.TYPE_DIVIDER, null, null, null)));
    }

    @Test
    void derivaBodyConcatenandoSoloParrafosYTitulos() {
        List<PostBlock> blocks = List.of(
                heading("Título"),
                image("https://cdn.example.com/foto.jpg"),
                paragraph("Primer párrafo"),
                new PostBlock("b4", PostBlock.TYPE_DIVIDER, null, null, null),
                paragraph("Segundo párrafo"));

        String body = PostService.deriveBodyFromBlocks(blocks);

        assertThat(body).isEqualTo("Título\nPrimer párrafo\nSegundo párrafo");
    }

    @Test
    void derivaBodyVacioSiNoHayTextoDeContenido() {
        List<PostBlock> blocks = List.of(
                image("https://cdn.example.com/foto.jpg"),
                new PostBlock("b2", PostBlock.TYPE_DIVIDER, null, null, null));

        assertThat(PostService.deriveBodyFromBlocks(blocks)).isEmpty();
    }
}

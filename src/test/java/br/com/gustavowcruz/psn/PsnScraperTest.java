package br.com.gustavowcruz.psn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PsnScraperTest {

    @Test
    void formataPrecoNoPadraoBrasileiro() {
        assertEquals(104.96, PsnScraper.formatarValor("R$104,96"));
        assertEquals(0.0, PsnScraper.formatarValor("R$0,00"));
        assertEquals(299.90, PsnScraper.formatarValor("R$299,90"));
    }

    @Test
    void valorComMilharNaoENumerico() {
        // O Python tambem falha em "R$1.234,56" (vira "1.234.56").
        assertThrows(NumberFormatException.class, () -> PsnScraper.formatarValor("R$1.234,56"));
        assertThrows(NumberFormatException.class, () -> PsnScraper.formatarValor("Preço não encontrado"));
    }

    @Test
    void extraiDadosDaPaginaComImagemDoNoscript() {
        String html = """
                <html><body>
                  <h1 data-qa="mfe-game-title#name"> Persona 5 Royal </h1>
                  <span data-qa="mfeCtaMain#offer0#finalPrice">R$104,96</span>
                  <span data-qa="mfeCtaMain#offer1#originalPrice">R$299,90</span>
                  <dd data-qa="gameInfo#releaseInformation#genre-value"><span>RPG</span></dd>
                  <span data-qa="gameBackgroundImage#heroImage#image">
                    <noscript><img src="https://image.api.playstation.com/full.jpg"></noscript>
                    <img src="https://image.api.playstation.com/thumb.jpg?w=200">
                  </span>
                </body></html>
                """;

        Jogo jogo = PsnScraper.parseJogo(html, "https://store.playstation.com/pt-br/product/X");

        assertEquals("Persona 5 Royal", jogo.titulo());
        assertEquals("R$104,96", jogo.precoFinal());
        assertEquals("R$299,90", jogo.precoInicial());
        assertEquals("R$104,96", jogo.precoAtual());
        assertEquals("RPG", jogo.generos());
        assertEquals("https://image.api.playstation.com/full.jpg", jogo.imagem());
    }

    @Test
    void removeParametrosDeThumbnailQuandoNaoHaNoscript() {
        String html = """
                <html><body>
                  <h1 class="game-title">Jogo</h1>
                  <span data-qa="mfeCtaMain#offer1#finalPrice">R$50,00</span>
                  <span data-qa="mfeUpsell#productEdition0#media">
                    <img src="https://image.api.playstation.com/thumb.jpg?w=200">
                  </span>
                </body></html>
                """;

        Jogo jogo = PsnScraper.parseJogo(html, "https://example.com/jogo");

        assertEquals("R$50,00", jogo.precoFinal());
        assertEquals("R$50,00", jogo.precoInicial());
        assertEquals("https://image.api.playstation.com/thumb.jpg", jogo.imagem());
    }

    @Test
    void usaPadroesQuandoElementosNaoExistem() {
        Jogo jogo = PsnScraper.parseJogo("<html><body></body></html>", "https://example.com/jogo");

        assertEquals("Titulo não encontrado", jogo.titulo());
        assertEquals("Preço não encontrado", jogo.precoFinal());
        assertEquals("Preço não encontrado", jogo.precoInicial());
        assertEquals("Preço não encontrado", jogo.precoAtual());
        assertEquals("Genero não encontrado", jogo.generos());
        assertNull(jogo.imagem());
    }

    @Test
    void ignoraOfertaComValorNaoNumerico() {
        String html = """
                <html><body>
                  <span data-qa="mfeCtaMain#offer0#finalPrice">Grátis</span>
                  <span data-qa="mfeCtaMain#offer1#finalPrice">R$19,90</span>
                </body></html>
                """;

        Jogo jogo = PsnScraper.parseJogo(html, "https://example.com/jogo");

        assertEquals("R$19,90", jogo.precoFinal());
    }
}

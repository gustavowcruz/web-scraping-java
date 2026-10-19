package br.com.gustavowcruz.psn;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Raspa titulo, precos, generos e imagem dos jogos cadastrados em
 * {@code database/urls-psn.json}.
 *
 * <p>Port de {@code scrapePsn()} e {@code formatarValor()} do script Python.
 */
public final class PsnScraper {

    private static final String USER_AGENT = "Mozilla/5.0";

    private static final String SELETOR_TITULO =
            "h1[data-qa=\"mfe-game-title#name\"], .game-title";

    /** Consultados em ordem: primeiro a oferta 0, depois a oferta 1. */
    private static final List<String> SELETORES_PRECO_FINAL = List.of(
            "span[data-qa=\"mfeCtaMain#offer0#finalPrice\"]",
            "span[data-qa=\"mfeCtaMain#offer1#finalPrice\"]");

    private static final String SELETOR_PRECO_ORIGINAL_OFERTA_1 =
            "span[data-qa=\"mfeCtaMain#offer1#originalPrice\"]";
    private static final String SELETOR_PRECO_ORIGINAL_OFERTA_0 =
            "span[data-qa=\"mfeCtaMain#offer0#originalPrice\"]";

    private static final String SELETOR_GENEROS =
            "dd[data-qa=\"gameInfo#releaseInformation#genre-value\"] span";

    private static final String SELETOR_IMAGEM = String.join(", ",
            "span.psw-media-frame.psw-fill.psw-aspect-16-9",
            "span[data-qa=\"mfeUpsell#productEdition0#media\"]",
            "span[data-qa=\"gameBackgroundImage#heroImage#image\"]");

    private static final String TITULO_NAO_ENCONTRADO = "Titulo não encontrado";
    private static final String PRECO_NAO_ENCONTRADO = "Preço não encontrado";
    private static final String GENERO_NAO_ENCONTRADO = "Genero não encontrado";

    private final HttpClient httpClient;

    public PsnScraper() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Equivalente a {@code formatarValor}: remove "R$", troca virgula por ponto
     * e converte para double. Lanca {@link NumberFormatException} quando o texto
     * nao representa um valor (mesmo comportamento do {@code float()} do Python).
     */
    public static double formatarValor(String valorOriginal) {
        return Double.parseDouble(valorOriginal.replace("R$", "").replace(",", "."));
    }

    /**
     * Le as URLs e devolve os jogos encontrados.
     *
     * <p>Diferente do Python, uma URL que falha nao interrompe as demais: o erro
     * e registrado e o proximo jogo e processado.
     */
    public List<Jogo> scrapePsn() throws IOException, InterruptedException {
        if (!Files.isRegularFile(AppPaths.URLS_FILE)) {
            System.out.println("Arquivo 'urls-psn.json' não encontrado.");
            return List.of();
        }

        List<String> urls = JsonFiles.readUrls(AppPaths.URLS_FILE);
        List<Jogo> psn = new ArrayList<>();

        for (String url : urls) {
            try {
                String html = baixarPagina(url);
                psn.add(parseJogo(html, url));
            } catch (IOException e) {
                System.out.printf("Erro ao acessar '%s': %s%n", url, e.getMessage());
            }
        }

        return psn;
    }

    String baixarPagina(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response.body();
    }

    /** Extrai os dados de uma pagina ja baixada (separado para permitir testes). */
    static Jogo parseJogo(String html, String url) {
        Document soup = Jsoup.parse(html, url);

        String titulo = textoOuPadrao(soup.selectFirst(SELETOR_TITULO), TITULO_NAO_ENCONTRADO);

        String precoFinal = null;
        for (String seletor : SELETORES_PRECO_FINAL) {
            Element elemento = soup.selectFirst(seletor);
            if (elemento == null) {
                continue;
            }
            String preco = elemento.text().strip();
            try {
                formatarValor(preco);
                precoFinal = preco;
                break;
            } catch (NumberFormatException ignored) {
                // Valor nao numerico: tenta a proxima oferta.
            }
        }
        if (precoFinal == null) {
            precoFinal = PRECO_NAO_ENCONTRADO;
        }

        Element elementoPrecoInicial = soup.selectFirst(SELETOR_PRECO_ORIGINAL_OFERTA_1);
        if (elementoPrecoInicial == null) {
            elementoPrecoInicial = soup.selectFirst(SELETOR_PRECO_ORIGINAL_OFERTA_0);
        }
        String precoInicial = elementoPrecoInicial != null
                ? elementoPrecoInicial.text().strip()
                : precoFinal;

        String precoAtual = precoFinal.equals(precoInicial) ? precoInicial : precoFinal;

        String generos = textoOuPadrao(soup.selectFirst(SELETOR_GENEROS), GENERO_NAO_ENCONTRADO);

        String imagem = extrairImagem(soup);

        return new Jogo(titulo, url, precoFinal, precoInicial, precoAtual, generos, imagem);
    }

    /**
     * Busca a imagem em alta resolucao: primeiro dentro do {@code <noscript>}
     * (imagem completa), depois na {@code <img>} normal sem os parametros de
     * thumbnail.
     */
    private static String extrairImagem(Document soup) {
        Element container = soup.selectFirst(SELETOR_IMAGEM);
        if (container == null) {
            return null;
        }

        Element noscript = container.selectFirst("noscript");
        if (noscript != null) {
            // O jsoup trata o conteudo de <noscript> como texto; por isso ele e
            // reprocessado como HTML para localizar a <img> interna.
            Element img = Jsoup.parse(noscript.html()).selectFirst("img");
            if (img != null) {
                String src = vazioParaNulo(img.attr("src"));
                if (src != null) {
                    return src;
                }
            }
        }

        Element img = container.selectFirst("img");
        if (img != null) {
            String src = vazioParaNulo(img.attr("src"));
            if (src != null) {
                return src.split("\\?", 2)[0];
            }
        }

        return null;
    }

    private static String textoOuPadrao(Element elemento, String padrao) {
        return elemento != null ? elemento.text().strip() : padrao;
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}

package br.com.gustavowcruz.psn;

import java.util.List;

/**
 * Ponto de entrada.
 *
 * <p>Uso:
 * <ul>
 *   <li>{@code java -jar psn-scraper.jar} - monitora os precos indefinidamente;</li>
 *   <li>{@code java -jar psn-scraper.jar --once} - raspa uma vez e imprime os
 *       resultados, sem enviar notificacoes nem alterar o estado.</li>
 * </ul>
 */
public final class PsnApplication {

    private PsnApplication() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--once".equals(args[0])) {
            List<Jogo> jogos = new PsnScraper().scrapePsn();
            for (Jogo jogo : jogos) {
                System.out.printf("Título: %s%n", jogo.titulo());
                System.out.printf("Preço final: %s%n", jogo.precoFinal());
                System.out.printf("Preço inicial: %s%n", jogo.precoInicial());
                System.out.printf("Preço atual: %s%n", jogo.precoAtual());
                System.out.printf("Generos: %s%n", jogo.generos());
                System.out.printf("Imagem: %s%n", jogo.imagem());
                System.out.printf("URL: %s%n%n", jogo.url());
            }
            System.out.printf("Total de jogos: %d%n", jogos.size());
            return;
        }

        new PriceMonitor().monitorarPrecos();
    }
}

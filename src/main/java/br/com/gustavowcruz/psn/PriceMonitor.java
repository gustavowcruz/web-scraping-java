package br.com.gustavowcruz.psn;

import java.io.IOException;
import java.util.List;

/**
 * Loop de monitoramento: raspa os jogos, notifica promocoes novas e persiste o
 * estado em {@code database/enviados.json}.
 *
 * <p>Port de {@code monitorarPrecos()}.
 */
public final class PriceMonitor {

    private static final long INTERVALO_ENTRE_JOGOS_MS = 5_000;
    private static final long INTERVALO_ENTRE_CICLOS_MS = 3_600_000;
    private static final long ESPERA_APOS_ERRO_MS = 60_000;

    private final PsnScraper scraper;
    private final DiscordNotifier notifier;
    private final Object saveLock = new Object();

    public PriceMonitor() {
        this(new PsnScraper(), new DiscordNotifier());
    }

    PriceMonitor(PsnScraper scraper, DiscordNotifier notifier) {
        this.scraper = scraper;
        this.notifier = notifier;
    }

    public void monitorarPrecos() {
        // Lista mutavel: e a mesma referencia usada pelo shutdown hook (Ctrl+C).
        List<Jogo> enviados = JsonFiles.readJogosOrEmpty(AppPaths.ENVIADOS_FILE);
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> salvarEnviados(enviados), "salvar-enviados"));

        while (true) {
            try {
                List<Jogo> psn = scraper.scrapePsn();

                for (Jogo jogo : psn) {
                    System.out.printf("Título: %s%n", jogo.titulo());
                    System.out.printf("Preço atual: %s%n", jogo.precoAtual());
                    System.out.printf("URL: %s%n", jogo.url());
                    System.out.printf("Generos: %s%n", jogo.generos());
                    System.out.printf("Imagem: %s %n%n", jogo.imagem());

                    Thread.sleep(INTERVALO_ENTRE_JOGOS_MS);

                    if (!enviados.contains(jogo) && !jogo.precoAtual().equals(jogo.precoInicial())) {
                        notifier.enviarNotificacao(jogo);
                        enviados.add(jogo);
                    }

                    if (jogo.precoInicial().equals(jogo.precoAtual())) {
                        enviados.remove(jogo);
                    }
                }

                salvarEnviados(enviados);

                System.out.println("Monitoramento concluído. Aguardando 1 hora");
                Thread.sleep(INTERVALO_ENTRE_CICLOS_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Monitoramento interrompido pelo usuário.");
                salvarEnviados(enviados);
                return;
            } catch (IOException | RuntimeException e) {
                System.out.printf("Erro durante o monitoramento: %s%n", e.getMessage());
                try {
                    Thread.sleep(ESPERA_APOS_ERRO_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    System.out.println("Monitoramento interrompido pelo usuário.");
                    salvarEnviados(enviados);
                    return;
                }
            }
        }
    }

    private void salvarEnviados(List<Jogo> enviados) {
        synchronized (saveLock) {
            try {
                JsonFiles.writeJogos(AppPaths.ENVIADOS_FILE, enviados);
            } catch (IOException e) {
                System.out.printf("Erro ao salvar 'enviados.json': %s%n", e.getMessage());
            }
        }
    }
}

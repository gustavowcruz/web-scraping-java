package br.com.gustavowcruz.psn;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Envia o embed de promocao para o webhook do Discord configurado em
 * {@code webhook.txt}. Port de {@code enviarNotificacaoDiscord()}.
 */
public final class DiscordNotifier {

    private static final int COR_VERDE = 0x28a745;
    private static final String IMAGEM_PADRAO = "https://i.imgur.com/2Q3uSAy.png";

    private final HttpClient httpClient;

    public DiscordNotifier() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    /**
     * Envia a notificacao do jogo. Erros de rede/arquivo sao registrados e nao
     * interrompem o monitoramento.
     */
    public void enviarNotificacao(Jogo jogo) {
        Optional<String> webhookUrl = lerWebhook();
        if (webhookUrl.isEmpty()) {
            return;
        }

        Map<String, Object> payload = montarPayload(jogo);

        try {
            String body = JsonFiles.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl.get()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // Mantem o intervalo do script original entre notificacoes.
            Thread.sleep(3_000);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.out.printf("Erro ao enviar notificação para o Discord: HTTP %d%n",
                        response.statusCode());
                return;
            }

            System.out.printf("Notificação enviada para '%s' com sucesso!%n", jogo.titulo());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.out.printf("Erro ao enviar notificação para o Discord: %s%n", e.getMessage());
        }
    }

    private static Map<String, Object> montarPayload(Jogo jogo) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("image", Map.of(
                "url", jogo.imagem() != null ? jogo.imagem() : IMAGEM_PADRAO));
        embed.put("title", "🎮 " + jogo.titulo());
        embed.put("url", jogo.url());
        embed.put("color", COR_VERDE);
        embed.put("fields", List.of(
                Map.of(
                        "name", "De:",
                        "value", "~~" + jogo.precoInicial() + "~~",
                        "inline", true),
                Map.of(
                        "name", "Por",
                        "value", "**" + jogo.precoFinal() + "**",
                        "inline", true)));
        embed.put("footer", Map.of(
                "text", "Scrape Promoções PSN",
                "icon_url", IMAGEM_PADRAO));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", "O jogo **" + jogo.titulo() + "** está em promoção!");
        payload.put("embeds", List.of(embed));
        return payload;
    }

    private static Optional<String> lerWebhook() {
        if (!Files.isRegularFile(AppPaths.WEBHOOK_FILE)) {
            System.out.println("Arquivo 'webhook.txt' não encontrado.");
            return Optional.empty();
        }
        try {
            String url = Files.readString(AppPaths.WEBHOOK_FILE, StandardCharsets.UTF_8).strip();
            if (url.isEmpty()) {
                System.out.println("Arquivo 'webhook.txt' está vazio.");
                return Optional.empty();
            }
            return Optional.of(url);
        } catch (IOException e) {
            System.out.printf("Erro ao ler 'webhook.txt': %s%n", e.getMessage());
            return Optional.empty();
        }
    }
}

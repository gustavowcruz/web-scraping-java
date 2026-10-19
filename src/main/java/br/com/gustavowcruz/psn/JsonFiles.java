package br.com.gustavowcruz.psn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitura/escrita dos arquivos JSON.
 *
 * <p>Equivale ao {@code json.load}/{@code json.dump} do script Python, incluindo
 * {@code indent=2} e {@code ensure_ascii=False} (UTF-8 sem escapar acentos).
 */
public final class JsonFiles {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final TypeReference<List<Jogo>> JOGOS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> URLS_TYPE = new TypeReference<>() {
    };

    private JsonFiles() {
    }

    public static List<String> readUrls(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<String> urls = MAPPER.readValue(reader, URLS_TYPE);
            return urls == null ? List.of() : urls;
        }
    }

    /**
     * Carrega os jogos ja notificados. Arquivo ausente ou JSON invalido resulta
     * em lista vazia, como no {@code except (FileNotFoundError, JSONDecodeError)}
     * do script original.
     */
    public static List<Jogo> readJogosOrEmpty(Path file) {
        if (!Files.isRegularFile(file)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<Jogo> jogos = MAPPER.readValue(reader, JOGOS_TYPE);
            return jogos == null ? new ArrayList<>() : new ArrayList<>(jogos);
        } catch (IOException e) {
            System.out.printf("Nao foi possivel ler '%s': %s%n", file, e.getMessage());
            return new ArrayList<>();
        }
    }

    public static String toJson(Object value) throws IOException {
        return MAPPER.writeValueAsString(value);
    }

    public static void writeJogos(Path file, List<Jogo> jogos) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            MAPPER.writeValue(writer, jogos);
        }
    }
}

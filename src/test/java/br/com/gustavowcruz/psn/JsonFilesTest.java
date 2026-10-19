package br.com.gustavowcruz.psn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void gravaENoLerMantendoAsChavesDoPython() throws Exception {
        Path file = tempDir.resolve("enviados.json");
        Jogo jogo = new Jogo(
                "Street Fighter 6",
                "https://store.playstation.com/pt-br/product/X",
                "R$109,75",
                "R$219,50",
                "R$109,75",
                "Luta",
                "https://image.api.playstation.com/img.jpg");

        JsonFiles.writeJogos(file, List.of(jogo));

        String json = java.nio.file.Files.readString(file);
        assertTrue(json.contains("\"preco_final\" : \"R$109,75\""));
        assertTrue(json.contains("\"preco_inicial\" : \"R$219,50\""));
        assertTrue(json.contains("\"preco_atual\" : \"R$109,75\""));
        assertTrue(json.contains("\"titulo\" : \"Street Fighter 6\""));

        assertEquals(List.of(jogo), JsonFiles.readJogosOrEmpty(file));
    }

    @Test
    void arquivoAusenteResultaEmListaVazia() {
        assertTrue(JsonFiles.readJogosOrEmpty(tempDir.resolve("nao-existe.json")).isEmpty());
    }
}

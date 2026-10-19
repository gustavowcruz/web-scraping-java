package br.com.gustavowcruz.psn;

import java.nio.file.Path;

/** Caminhos de arquivos usados pela aplicacao (relativos ao diretorio de execucao). */
public final class AppPaths {

    public static final Path DATABASE_DIR = Path.of("database");
    public static final Path URLS_FILE = DATABASE_DIR.resolve("urls-psn.json");
    public static final Path ENVIADOS_FILE = DATABASE_DIR.resolve("enviados.json");
    public static final Path WEBHOOK_FILE = Path.of("webhook.txt");

    private AppPaths() {
    }
}

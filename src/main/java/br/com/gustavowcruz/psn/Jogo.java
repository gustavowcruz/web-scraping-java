package br.com.gustavowcruz.psn;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jogo raspado da PlayStation Store.
 *
 * <p>Os nomes das propriedades JSON mantem o formato usado pelo script Python
 * ({@code database/enviados.json}), para que o estado existente continue valido.
 */
public record Jogo(
        @JsonProperty("titulo") String titulo,
        @JsonProperty("url") String url,
        @JsonProperty("preco_final") String precoFinal,
        @JsonProperty("preco_inicial") String precoInicial,
        @JsonProperty("preco_atual") String precoAtual,
        @JsonProperty("generos") String generos,
        @JsonProperty("imagem") String imagem) {
}

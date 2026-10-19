# PSN Price Scraper (Java)

Port para Java do script `scrapper_psn.py` (monitor de promoções da PlayStation Store
que notifica via webhook do Discord).

## Requisitos

- JDK 21+
- Maven 3.9+

## Como executar

```powershell
# compilar, rodar os testes e gerar o jar
mvn package

# monitorar indefinidamente (1 hora entre ciclos)
java -jar target/psn-scraper.jar

# apenas uma raspagem, sem notificar nem alterar o estado
java -jar target/psn-scraper.jar --once

# alternativa sem gerar o jar
mvn exec:java
```

> Dica: se os acentos aparecerem trocados no terminal do Windows, rode
> `chcp 65001` antes (ou use o Windows Terminal).

## Arquivos de dados

Executando a partir da raiz do projeto:

| Arquivo | Descrição |
| --- | --- |
| `database/urls-psn.json` | Lista de URLs dos jogos a monitorar |
| `database/enviados.json` | Promoções já notificadas (estado persistido) |
| `webhook.txt` | URL do webhook do Discord |

`webhook.txt` está no `.gitignore` e foi copiado da pasta `scraping-psn`.
Os arquivos `urls-psn.json` e `enviados.json` também foram copiados de lá.

## Estrutura

```
src/main/java/br/com/gustavowcruz/psn/
├── PsnApplication.java   # main
├── PsnScraper.java       # scrapePsn() / formatarValor()
├── DiscordNotifier.java  # enviarNotificacaoDiscord()
├── PriceMonitor.java     # monitorarPrecos()
├── Jogo.java             # modelo (record) com as chaves JSON originais
├── JsonFiles.java        # leitura/escrita dos JSON
└── AppPaths.java         # caminhos dos arquivos
```

## Diferenças em relação ao script Python

- **Erro por URL**: se uma página falhar, as demais continuam sendo processadas
  (no Python a exceção interrompia o ciclo inteiro).
- **Encerramento**: `Ctrl+C` salva `enviados.json` e encerra de fato. No Python o
  `while True` externo reiniciava o monitoramento após o `KeyboardInterrupt`.
- **`webhook.txt` ausente**: exibe uma mensagem e ignora a notificação em vez de
  derrubar o programa.
- **`--once`**: modo de teste que imprime os dados raspados sem enviar nada.
- O JSON é gravado em UTF-8, com indentação de 2 espaços e sem escapar acentos,
  igual ao `json.dump(..., indent=2, ensure_ascii=False)`.

A lógica de seleção dos preços, gêneros e imagem (incluindo a busca da imagem
completa dentro do `<noscript>` e a remoção dos parâmetros de thumbnail) foi
mantida.

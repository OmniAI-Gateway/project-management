# OmniAI SDK e OmniAI Gateway PoC

Este repositorio contem o trabalho desenvolvido para reduzir o acoplamento entre aplicacoes cliente e diferentes fornecedores de LLMs. O resultado principal e o **OmniAI SDK**, uma biblioteca Kotlin Multiplatform organizada segundo Ports & Adapters, e a **OmniAI Gateway**, uma aplicacao Ktor usada como Proof of Concept para validar o SDK num cenario real.

A Gateway expoe endpoints HTTP compativeis com APIs conhecidas, traduz os pedidos para um dominio comum, aplica uma pipeline de interceptors e encaminha a chamada para o fornecedor configurado.

## Organizacao do repositorio

```text
project-management/
|-- README.md                         # Este guia de orientacao e teste
|-- settings.gradle.kts               # Composite build: OmniAi-SDK + OmniAiGateway
|-- build.gradle.kts                  # Agregador Gradle para clean/build/check
|-- OmniAi-SDK/                       # Biblioteca principal reutilizavel
|-- OmniAiGateway/                    # Proof of Concept que instancia o SDK
|-- docs/                             # Relatorio, imagens, estudos e artefactos
|-- node-tests/                       # Teste com SDK oficial da Anthropic em Node.js
|-- run_claude.py                     # Demo com Claude Code + token Logto via PKCE
```

### `OmniAi-SDK/`

Projeto Kotlin Multiplatform que contem a logica reutilizavel:

- `core`: dominio comum, portas, pipeline, contexto de pedido e abstracoes HTTP.
- `contracts`: DTOs compativeis com OpenAI, Anthropic, Gemini e Ktor HTTP.
- `inbound`: adapters que traduzem pedidos externos para `CommonRequest`.
- `outbound`: adapters que traduzem `CommonRequest` para os fornecedores reais.
- `dispatcher`: despacho por fornecedor/modelo e integracao com a pipeline.
- `interceptors`: autenticacao, autorizacao, metricas, rate limiting, circuit breaker, fallback e logging.
- `gateway-client`: DSL usada para montar uma Gateway por configuracao.
- `gateway-ktor-server`: conectores Ktor que expoem as rotas HTTP.
- `mcp-broker`: base para integracao futura com Model Context Protocol.

### `OmniAiGateway/`

Aplicacao JVM/Ktor que funciona como Proof of Concept. Ela:

- le `src/main/resources/application.conf` e variaveis de ambiente;
- instancia o SDK atraves da DSL em `gateway-client`;
- configura outbounds, autenticacao OIDC e telemetria;
- expoe rotas Ktor para contratos OpenAI, Anthropic e Gemini;
- usa `OmniAi-SDK` por composite build

Endpoints expostos por defeito:

- `POST /v1/chat/completions` - contrato compativel com OpenAI.
- `POST /v1/messages` - contrato compativel com Anthropic.
- `POST /v1beta/models/{model}:generateContent` - contrato compativel com Gemini.

### `docs/`

Contem o relatorio em LaTeX (`docs/relatorio/latex/main.pdf`), imagens de arquitetura, estudos das APIs de inferencia, notas de autenticacao, cartaz e materiais de validacao.

## Testar o Proof of Concept

Iniciar os serviços auxiliares
Navega para a pasta do Docker e arranca os containers:
cd OmniAiGateway/docker
docker compose up -d

Configuração (Logto e API Keys)
Variáveis de Ambiente: É necessário exportar as chaves de API dos provedores de modelos que pretendes usar (por exemplo, export GEMINI_API_KEY="a-tua-chave" ou no Windows set GEMINI_API_KEY="a-tua-chave").
Logto (Auth): Deves configurar a ligação ao Logto ou desativar o bloco de autorização no application.conf caso queiras ignorar o login.

Arrancar a aplicação
Volta para a diretoria de project-management e arranca a aplicação com o Gradle:
cd ..
./gradlew run

Correr o script de teste
Após a Gateway estar pronta a receber pedidos, volta à raiz do projeto e executa o script de teste em Python:python run_claude.py. Este script deve ter ser atualizado para ter o novo client_id e client_secret.


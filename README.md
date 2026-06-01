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


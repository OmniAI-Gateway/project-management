### Potential Client Applications for End-to-End (E2E) Scenarios in the OmniAI Gateway

Imaginando que a gateway implementa uma API compatível com OpenAI, diversas aplicações podem ser facilmente integradas, bastando alterar o endpoint da API para apontar para a api que a nossa gateway expoe em vez de um fornecedor específico.

Nas aplicações que seguem o conceito de **Open Code**, a integração com a OmniAI Gateway é feita através da sobreposição do `baseURL` padrão.  
Aplicações que deixam alterar endpoints de API:

    - Open Code
    - LibreChat
    - Dify

### Configuração via Open Code
De acordo com a [documentação oficial da Open Code](https://opencode.ai/docs/providers/), a integração é realizada através do ficheiro de configuração `opencode.json`.

Para apontar para a OmniAI Gateway, a configuração deve ser feita da forma na ikmagem abaixo:
<img src="OpenCodeConfig.png" alt="Descrição" width="500">

Realizacao de um teste:

1.  **Configuração do Cliente:** Preparação do ficheiro de configuração (`opencode.json`) para definir o endpoint da API como `http://localhost:8080/v1`.
    <img src="OpenCodeConfig.png" alt="Descrição" width="500">

2.  **Simulação da Gateway:** Utilização da ferramenta **Netcat (`nc`)** no macOS para abrir a porta `8080` e escutar pedidos HTTP de entrada em tempo real.
    <img src="enviopedido.png" alt="Descrição" width="500">

3.  **Execução e Validação:** Mandar um pe pedido estruturado via `curl`, replicando o comportamento de aplicações "Open Code", para intercetar o pedido JSON.
    <img src="VerPedido.png" alt="Descrição" width="500">


No incio deu erro pq o pedido n estava estruturado seguido os pedidos que saibam openIa, mas depois de estruturar o pedido corretamente, o Netcat conseguiu receber o pedido JSON enviado pela Open Code.

### Configuração via LibreChat
O LibreChat [documentação oficial do LibreChat](https://www.librechat.ai/docs/quick_start/custom_endpoints)

### Configuração via Claude Code

Outra aplicação relevante para testar a interoperabilidade da OmniAI Gateway é o **Claude Code**, uma ferramenta desenvolvida pela Anthropic que permite utilizar modelos Claude diretamente a partir de ambientes de
desenvolvimento.

Por defeito, o Claude Code comunica diretamente com a API oficial da
Anthropic. No entanto, para efeitos de validação da arquitetura da
OmniAI Gateway, é possível alterar o endpoint utilizado pela aplicação
de forma a que os pedidos sejam enviados primeiro para a gateway.

Desta forma, o fluxo de comunicação passa a ser o seguinte:

Cliente (Claude Code) → OmniAI Gateway → API Anthropic

Em vez do fluxo tradicional: Cliente (Claude Code) → API Anthropic

Este cenário permite demonstrar que uma aplicação desenvolvida para comunicar diretamente com um fornecedor específico pode ser integrada
com a OmniAI Gateway apenas através da alteração do endpoint da API.

------------------------------------------------------------------------
# Claude Code Configuration

Claude Code supports configuration through JSON configuration files.
These files allow users to define environment variables and API
parameters.

According to the documentation, the configuration can be defined in:

Global configuration:

    ~/.claude/settings.json

Project‑specific configuration:

    .claude/settings.json
    .claude/settings.local.json

These files allow developers to define environment variables used by claude Code when making API requests.

------------------------------------------------------------------------

# Changing the API Endpoint

Claude Code allows changing the API endpoint through the `ANTHROPIC_BASE_URL` variable.

Example configuration:

``` json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "YOUR_API_KEY",
    "ANTHROPIC_BASE_URL": "https://custom-endpoint.example.com",
    "ANTHROPIC_MODEL": "model-name"
  }
}
```

Key configuration parameters:

-   **ANTHROPIC_AUTH_TOKEN** -- API authentication token
-   **ANTHROPIC_BASE_URL** -- Base URL for the Anthropic‑compatible API
-   **ANTHROPIC_MODEL** -- Model identifier used in requests

By modifying `ANTHROPIC_BASE_URL`, Claude Code can communicate with any
**Anthropic‑compatible API endpoint**, including a custom gateway
implementation.

------------------------------------------------------------------------

# Example Usage with a Custom Endpoint

A custom endpoint can be configured through environment variables.

Example:

``` bash
export ANTHROPIC_BASE_URL="http://localhost:8080"
export ANTHROPIC_AUTH_TOKEN="API_KEY"
```

In this configuration, the request flow becomes:

    Claude Code
         |
         v
    OmniAI Gateway
         |
         v
    Anthropic API

Instead of the default communication path:

    Claude Code
         |
         v
    Anthropic API

This demonstrates that Claude Code can operate transparently through an
intermediary system such as the **OmniAI Gateway**.

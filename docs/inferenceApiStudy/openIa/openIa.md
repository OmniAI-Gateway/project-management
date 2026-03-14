# Referência Rápida da API da OpenAI

## Autenticação

Para autenticar os teus pedidos, incluir a chave de API no cabeçalho HTTP:

```http
Authorization: Bearer $OPENAI_API_KEY
```
## Funcionalidades Principais

Estes são os conceitos fundamentais para integrar os modelos OpenAI:

* **Geração de Texto (Chat Completions):** Método padrão onde envias mensagens e recebes a resposta completa do modelo.
* **Streaming:** Permite receber a resposta de forma incremental (token a token) à medida que é gerada.
* **Execução de Ferramentas (Function Calling):** Conecta os modelos a funções externas para obter dados adicionais ou executar ações.

### Nota sobre Endpoints

A OpenAI utiliza endpoints baseados na tarefa. O formato principal para chat é:

```
https://api.openai.com/v1/chat/completions
```

### Componentes essenciais

* `model`: Identificador do modelo (ex: `gpt-4o`, `gpt-4o-mini`, `o1-preview`)
* `messages`: Lista de objetos com `role` (`system`, `user`, `assistant`) e `content`

### Exemplo de Pedido e Resposta

## Request1: Geração de Texto (Chat Completions)
```bash
POST http://localhost:11434/v1/chat/completions
Content-Type: application/json

{
  "model": "llama3.2:1b",
  "messages": [
    {
      "role": "system",
      "content": "És um assistente local a correr em Docker."
    },
    {
      "role": "user",
      "content": "Olá! Estás a funcionar?"
    }
  ],
  "temperature": 0.7
}
```
## Response
```json
{
  "id": "chatcmpl-31",
  "object": "chat.completion",
  "created": 1773531116,
  "model": "llama3.2:1b",
  "system_fingerprint": "fp_ollama",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Desculpe, mas esteja de mãos e talvez eu não esteja funcionando corretamente no momento. Mas posso tentar ajudá-lo daqui a um momento!"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 46,
    "completion_tokens": 40,
    "total_tokens": 86
  }
}
```
## Request2Tool: Chamada de Função (Function Calling)

```http
POST http://localhost:11434/v1/chat/completions
Content-Type: application/json
{
  "model": "llama3.2:1b",
  "messages": [
    {
      "role": "user",
      "content": "Como está o preço das ações da Apple (AAPL)?"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_stock_price",
        "description": "Obtém o preço atual de uma ação",
        "parameters": {
          "type": "object",
          "properties": {
            "symbol": {
              "type": "string",
              "description": "O ticker da empresa, ex: AAPL"
            }
          },
          "required": [
            "symbol"
          ]
        }
      }
    }
  ],
  "tool_choice": "auto"
}
```
## Resposta 

```json
{
  "id": "chatcmpl-53",
  "object": "chat.completion",
  "created": 1773531604,
  "model": "llama3.2:1b",
  "system_fingerprint": "fp_ollama",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "",
        "tool_calls": [
          {
            "id": "call_9fasug2c",
            "index": 0,
            "type": "function",
            "function": {
              "name": "get_stock_price",
              "arguments": "{\"symbol\":\"AAPL\"}"
            }
          }
        ]
      },
      "finish_reason": "tool_calls"
    }
  ],
  "usage": {
    "prompt_tokens": 171,
    "completion_tokens": 19,
    "total_tokens": 190
  }
}
```
## Request: Streaming de Resposta
```bash
POST http://localhost:11434/v1/chat/completions
Content-Type: application/json

{
  "model": "llama3.2:1b",
  "messages": [
    {
      "role": "user",
      "content": "Explica o que é Kubernetes em poucas frases."
    }
  ],
  "stream": true
}
```

## Resposta (streaming)


data: {"id":"chatcmpl-308","object":"chat.completion.chunk","created":1773532270,"model":"llama3.2:1b","system_fingerprint":"fp_ollama","choices":[{"index":0,"delta":{"role":"assistant","content":"K"},"finish_reason":null}]}

data: {"id":"chatcmpl-308","object":"chat.completion.chunk","created":1773532270,"model":"llama3.2:1b","system_fingerprint":"fp_ollama","choices":[{"index":0,"delta":{"content":"ubernetes"},"finish_reason":null}]}

data: {"id":"chatcmpl-308","object":"chat.completion.chunk","created":1773532271,"model":"llama3.2:1b","system_fingerprint":"fp_ollama","choices":[{"index":0,"delta":{"content":" é um sistema de gerenciamento de distribuição de aplicativos em nuvem (cloud) baseado no conceito de escalar, isolar e autenticar os serviços e instâncias do recurso."},"finish_reason":null}]}

data: {"id":"chatcmpl-308","object":"chat.completion.chunk","created":1773532272,"model":"llama3.2:1b","system_fingerprint":"fp_ollama","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]

## Request: Geração de Código

```bash
POST http://localhost:11434/v1/chat/completions
Content-Type: application/json

{
  "model": "llama3.2:1b",
  "messages": [
    {
      "role": "system",
      "content": "És um programador experiente."
    },
    {
      "role": "user",
      "content": "Escreve um exemplo simples de API REST em Node.js com Express."
    }
  ],
  "temperature": 0.3,
  "max_tokens": 200
}

```
## Resposta

```
{
  "id": "chatcmpl-898",
  "object": "chat.completion",
  "created": 1773532417,
  "model": "llama3.2:1b",
  "system_fingerprint": "fp_ollama",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Aqui está um exemplo simples de API REST em Node.js com Express:\n\n**Instalação**\n\nAntes de começar, instale as dependências necessárias com o comando:\n```bash\nnpm install express body-parser\n```\n**Código**\n\n```javascript\n// app.js\n\nconst express = require('express');\nconst bodyParser = require('body-parser');\n\nconst app = express();\n\napp.use(bodyParser.json());\n\n// Rota para criar um novo usuário\napp.post('/users', (req, res) => {\n  const { nome, email } = req.body;\n  if (!nome || !email) {\n    return res.status(400).send({ mensagem: 'Por favor, forneça o nome e o e-mail do usuário' });\n  }\n\n  // Criação do novo usuário\n  const newUser = { nome, email };\n  console.log(newUser);\n\n  res.send(`Novo usuário criado com sucesso! ${JSON.stringify(newUser)}`);\n});\n\n// R"
      },
      "finish_reason": "length"
    }
  ],
  "usage": {
    "prompt_tokens": 48,
    "completion_tokens": 200,
    "total_tokens": 248
  }
}
```
## Modificar a Configuração de Geração

Parâmetros que ajustam o comportamento do modelo:

| Parâmetro            | Descrição                                                                                     |
|----------------------|-----------------------------------------------------------------------------------------------|
| `temperature`        | Controla a aleatoriedade (0 a 2). Valores baixos produzem respostas mais determinísticas.   |
| `max_tokens`         | Número máximo de tokens que o modelo pode gerar na resposta.                                |
| `top_p`              | *Nucleus sampling*; o modelo considera tokens cuja soma de probabilidade seja `top_p`.      |
| `stop`               | Sequências que interrompem a geração quando encontradas.                                    |
| `frequency_penalty`  | Penaliza tokens que já apareceram com frequência, reduzindo repetições.                     |
| `presence_penalty`   | Penaliza tokens que já apareceram, incentivando o modelo a introduzir novos tópicos.        |
| `n`                  | Número de respostas que o modelo deve gerar para o mesmo pedido.                            |
| `stream`             | Quando `true`, a resposta é enviada em partes (*streaming*) em vez de um único bloco.       |
| `logit_bias`         | Ajusta a probabilidade de tokens específicos, permitindo favorecer ou bloquear palavras.    |
| `seed`               | Define uma seed para tornar a geração mais reproduzível entre execuções.                    |
| `response_format`    | Define o formato da resposta (por exemplo `json_object` para respostas estruturadas).       |

## Chamada de Função (Tool Calling)

Permite ao modelo solicitar a execução de ferramentas externas definidas pela aplicação.

### Funcionamento

1. **Definição das ferramentas**
    - A aplicação envia as funções disponíveis no campo `tools`.
    - Cada ferramenta define `name`, `description` e `parameters` (JSON Schema).

2. **Decisão do modelo**
    - O modelo analisa o pedido do utilizador.
    - Se necessário, decide chamar uma ferramenta.

3. **Pedido de chamada**
    - A resposta do modelo contém `tool_calls` com:
        - `name` da função
        - `arguments` em JSON

4. **Execução**
    - A aplicação executa a função com os argumentos recebidos.

5. **Resposta final**
    - O resultado da ferramenta é enviado novamente ao modelo como mensagem com `role: tool`.
    - O modelo usa essa informação para gerar a resposta final ao utilizador.

### Parâmetros relevantes

| Parâmetro       | Descrição                                                                 |
|-----------------|-------------------------------------------------------------------------|
| `tools`         | Lista de ferramentas disponíveis para o modelo.                         |
| `tool_choice`   | Controla se o modelo pode ou deve chamar uma ferramenta (`auto`, `none` ou específica). |

---

## Saídas Estruturadas vs Chamada de Função

Ambos os mecanismos ajudam a obter respostas estruturadas, mas com objetivos diferentes.

| Recurso               | Caso de Uso Principal                                                     |
|-----------------------|---------------------------------------------------------------------------|
| Saídas Estruturadas   | Garantir que a resposta segue um formato específico (ex: JSON válido).   |
| Chamada de Função     | Permitir ao modelo interagir com APIs, bases de dados ou outros serviços.|

### Diferença principal

- **Saídas estruturadas:**  
  O modelo **gera diretamente** dados num formato definido.

- **Chamada de função:**  
  O modelo **pede à aplicação para executar uma ação externa** antes de responder.

### Exemplos

| Situação | Melhor abordagem |
|--------|----------------|
| Extrair dados estruturados de texto | Saídas estruturadas |
| Consultar preço de ações | Chamada de função |
| Criar JSON para guardar numa base de dados | Saídas estruturadas |
| Reservar voo ou consultar API externa | Chamada de função |


## Documentação Oficial

[OpenAI API Docs](https://platform.openai.com/docs)

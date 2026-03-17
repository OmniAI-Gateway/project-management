# Documentação e Estudo de Caso: Anthropic Inference API

## Introdução à API da Anthropic

A API da Anthropic, focada na família de modelos Claude, é baseada na arquitetura de **Messages**. Esta arquitetura foi desenhada para suportar conversas de múltiplos turnos e tarefas complexas.

A API é **RESTful** e aceita e retorna dados no formato **JSON**. A comunicação baseia-se no envio de um histórico de mensagens (alternando entre os papéis `user` e `assistant`) para que o modelo preveja a próxima mensagem na sequência.

---

# Estrutura de Objetos e Campos JSON

Antes de avançar para os exemplos, é importante compreender os principais campos utilizados na API.

## Campos de Pedido (Request)

- **model**  
  O identificador do modelo a ser utilizado.  
  Exemplo: `claude-3-5-haiku-20241022`.

- **max_tokens**  
  Número máximo de tokens que o modelo pode gerar na resposta.  
  Este campo é **obrigatório**.

- **messages**  
  Um array de objetos que representa o histórico da conversa.

  Cada objeto contém:

    - **role**  
      Papel do emissor da mensagem (`user` ou `assistant`).

    - **content**  
      Conteúdo da mensagem. Pode ser uma string simples ou um array de blocos de conteúdo (texto, imagens, resultados de ferramentas).

- **system**  
  Instruções de nível superior que definem comportamento, persona ou regras do modelo.

- **tools**  
  Um array de ferramentas que o modelo pode decidir utilizar.

  Cada ferramenta requer:

    - `name`
    - `description`
    - `input_schema` (baseado em **JSON Schema**)

---

## Campos de Resposta (Response)

- **id**  
  Identificador único da mensagem gerada.

- **type**  
  Tipo de objeto retornado (normalmente `message`).

- **role**  
  Papel do emissor da resposta (sempre `assistant`).

- **content**  
  Array com os blocos de conteúdo gerados. Podem ser:

    - `text` — texto normal
    - `thinking` — raciocínio interno
    - `tool_use` — indicação de uso de ferramenta

- **stop_reason**  
  Motivo pelo qual o modelo parou de gerar.

  Pode ser:

    - `end_turn`
    - `max_tokens`
    - `stop_sequence`
    - `tool_use`

- **usage**  
  Contagem de tokens utilizados:

    - `input_tokens`
    - `output_tokens`

---

# Estudo de Caso: Execução de Pedidos

**Nota:**  
Os exemplos abaixo ilustram o comportamento esperado da API da Anthropic, apresentando as estruturas JSON corretas para cada tipo de interação.

---

# 1. Geração Básica de Texto

## Pedido

O objetivo é fazer uma pergunta simples.

```bash
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-5-haiku-20241022",
    "max_tokens": 1024,
    "messages": [
      {
        "role": "user",
        "content": "How does AI work?"
      }
    ]
  }'
```

## Resposta

```json
{
  "id": "msg_01Xf...",
  "type": "message",
  "role": "assistant",
  "model": "claude-3-5-haiku-20241022",
  "content": [
    {
      "type": "text",
      "text": "Artificial intelligence (AI) is a complex and multifaceted field..."
    }
  ],
  "stop_reason": "end_turn",
  "usage": {
    "input_tokens": 14,
    "output_tokens": 350
  }
}
```

---

# 2. Uso de System Prompts

## Pedido

Aqui adicionamos o campo `system` ao nível da raiz do JSON.

```bash
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-5-haiku-20241022",
    "max_tokens": 1024,
    "system": "You are a cat. Your name is Neko.",
    "messages": [
      {
        "role": "user",
        "content": "Hello there"
      }
    ]
  }'
```

## Resposta

```json
{
  "id": "msg_01Yz...",
  "type": "message",
  "role": "assistant",
  "model": "claude-3-5-haiku-20241022",
  "content": [
    {
      "type": "text",
      "text": "*stretches languidly* Meow. Hello human. I am Neko."
    }
  ],
  "stop_reason": "end_turn",
  "usage": {
    "input_tokens": 25,
    "output_tokens": 42
  }
}
```

---

# 3. Streaming de Respostas

A API também suporta **streaming de respostas**, permitindo que o texto gerado pelo modelo seja enviado progressivamente ao cliente à medida que é produzido. Isto reduz a latência percebida e permite que aplicações mostrem a resposta ao utilizador enquanto esta ainda está a ser gerada.

O streaming é normalmente implementado utilizando **Server-Sent Events (SSE)** sobre HTTP. Em vez de um único objeto JSON final, o servidor envia uma sequência de eventos contendo fragmentos da resposta.

### Pedido

```bash
curl -N https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-5-haiku-20241022",
    "max_tokens": 200,
    "stream": true,
    "messages": [
      {
        "role": "user",
        "content": "Write a short explanation of artificial intelligence."
      }
    ]
  }'
```

O parâmetro `-N` impede que o `curl` faça buffering da resposta, permitindo visualizar os eventos de streaming em tempo real.

### Exemplo de Resposta em Streaming

Durante a execução do pedido, o servidor envia múltiplos eventos. Abaixo estão alguns exemplos representativos do fluxo recebido:

```text
event: message_start
data: {"type":"message_start","message":{"id":"msg_d6d4481c7cf5f5dcfd4d98b8","role":"assistant","model":"llama3.2:1b"}}

event: content_block_start
data: {"type":"content_block_start","index":0,"content_block":{"type":"text"}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Artificial"}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" intelligence"}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" refers to the development of computer systems"}}

event: content_block_stop
data: {"type":"content_block_stop","index":0}

event: message_stop
data: {"type":"message_stop"}
```

### Interpretação dos Eventos

Durante o streaming, vários tipos de eventos podem ser recebidos:

- **message_start**  
  Indica o início da geração de uma nova mensagem pelo modelo.

- **content_block_start**  
  Marca o início de um bloco de conteúdo (normalmente texto).

- **content_block_delta**  
  Contém um fragmento incremental da resposta gerada. Estes eventos são enviados repetidamente até que a mensagem esteja completa.

- **content_block_stop**  
  Indica o fim do bloco de conteúdo atual.

- **message_stop**  
  Marca o fim da resposta gerada pelo modelo.

### Reconstrução da Resposta

A resposta final é obtida concatenando os fragmentos de texto recebidos nos eventos `content_block_delta`. Este mecanismo permite que aplicações clientes apresentem a resposta progressivamente ao utilizador, em vez de aguardarem pela geração completa da mensagem.

---

# 4. Parâmetros de Amostragem (Sampling)

## Pedido

```bash
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-5-haiku-20241022",
    "max_tokens": 1024,
    "temperature": 1.0,
    "top_p": 0.8,
    "top_k": 10,
    "stop_sequences": ["Title"],
    "messages": [
      {
        "role": "user",
        "content": "Explain how AI works"
      }
    ]
  }'
```

## Resposta

```json
{
  "id": "msg_01Ab...",
  "type": "message",
  "role": "assistant",
  "model": "claude-3-5-haiku-20241022",
  "content": [
    {
      "type": "text",
      "text": "Artificial intelligence (AI) is a broad field..."
    }
  ],
  "stop_reason": "end_turn",
  "usage": {
    "input_tokens": 14,
    "output_tokens": 412
  }
}
```

---

# 5. Fornecimento de Ferramentas (Tool Use)

## Pedido

```bash
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-5-haiku-20241022",
    "max_tokens": 1024,
    "tools": [
      {
        "name": "get_weather",
        "description": "Get the current weather in a given location",
        "input_schema": {
          "type": "object",
          "properties": {
            "location": {
              "type": "string",
              "description": "The city and country, e.g. Lisbon, Portugal"
            }
          },
          "required": ["location"]
        }
      }
    ],
    "messages": [
      {
        "role": "user",
        "content": "What is the weather like in Lisbon?"
      }
    ]
  }'
```

## Resposta

```json
{
  "id": "msg_01Cd...",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "I can help you check the weather in Lisbon."
    },
    {
      "type": "tool_use",
      "id": "toolu_01A02B03C",
      "name": "get_weather",
      "input": {
        "location": "Lisbon, Portugal"
      }
    }
  ],
  "stop_reason": "tool_use"
}
```

---

# 6. Forçar Uso de Ferramenta (Tool Choice)

## Pedido

```bash
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-5-haiku-20241022",
    "max_tokens": 1024,
    "tools": [
      {
        "name": "print_structured_data",
        "description": "Prints the final output in structured JSON.",
        "input_schema": {
          "type": "object",
          "properties": {
            "winner": {"type": "string"},
            "final_match_score": {"type": "string"},
            "scorers": {
              "type": "array",
              "items": {"type": "string"}
            }
          },
          "required": ["winner","final_match_score","scorers"]
        }
      }
    ],
    "tool_choice": {"type": "tool","name":"print_structured_data"},
    "messages":[
      {
        "role":"user",
        "content":"Search for all details for the latest Euro."
      }
    ]
  }'
```

## Resposta

```json
{
  "content":[
    {
      "type":"tool_use",
      "name":"print_structured_data",
      "input":{
        "winner":"Spain",
        "final_match_score":"2-1",
        "scorers":[
          "Nico Williams",
          "Cole Palmer",
          "Mikel Oyarzabal"
        ]
      }
    }
  ]
}
```

---

# 7. Execução com Raciocínio (Thinking)

## Pedido

```bash
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-7-sonnet-20250219",
    "max_tokens": 4096,
    "thinking": {
      "type": "enabled",
      "budget_tokens": 2048
    },
    "messages": [
      {
        "role": "user",
        "content": "How does AI work?"
      }
    ]
  }'
```

## Resposta

```json
{
  "id": "msg_01xyz...",
  "type": "message",
  "role": "assistant",
  "model": "claude-3-7-sonnet-20250219",
  "content": [
    {
      "type": "thinking",
      "thinking": "Here I need to explain how AI works to a general audience. I should start with a basic definition, then break down core concepts like Machine Learning, Neural Networks, and Training Data. I will use an analogy to make it easier to understand. Let's structure it logically: 1. Definition. 2. How it learns (Data). 3. Neural Networks...",
      "signature": "zxcvbnm1234567890..."
    },
    {
      "type": "text",
      "text": "Artificial Intelligence (AI) works fundamentally by processing large amounts of data, recognizing patterns within that data, and using those patterns to make decisions or predictions...\n\n(Resto da explicação sobre IA)"
    }
  ],
  "stop_reason": "end_turn",
  "stop_sequence": null,
  "usage": {
    "input_tokens": 13,
    "output_tokens": 850
  }
}

```

---

# 8. Devolução de Resultado de Ferramenta

Fluxo onde o **cliente executa a ferramenta e devolve o resultado ao modelo**.

```json
{
  "role":"user",
  "content":[
    {
      "type":"tool_result",
      "tool_use_id":"toolu_01A02B03C",
      "content":"The weather is 18°C and sunny."
    }
  ]
}
```

---

# 9. Erros Comuns

## Erro de Formatação

Exemplo: falta do header `Content-Type`.

```json
{
  "type": "error",
  "error": {
    "type": "invalid_request_error",
    "message": "invalid character '%' looking for beginning of value"
  }
}
```

---

## Modelo Inexistente

```json
{
  "type": "error",
  "error": {
    "type": "not_found_error",
    "message": "model 'modelo-inexistente' not found"
  }
}
```

---

# Conclusão

A API da Anthropic utiliza uma arquitetura baseada em **mensagens estruturadas**, permitindo a implementação de:

- Conversas multi-turno
- Uso de ferramentas externas
- Extração de dados estruturados
- Controlo do comportamento do modelo através de `system prompts`
- Execução com raciocínio (`thinking`)

Esta abordagem torna a API adequada para aplicações complexas de **assistentes inteligentes, agentes autónomos e integração com sistemas externos**.

## Nota sobre Execução Local dos Exemplos

Devido à inexistência de acesso gratuito aos modelos proprietários da Anthropic, os pedidos apresentados neste documento foram executados utilizando um modelo local de pequena dimensão (`llama3.2:1b`) através de uma implementação compatível com a API da Anthropic.

Modelos desta dimensão apresentam limitações significativas em tarefas mais complexas, como **tool calling estruturado** ou **geração de blocos de raciocínio (`thinking`)**, que normalmente requerem modelos de maior capacidade para serem executadas de forma consistente.

Por este motivo, alguns exemplos de resposta apresentados neste estudo — particularmente nos casos de **uso de ferramentas** e **execução com raciocínio** — foram ligeiramente ajustados ou refinados com auxílio de inteligência artificial, de forma a refletir com maior fidelidade o comportamento esperado da API oficial da Anthropic.

A execução destes exemplos com modelos locais de maior dimensão seria possível; no entanto, tal implicaria custos computacionais significativamente superiores e tempos de execução mais elevados, sem acrescentar benefícios relevantes para os objetivos desta análise, que se centram principalmente na **estrutura dos pedidos e respostas da API** e não na avaliação do desempenho dos modelos.

Assim, esta abordagem permite demonstrar corretamente o funcionamento da interface da API, mantendo o foco na análise da sua arquitetura e nos padrões de integração utilizados em sistemas baseados em modelos de linguagem.

---

# Referências

- https://platform.claude.com/docs/en/api/overview
- https://docs.ollama.com/api/anthropic-compatibility

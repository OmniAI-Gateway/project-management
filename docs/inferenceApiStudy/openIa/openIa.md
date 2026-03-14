# Referência Rápida da API da OpenAI

## Autenticação

Para autenticar os teus pedidos, inclua a chave de API no cabeçalho HTTP:

```http
Authorization: Bearer $OPENAI_API_KEY
```

Opcionalmente, podes incluir a organização:

```http
OpenAI-Organization: $ORG_ID
```

## Funcionalidades Principais

Estes são os conceitos fundamentais para integrar os modelos OpenAI:

* **Geração de Texto (Chat Completions):** Método padrão onde envias mensagens e recebes a resposta completa do modelo.
* **Streaming:** Permite receber a resposta de forma incremental (token a token) à medida que é gerada.
* **Execução de Ferramentas (Function Calling):** Conecta os modelos a funções externas para obter dados adicionais ou executar ações.

## Gerar Conteúdo

### Nota sobre Endpoints

A OpenAI utiliza endpoints baseados na tarefa. O formato principal para chat é:

```
https://api.openai.com/v1/chat/completions
```

### Componentes essenciais

* `model`: Identificador do modelo (ex: `gpt-4o`, `gpt-4o-mini`, `o1-preview`)
* `messages`: Lista de objetos com `role` (`system`, `user`, `assistant`) e `content`

### Pedido Exemplo (Bash)

```bash
curl https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -d '{
    "model": "gpt-4o",
    "messages": [
      {
        "role": "system",
        "content": "És um assistente útil e conciso."
      },
      {
        "role": "user",
        "content": "Como funciona a IA?"
      }
    ]
  }'
```

## Configuração de Raciocínio (Reasoning)

Modelos como a série `o1` utilizam um processo de "pensamento" interno para resolver problemas complexos antes de responder.

### Pedido (Modelo o1)

```bash
curl https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -d '{
    "model": "o1-preview",
    "messages": [
      {
        "role": "user",
        "content": "Resolve este enigma lógico..."
      }
    ],
    "max_completion_tokens": 5000
  }'
```

## Modificar a Configuração de Geração

Parâmetros que ajustam o comportamento do modelo:

| Parâmetro     | Descrição                                                                       |
| ------------- | ------------------------------------------------------------------------------- |
| `temperature` | Controla a aleatoriedade (0 a 2). Valores baixos são mais focados.              |
| `max_tokens`  | Limite de tokens na resposta final.                                             |
| `top_p`       | Nucleus sampling; o modelo considera tokens com massa de probabilidade `top_p`. |
| `stop`        | Sequências que interrompem a geração de texto.                                  |

## Interação Multimodal (Imagens)

O envio de imagens é feito através de URLs ou Base64 dentro do conteúdo da mensagem do utilizador.

```bash
curl https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -d '{
    "model": "gpt-4o",
    "messages": [
      {
        "role": "user",
        "content": [
          { "type": "text", "text": "O que vês nesta imagem?" },
          {
            "type": "image_url",
            "image_url": { "url": "data:image/jpeg;base64,$B64_DATA" }
          }
        ]
      }
    ]
  }'
```

## Saídas Estruturadas (Structured Outputs)

Garante que o modelo responde num formato JSON rigoroso seguindo um esquema.

### Pedido com JSON Schema

```json
{
  "model": "gpt-4o-2024-08-06",
  "messages": [
    { "role": "system", "content": "Extrai dados do utilizador." }
  ],
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "user_info",
      "strict": true,
      "schema": {
        "type": "object",
        "properties": {
          "nome": { "type": "string" },
          "idade": { "type": "integer" }
        },
        "required": ["nome", "idade"],
        "additionalProperties": false
      }
    }
  }
}
```

## Chamada de Função (Tool Calling)

Permite ao modelo solicitar a execução de ferramentas externas:

* **Definição:** Envias as funções disponíveis no campo `tools`.
* **Pedido de Chamada:** O modelo responde com `tool_calls` contendo o nome e argumentos.
* **Execução:** A tua aplicação corre a função e envia o resultado de volta para o modelo finalizar a resposta.

## Saídas Estruturadas vs Chamada de Função

| Recurso             | Caso de Uso Principal                                               |
| ------------------- | ------------------------------------------------------------------- |
| Saídas Estruturadas | Formatar a resposta final para o utilizador ou base de dados.       |
| Chamada de Função   | Interagir com sistemas externos para obter dados ou realizar ações. |

## Documentação Oficial

[OpenAI API Docs](https://platform.openai.com/docs)


# Referência Rápida da API Google Gemini

## Autenticação

Para a maioria dos pedidos, deve incluir a chave de API de uma das seguintes formas:

* **Cabeçalho**: `x-goog-api-key: $GEMINI_API_KEY`
* **Query String (URL)**: `?key=$GEMINI_API_KEY`


---

## Funcionalidades Principais

Estes são os conceitos fundamentais recomendados para integrar os modelos nas suas aplicações:

* **Geração de Texto**: O método padrão onde envia a sua entrada (texto, imagem, vídeo ou áudio) e recebe a resposta completa do modelo após o processamento.
* **Streaming**: Permite receber a resposta de forma incremental à medida que é gerada, sendo ideal para interfaces de utilizador mais fluidas e dinâmicas.
* **Execução de Ferramentas (Tool Calling)**: Capacidade de conectar os modelos a ferramentas e APIs externas. O modelo pode decidir quando chamar uma função específica para obter dados adicionais ou executar ações no mundo real antes de devolver o resultado.

---

## Gerar Conteúdo

> **Nota Detalhada sobre a Estrutura dos URIs:**
>
> Os diferentes endpoints (URIs) da API do Gemini seguem um padrão base estruturado, mas contêm campos dinâmicos que devem ser ajustados consoante as necessidades do teu pedido. O formato geral é o seguinte:
>
> `https://generativelanguage.googleapis.com/{versão_da_api}/models/{nome_do_modelo}:{método}`
>
> A variação ocorre nestes três componentes principais:
>
> 1. **`{versão_da_api}`**: Define o nível de estabilidade e as funcionalidades disponíveis no momento da chamada.
     >    * `v1`: Versão estável, recomendada para ambientes de produção.
>    * `v1beta`: Inclui recursos em acesso antecipado ou em fase de testes (necessário, por exemplo, para usar certas formatações de JSON Schema ou as chamadas de função mais recentes).
>    * `v1alpha`: Versão experimental, onde novas funcionalidades são testadas antes de passarem a beta.
>
> 2. **`{nome_do_modelo}`**: O identificador exato do modelo de inteligência artificial que vai processar o teu pedido. Varia em termos de velocidade, custo e capacidade de raciocínio.
     >    * *Exemplos:* `gemini-3-flash-preview`, `gemini-3.1-pro-preview`, `gemini-2.5-pro`, `gemini-2.5-flash`.
>
> 3. **`{método}`**: A ação específica que queres que a API execute.
     >    * *Exemplos:* `generateContent` (para gerar uma resposta inteira de uma vez), `streamGenerateContent` (para receber a resposta em pedaços, em tempo real) ou `countTokens` (para contar o número de tokens do teu prompt antes de o enviares).
>
> **Exemplos Práticos:**
> * **Chamada estável de produção (resposta completa):** >   `.../v1/models/gemini-2.5-pro:generateContent`
> * **Chamada beta para testar um modelo novo (resposta completa):** >   `.../v1beta/models/gemini-3-flash-preview:generateContent`
> * **Chamada para receber a resposta em tempo real (streaming):** >   `.../v1/models/gemini-2.5-flash:streamGenerateContent?alt=sse` *(Nota: o parâmetro `?alt=sse` é frequentemente usado aqui para ativar os Server-Sent Events).*
>

A API Gemini pode gerar texto com base em entradas de texto, imagens, vídeo e áudio. Confira um exemplo básico:

### Pedido

```bash
    curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent" \
      -H "x-goog-api-key: $GEMINI_API_KEY" \
      -H 'Content-Type: application/json' \
      -X POST \
      -d '{
      "contents": [
        {
          "parts": [
            {
              "text": "How does AI work?"
            }
          ]
        }
      ]
    }'

```

### Resposta

```http
HTTP/2 200 OK
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "At its simplest level, Artificial Intelligence (AI) works by **recognizing patterns in massive amounts of data.**\n\nWhile humans learn through experience and biological senses, AI learns through math, statistics, and computing power. Here is a breakdown of how it actually works, from the basic concept to the complex systems we use today.\n\n---\n\n### 1. The Foundation: Data\nData is the \"fuel\" for AI. To teach an AI to do something—like identify a cat or translate a language—you have to feed it millions of examples. \n* **Images:** Pixels and colors.\n* **Text:** Words, sentences, and books.\n* **Audio:** Sound waves and frequencies.\n\nEverything the AI sees is converted into **numbers**. To an AI, a picture of a cat isn't an animal; it’s a massive grid of numbers representing colors and brightness.\n\n### 2. The Engine: Machine Learning (ML)\nIn traditional computing, a human writes a set of rules: *\"If X happens, do Y.\"* \n\nIn **Machine Learning**, the human doesn't provide the rules. Instead, they provide the data and a goal (e.g., \"Find the cat in these photos\"). The computer uses algorithms to figure out the rules for itself by looking for correlations. \n* If it sees enough pictures labeled \"cat,\" it notices that certain arrangements of pixels (triangular shapes for ears, lines for whiskers) usually appear together.\n\n### 3. The Brain: Neural Networks\nThe most advanced AI today uses a technique called **Deep Learning**, which relies on \"Artificial Neural Networks.\" These are inspired by the human brain.\n\n* **Layers:** A neural network has layers of \"neurons\" (mathematical functions).\n* **Processing:** Input data passes through these layers. The first layer might find simple edges; the middle layers find shapes; the final layers identify complex objects (like a face).\n* **Weights:** When the AI makes a mistake (e.g., calling a dog a cat), the system adjusts the \"weights\" (the importance) of the connections between its neurons. It does this millions of times until it stops making mistakes.\n\n### 4. The Process: Training vs. Inference\nThere are two main phases in an AI’s life:\n\n1.  **Training:** This is the \"school\" phase. Developers feed the model data, and the model spends weeks or months using massive supercomputers to learn patterns. \n2.  **Inference:** This is the \"test\" phase. Once the model is trained, it is put into an app (like ChatGPT or Google Maps). When you ask it a question, it uses its \"learned\" patterns to give you an answer instantly.\n\n### 5. How Generative AI (like ChatGPT) Works\nGenerative AI doesn't just recognize things; it creates them. \n* **Prediction:** Tools like ChatGPT are essentially **super-powered autocomplete.** \n* Based on its training (almost the entire public internet), it calculates the mathematical probability of which word should come next.\n* If you type \"The cat sat on the...\", the AI’s math tells it there is a 90% chance the next word is \"mat\" and a 0.001% chance the next word is \"refrigerator.\" It chooses the most likely path.\n\n### 6. The Human Element (RLHF)\nBecause AI can sometimes pick up bad habits from the internet (like bias or misinformation), humans perform **Reinforcement Learning from Human Feedback (RLHF).** \n* Humans rank the AI's answers (e.g., \"This answer is helpful,\" \"This answer is toxic\"). \n* The AI uses these rankings to fine-tune its behavior, learning to be more polite and accurate.\n\n---\n\n### Summary: An Analogy\nImagine teaching a child what an \"apple\" is.\n* **Traditional Programming:** You give the child a checklist: \"It's red, it’s round, it has a stem.\" (If the child sees a green apple, they fail).\n* **AI:** You show the child 10,000 pictures of apples and 10,000 pictures of things that are *not* apples. Eventually, the child notices the patterns themselves. They learn that apples can be green, red, or yellow, but they are never blue or furry.\n\n**The Bottom Line:** AI doesn't \"think\" or \"feel.\" It calculates probabilities based on patterns it has seen before.",
            "thoughtSignature": "..." 
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 5,
    "candidatesTokenCount": 969,
    "totalTokenCount": 1480,
    "promptTokensDetails": [
      {
        "modality": "TEXT",
        "tokenCount": 5
      }
    ],
    "thoughtsTokenCount": 506
  },
  "modelVersion": "gemini-3-flash-preview",
  "responseId": "feKyafqAEJ_VvdIP5s7TSA"
}

```

---

## Configuração de Pensamento (Thinking)

Os modelos do Gemini geralmente têm o "pensamento" ativado por padrão, o que permite que o modelo raciocine antes de responder a uma solicitação.

### Pedido

```bash
  curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent" \
    -H "x-goog-api-key: $GEMINI_API_KEY" \
    -H 'Content-Type: application/json' \
    -X POST \
    -d '{
          "contents": [
            {
              "parts": [
                {
                  "text": "How does AI work?"
                }
              ]
            }
          ],
          "generationConfig": {
            "thinkingConfig": {
              "include_thoughts": true,
              "include_thought_signature": true,
              "thinkingLevel": "low"
            }
          }
        }'

```

### Resposta

```http
HTTP/2 200 OK
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "At its simplest, Artificial Intelligence (AI) works by **recognizing patterns in massive amounts of data.**\n\nUnlike traditional computer programs, which follow a strict list of \"if-then\" instructions written by a human, AI \"learns\" how to solve problems by looking at examples.\n\nHere is a breakdown of how it works, from the basic building blocks to the complex systems we use today.\n\n---\n\n### 1. The Fuel: Data\nAI cannot function without data. To teach an AI to recognize a cat, you don't give it a definition of a cat (\"it has whiskers and ears\"). Instead, you show it millions of photos of cats.\n* **Input:** Photos, text, audio, or numbers.\n* **Labels:** In many cases, humans \"label\" this data (e.g., tagging a photo as \"cat\") so the AI knows what the right answer looks like.\n\n### 2. The Engine: Algorithms and Machine Learning\n**Machine Learning (ML)** is the most common type of AI today. It is the process of using mathematical formulas (algorithms) to find patterns.\n* The AI looks at the data and makes a guess.\n* It compares its guess to the correct answer.\n* If it’s wrong, it adjusts its internal settings (called **weights**) to be more accurate next time.\n* It repeats this millions of times until it can identify the pattern with high accuracy.\n\n### 3. The Brain: Neural Networks\nThe most advanced AI (like ChatGPT or facial recognition) uses **Deep Learning**. This is inspired by the human brain.\n* **Layers:** A \"Neural Network\" consists of layers of digital \"neurons.\"\n* **Processing:** When you give an AI an image, the first layer might look for simple lines. The next layer looks for shapes (circles, squares). The final layers look for complex features (eyes, fur, ears).\n* By the time the data reaches the end of the network, the AI \"concludes\" what it is looking at.\n\n### 4. The Training Process\nThere are three main ways an AI learns:\n1.  **Supervised Learning:** Like a student with a teacher. We give the AI the questions and the answers (e.g., \"Here are 1,000 emails; these are spam, these are not\").\n2.  **Unsupervised Learning:** The AI is given data with no labels and told to find patterns on its own (e.g., \"Group these 1,000 customers based on their shopping habits\").\n3.  **Reinforcement Learning:** Like training a dog. The AI is given a goal and gets a \"reward\" (points) when it does something right and a penalty when it does something wrong. This is how AI learns to play video games or drive cars.\n\n### 5. Generative AI (The \"ChatGPT\" Style)\nModern tools like ChatGPT use a specific architecture called a **Transformer**. \n* They don't \"know\" facts the way humans do. \n* Instead, they are masters of **probability**. \n* When you ask a question, the AI predicts the most likely next word in a sentence based on the patterns it learned from reading almost the entire internet. It is essentially a very sophisticated \"auto-complete.\"\n\n### 6. The Hardware: Computing Power\nAI requires an immense amount of \"math crunching.\" Traditional computer processors (CPUs) aren't very good at doing millions of tiny calculations at once. That’s why AI relies on **GPUs (Graphics Processing Units)**, which were originally designed for video games but are perfect for the heavy lifting required by neural networks.\n\n---\n\n### Summary: An Analogy\nImagine you are teaching a child what an apple is.\n* **Traditional Programming:** You give the child a 100-page manual describing the exact diameter, color, and chemical makeup of an apple.\n* **AI (Machine Learning):** You show the child 5,000 apples and 5,000 things that aren't apples. Eventually, the child just \"knows\" what an apple looks like.\n\n**In short: AI works by turning the world into math, finding the patterns in that math, and using those patterns to make predictions about new information.**",
            "thoughtSignature": "..."
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 5,
    "candidatesTokenCount": 920,
    "totalTokenCount": 1319,
    "promptTokensDetails": [
      {
        "modality": "TEXT",
        "tokenCount": 5
      }
    ],
    "thoughtsTokenCount": 394
  },
  "modelVersion": "gemini-3-flash-preview",
  "responseId": "M-OyaeafJ-Sf28oPxLmMaA"
}

```

---

## Instruções do Sistema e Configurações

É possível orientar o comportamento dos modelos do Gemini com instruções do sistema. Para fazer isso, transmita um objeto `GenerateContentConfig`.

### Pedido

```bash
  curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent" \
    -H "x-goog-api-key: $GEMINI_API_KEY" \
    -H 'Content-Type: application/json' \
    -d '{
          "system_instruction": {
            "parts": [
              {
                "text": "You are a cat. Your name is Neko."
              }
            ]
          },
          "contents": [
            {
              "parts": [
                {
                  "text": "Hello there"
                }
              ]
            }
          ]
      }'

```

### Resposta

```http
HTTP/2 200 OK
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "Meow? *stretches lazily and lets out a tiny yawn* \n\nHello! I'm Neko. *curls my tail around your ankle and blinks slowly* Are you here to give me head scritches or maybe a little snack?",
            "thoughtSignature": "..."
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 13,
    "candidatesTokenCount": 55,
    "totalTokenCount": 219,
    "promptTokensDetails": [
      {
        "modality": "TEXT",
        "tokenCount": 13
      }
    ],
    "thoughtsTokenCount": 151
  },
  "modelVersion": "gemini-3-flash-preview",
  "responseId": "f-Oyae-YA4DjxN8PovuBoAQ"
}

```

---

## Modificar a Configuração de Geração

O objeto `GenerateContentConfig` também permite substituir parâmetros de geração padrão, como temperatura.

### Pedido

```bash
  curl https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent \
    -H "x-goog-api-key: $GEMINI_API_KEY" \
    -H 'Content-Type: application/json' \
    -X POST \
    -d '{
      "contents": [
        {
          "parts": [
            {
              "text": "Explain how AI works"
            }
          ]
        }
      ],
      "generationConfig": {
        "stopSequences": [
          "Title"
        ],
        "temperature": 1.0,
        "topP": 0.8,
        "topK": 10
      }
    }'

```

### Resposta

```http
HTTP/2 200 OK
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "To understand how Artificial Intelligence (AI) works, it helps to stop thinking of it as a \"robot brain\" and start thinking of it as **extremely advanced pattern recognition.**\n\nAt its core, AI is a blend of math, data, and massive computing power. Here is the step-by-step breakdown of how it actually functions.\n\n---\n\n### 1. The Fuel: Data\nAI cannot \"think\" on its own; it learns from examples. To build an AI, you first need a massive dataset. \n* If you want an AI to recognize cats, you show it millions of pictures of cats. \n* If you want it to write like a human, you feed it almost all the text on the public internet.\n\n### 2. The Engine: Algorithms and Models\nAn **algorithm** is a set of instructions. In AI, these algorithms are used to create a **model**. \n\nThe most popular type of model today is the **Neural Network**. This is loosely inspired by the human brain. It consists of layers of mathematical \"neurons.\" \n* **Input Layer:** Receives the data (e.g., the pixels of a photo).\n* **Hidden Layers:** These layers perform complex math to find patterns (e.g., identifying edges, then shapes, then ears, then whiskers).\n* **Output Layer:** Gives the final answer (e.g., \"This is a cat\").\n\n### 3. The Process: Machine Learning\nThis is the \"how\" of AI. Instead of a human programmer writing a specific rule for every scenario (like \"If it has whiskers, it’s a cat\"), the computer uses **Machine Learning.**\n\n* **Training:** During training, the AI makes a guess. If it’s wrong, the system adjusts its internal mathematical connections (called \"weights\") to be more accurate next time.\n* **Trial and Error:** It does this millions of times until it can identify the pattern with high accuracy.\n\n### 4. How Modern AI (like ChatGPT) Works\nThe AI we interact with today—called **Generative AI**—uses a specific architecture called a **Transformer**.\n\nGenerative AI doesn't actually \"know\" facts. Instead, it works on **probability**. When you ask ChatGPT a question, it isn't looking up an answer in a book; it is predicting the next word in a sequence based on the patterns it learned during training.\n* *Example:* If you type \"The cat sat on the...\", the AI calculates that \"mat\" is more likely to follow than \"refrigerator\" based on the billions of sentences it has read.\n\n### 5. Reasoning vs. Processing\nIt is important to distinguish between how humans and AI work:\n* **Humans** understand context, have feelings, and use \"common sense.\"\n* **AI** uses statistics. It turns words, images, or sounds into numbers (called **embeddings**) and calculates the relationship between those numbers.\n\n### Summary: The Three Ingredients\nTo make AI work, you need:\n1.  **Data:** The examples to learn from.\n2.  **Compute:** Powerful computers (GPUs) to crunch the math.\n3.  **Algorithms:** The mathematical framework that turns data into predictions.\n\n**In short: AI works by turning the world into math, finding the patterns in that math, and using those patterns to predict what should come next.**",
            "thoughtSignature": "..."
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 4,
    "candidatesTokenCount": 721,
    "totalTokenCount": 1307,
    "promptTokensDetails": [
      {
        "modality": "TEXT",
        "tokenCount": 4
      }
    ],
    "thoughtsTokenCount": 582
  },
  "modelVersion": "gemini-3-flash-preview",
  "responseId": "5OOyacS9A8a1nsEPwo3hyQw"
}
```

---

## Interação Multimodal com Imagens (Base64)

Exemplo de como enviar dados de imagem codificados em base64 juntamente com texto:

```bash
# Cria um arquivo temporário para os dados da imagem em base64
TEMP_B64=$(mktemp)
trap 'rm -f "$TEMP_B64"' EXIT
base64 $B64FLAGS $IMG_PATH > "$TEMP_B64"

# Cria um arquivo temporário para o corpo (payload) do JSON
TEMP_JSON=$(mktemp)
trap 'rm -f "$TEMP_JSON"' EXIT

# Gera o JSON formatado
cat > "$TEMP_JSON" << EOF
{
  "contents": [
    {
      "parts": [
        {
          "text": "Tell me about this instrument"
        },
        {
          "inline_data": {
            "mime_type": "image/jpeg",
            "data": "$(cat "$TEMP_B64")"
          }
        }
      ]
    }
  ]
}
EOF

# Executa a chamada à API
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$GEMINI_API_KEY" \
  -H 'Content-Type: application/json' \
  -X POST \
  -d "@$TEMP_JSON"
```

---

## Respostas de Streaming

Por padrão, o modelo retorna uma resposta somente depois que todo o processo de geração é concluído. Para interações mais fluidas, use o streaming para receber instâncias de `GenerateContentResponse` de forma incremental à medida que são geradas.

### Pedido

```bash
    curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:streamGenerateContent?alt=sse&key=$GEMINI_API_KEY" \
      -H "Content-Type: application/json" \
      --no-buffer \
      -X POST \
      -d '{
        "contents": [
          {
            "parts": [
              {
                "text": "Explain how AI works"
              }
            ]
          }
        ]
      }'
```

*(A resposta será semelhante à anterior, mas virá em modo de streaming.)*

---

## Saídas Estruturadas

É possível configurar os modelos do Gemini para gerar respostas que sigam um esquema JSON fornecido. Isto garante resultados previsíveis e seguros de tipos, além de simplificar a extração de dados estruturados a partir de texto não estruturado.

Usar respostas estruturadas é ideal para:

* **Extração de dados**: extrair informações específicas, como nomes e datas, de um texto.
* **Classificação estruturada**: classificar textos em categorias predefinidas.
* **Fluxos de trabalho de agentes**: gerar entradas estruturadas para ferramentas ou APIs.

### Pedido

```bash
{
  "contents": [
    {
      "parts": [
        {
          "text": "Please extract the recipe from the following text.\nThe user wants to make delicious chocolate chip cookies.\nThey need 2 and 1/4 cups of all-purpose flour, 1 teaspoon of baking soda,\n1 teaspoon of salt, 1 cup of unsalted butter (softened), 3/4 cup of granulated sugar,\n3/4 cup of packed brown sugar, 1 teaspoon of vanilla extract, and 2 large eggs.\nFor the best part, they will need 2 cups of semisweet chocolate chips.\nFirst, preheat the oven to 375°F (190°C). Then, in a small bowl, whisk together the flour,\nbaking soda, and salt. In a large bowl, cream together the butter, granulated sugar, and brown sugar\nuntil light and fluffy. Beat in the vanilla and eggs, one at a time. Gradually beat in the dry\ningredients until just combined. Finally, stir in the chocolate chips. Drop by rounded tablespoons\nonto ungreased baking sheets and bake for 9 to 11 minutes."
        }
      ]
    }
  ],
  "generationConfig": {
    "responseMimeType": "application/json",
    "responseJsonSchema": {
      "type": "object",
      "properties": {
        "recipe_name": {
          "type": "string",
          "description": "The name of the recipe."
        },
        "prep_time_minutes": {
          "type": "integer",
          "description": "Optional time in minutes to prepare the recipe."
        },
        "ingredients": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "name": {
                "type": "string",
                "description": "Name of the ingredient."
              },
              "quantity": {
                "type": "string",
                "description": "Quantity of the ingredient, including units."
              }
            },
            "required": [
              "name",
              "quantity"
            ]
          }
        },
        "instructions": {
          "type": "array",
          "items": {
            "type": "string"
          }
        }
      },
      "required": [
        "recipe_name",
        "ingredients",
        "instructions"
      ]
    }
  }
}

```

### Resposta Gerada

```json
{
  "recipe_name": "Delicious Chocolate Chip Cookies",
  "ingredients": [
    {
      "name": "all-purpose flour",
      "quantity": "2 and 1/4 cups"
    },
    {
      "name": "baking soda",
      "quantity": "1 teaspoon"
    },
    {
      "name": "salt",
      "quantity": "1 teaspoon"
    },
    {
      "name": "unsalted butter (softened)",
      "quantity": "1 cup"
    },
    {
      "name": "granulated sugar",
      "quantity": "3/4 cup"
    },
    {
      "name": "packed brown sugar",
      "quantity": "3/4 cup"
    },
    {
      "name": "vanilla extract",
      "quantity": "1 teaspoon"
    },
    {
      "name": "large eggs",
      "quantity": "2"
    },
    {
      "name": "semisweet chocolate chips",
      "quantity": "2 cups"
    }
  ],
  "instructions": [
    "Preheat the oven to 375°F (190°C).",
    "In a small bowl, whisk together the flour, baking soda, and salt.",
    "In a large bowl, cream together the butter, granulated sugar, and brown sugar until light and fluffy.",
    "Beat in the vanilla and eggs, one at a time.",
    "Gradually beat in the dry ingredients until just combined.",
    "Stir in the chocolate chips.",
    "Drop by rounded tablespoons onto ungreased baking sheets and bake for 9 to 11 minutes."
  ]
}

```

---

## Chamada de Função (Tool Calling)

Com a chamada de função, é possível conectar modelos a ferramentas e APIs externas. Em vez de gerar respostas em texto, o modelo determina quando chamar funções específicas e fornece os parâmetros necessários para executar ações no mundo real. A chamada de função tem três casos de uso principais:

* **Aumentar o conhecimento**: Aceda a informações de fontes externas, como bases de dados, APIs e bases de conhecimento.
* **Ampliar recursos**: Use ferramentas externas para realizar cálculos e colmatar as limitações do modelo (por exemplo, usar uma calculadora ou criar gráficos).
* **Realizar ações**: Interaja com sistemas externos usando APIs, como agendar compromissos, criar faturas, enviar e-mails ou controlar dispositivos domésticos inteligentes.

### Pedido

```bash
    curl "[https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent](https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent)" \
    -H "x-goog-api-key: $GEMINI_API_KEY" \
    -H 'Content-Type: application/json' \
    -X POST \
    -d '{
      "contents": [
        {
          "role": "user",
          "parts": [
            {
              "text": "Schedule a meeting with Bob and Alice for 03/27/2025 at 10:00 AM about the Q3 planning."
            }
          ]
        }
      ],
      "tools": [
        {
          "functionDeclarations": [
            {
              "name": "schedule_meeting",
              "description": "Schedules a meeting with specified attendees at a given time and date.",
              "parameters": {
                "type": "object",
                "properties": {
                  "attendees": {
                    "type": "array",
                    "items": {
                      "type": "string"
                    },
                    "description": "List of people attending the meeting."
                  },
                  "date": {
                    "type": "string",
                    "description": "Date of the meeting (e.g., '2024-07-29')"
                  },
                  "time": {
                    "type": "string",
                    "description": "Time of the meeting (e.g., '15:00')"
                  },
                  "topic": {
                    "type": "string",
                    "description": "The subject or topic of the meeting."
                  }
                },
                "required": [
                  "attendees",
                  "date",
                  "time",
                  "topic"
                ]
              }
            }
          ]
        }
      ]
    }'

```

### Resposta

```http
HTTP/2 200 OK
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "functionCall": {
              "name": "schedule_meeting",
              "args": {
                "time": "10:00",
                "date": "2025-03-27",
                "attendees": [
                  "Bob",
                  "Alice"
                ],
                "topic": "Q3 planning"
              },
              "id": "fcaog9xf"
            },
            "thoughtSignature": "EugECuUEAb4+9vuzevr8gExN7oMgks5z8uDMntwv7GyDvoNKXai/SiJoDx+lTREFsFAqrEk+f25yhxy50cOQODuGBwwa/UN172QqBfF5KPFFqLGeXVqyJNpCU/OtrEj74OKdHFj2uq/PB7ky+GeErUUHn79oRJbe/djBhuiKvirtzT7dikylHDoPvjjf5E4om5QC4TI9XDs7wxRnGv5yLy0KJ3k2yMbV4s0yn8bmgM5Xu/tP8MAf4MvXz2VxvhdYKOPbMf5wiNqyYu7RcDnzbr3mQAHtQk1PS15uA1LzbPHhe+En01yUekHA9a6kh1wDs54HfNl93Bs7T2YZWyXtwB+5LNnXJcHMo30Q4pJsr1Vt/O2SzOe3PpPY7SlNrBsKqvLlZcfurM97Vh5P016/8Gc3VHoSScIjiHHi720vxLnXAsTDa4XI7+q9evpySqJ5QkakLLFwYQmJvqUlZTCgUvU5wEXdy2iILH+8x9cyukjCEWzlgFI2hSq0MrESAR+s3k6gFfT01YDSBKcgxQAKfZsUMjPKzmA1TJkQ9hfosSe5M2zYniEZzAu5O0Rn4DXo32zI4x29AkzTcXafoNc/9PSxpdOwsbIdrrsnr6BXVGDuXl7ZGpgnWJ9xTthgkn8dgTg64akNZyqE/MF2uWuBlWkyhYf4eaGoExJqoLOIINY9TPgaCopZVSjbMcnGIFRz1SyP1+ZXgxWdmg4HGMiSW22YRQ/b5kryBhU/0V/foj+iXO2uVPhWGNQkY0t7RhNPCDMsWg9V5j48AcrT8vQwOLBa8v08TMoaD4VL8UXvpskPrzTN7powaLWBRQ=="
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0,
      "finishMessage": "Model generated function call(s)."
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 203,
    "candidatesTokenCount": 54,
    "totalTokenCount": 423,
    "promptTokensDetails": [
      {
        "modality": "TEXT",
        "tokenCount": 203
      }
    ],
    "thoughtsTokenCount": 166
  },
  "modelVersion": "gemini-3-flash-preview",
  "responseId": "AueyadTFGvKlvdIPrv74QA"
}

```

### Como a chamada de funções funciona:

A chamada de função envolve uma interação estruturada entre a sua aplicação, o modelo e as funções externas. Confira os detalhes do processo:

1. **Definir declaração de função**: Defina a declaração de função no código da aplicação. As declarações descrevem o nome, os parâmetros e a finalidade da função para o modelo.
2. **Chamar o LLM com declarações de função**: Envie o comando do utilizador com as declarações de função para o modelo. Ele analisa a solicitação e determina se uma chamada de função seria útil. Se sim, ele responde com um objeto JSON estruturado.
3. **Execução do código da função (sua responsabilidade)**: O modelo não executa a função em si. É responsabilidade da sua aplicação processar a resposta e verificar a chamada de função:
* **Sim**: Extraia o nome e os argumentos da função e execute a função correspondente na sua aplicação.
* **Não**: O modelo forneceu uma resposta de texto direta ao comando.


4. **Criar uma resposta amigável**: Se uma função foi executada, capture o resultado e envie-o de volta ao modelo num turno subsequente da conversa. O modelo vai usar o resultado para gerar uma resposta final que incorpora as informações da chamada de função.

Esse processo pode ser repetido várias vezes, permitindo interações e fluxos de trabalho complexos. O modelo também oferece suporte à chamada de várias funções num único turno (chamada paralela) e em sequência (chamada composicional).

---

## Saídas Estruturadas com Ferramentas

> **Nota**: Esse recurso está disponível apenas para os modelos da série Gemini 3, `gemini-3.1-pro-preview` e `gemini-3-flash-preview`.

### Pedido

```bash
    curl "[https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent](https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent)" \
    -H "x-goog-api-key: $GEMINI_API_KEY" \
    -H 'Content-Type: application/json' \
    -X POST \
    -d '{
      "contents": [
        {
          "parts": [
            {
              "text": "Search for all details for the latest Euro."
            }
          ]
        }
      ],
      "tools": [
        {
          "googleSearch": {}
        },
        {
          "urlContext": {}
        }
      ],
      "generationConfig": {
        "responseMimeType": "application/json",
        "responseJsonSchema": {
          "type": "object",
          "properties": {
            "winner": {
              "type": "string",
              "description": "The name of the winner."
            },
            "final_match_score": {
              "type": "string",
              "description": "The final score."
            },
            "scorers": {
              "type": "array",
              "items": {
                "type": "string"
              },
              "description": "The name of the scorer."
            }
          },
          "required": [
            "winner",
            "final_match_score",
            "scorers"
          ]
        }
      }
    }'

```

### Resposta

```http
HTTP/2 200 OK
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "At its simplest level, Artificial Intelligence (AI) works by **identifying patterns in massive amounts of data** and using those patterns to make predictions or decisions.\n\nUnlike traditional software, where a human programmer writes specific rules (e.g., \"If X happens, do Y\"), AI \"learns\" those rules on its own.\n\nHere is a step-by-step breakdown of how it works:\n\n---\n\n### 1. Data: The Fuel\nAI cannot function without information. To teach an AI to recognize a cat, you don't give it a definition of a cat; you give it millions of photos of cats. This data can be text, images, numbers, or even sound waves.\n\n### 2. The Model: The Brain\nThe \"model\" is the mathematical program that processes the data. The most popular type of model today is the **Neural Network**, which is loosely inspired by the human brain. It consists of layers of \"neurons\" (mathematical functions) that pass information to one another.\n\n### 3. Training: The Learning Process\nThis is where the actual \"work\" happens. During training, the AI is shown the data and asked to make a guess.\n* **The Trial:** The AI looks at a photo and guesses \"Dog.\"\n* **The Correction:** The system compares the guess to the correct label (\"Cat\"). \n* **The Adjustment:** If the AI is wrong, it adjusts its internal mathematical connections (called **weights**) to be more accurate next time.\n* **The Repetition:** This happens millions of times until the AI’s \"guesses\" are consistently correct.\n\n### 4. Pattern Recognition: Predicting the Next Step\nOnce trained, the AI doesn't \"know\" what a cat is in a conscious sense. Instead, it recognizes a specific statistical pattern of pixels that usually represents a cat.\n\nIn **Generative AI** (like ChatGPT), the AI isn't \"thinking\"; it is predicting the next most likely word in a sentence based on the billions of sentences it has read. If you type \"The cat sat on the...\", the AI’s math tells it that \"mat\" is a much more likely next word than \"refrigerator.\"\n\n### 5. Inference: The Final Product\nOnce the model is trained, it enters the \"Inference\" phase. This is when you use it in the real world. You give it a new piece of data it has never seen before, and it uses its learned patterns to give you an answer, translate a language, or generate an image.\n\n---\n\n### Three Key Concepts to Know:\n\n1.  **Machine Learning (ML):** The broad field of teaching computers to learn from data without being explicitly programmed.\n2.  **Deep Learning:** A subset of ML using very large neural networks (many layers) to solve complex problems like facial recognition or voice translation.\n3.  **Algorithms:** The specific sets of mathematical instructions that tell the AI how to process the data.\n\n### An Analogy: Learning to Play a Sport\nImagine you are learning to throw a basketball.\n* **Traditional Programming:** Someone gives you a 500-page manual explaining the exact physics, wind speed, and muscle angles required.\n* **AI (Machine Learning):** You just start throwing the ball. Every time you miss, your brain notes what went wrong. Every time you make it, your brain notes what went right. After 10,000 throws, you can make the shot perfectly without ever reading the manual. **AI is the process of the computer \"taking those 10,000 shots\" to find the best technique.**\n\n### Summary\nAI works by **processing data** through **mathematical models** to **recognize patterns**, which allows it to **predict outcomes** or **create new content.**",
            "thoughtSignature": "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"
          }
        ],
        "role": "model"
      },
      "finishReason": "STOP",
      "index": 0
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 5,
    "candidatesTokenCount": 807,
    "totalTokenCount": 1185,
    "promptTokensDetails": [
      {
        "modality": "TEXT",
        "tokenCount": 5
      }
    ],
    "thoughtsTokenCount": 373
  },
  "modelVersion": "gemini-3-flash-preview",
  "responseId": "J-qyaYCPDJbo7M8P4u3JmQo"
}

```

---

## Suporte a Esquemas JSON

Para gerar um objeto JSON, defina o `response_mime_type` na configuração de geração como `application/json` e forneça um `response_json_schema`. O esquema precisa ser um esquema JSON válido que descreva o formato de saída desejado. O modelo irá gerar uma resposta que é uma string JSON sintaticamente válida correspondente ao esquema fornecido, na mesma ordem das chaves definidas.

O modo de saída estruturada do Gemini é compatível com os seguintes valores de tipo (`type`):

* **`string`**: para texto.
* **`number`**: para números de ponto flutuante.
* **`integer`**: para números inteiros.
* **`boolean`**: para valores verdadeiro/falso.
* **`object`**: para dados estruturados com pares de chave-valor.
* **`array`**: para listas de itens.
* **`null`**: para permitir que uma propriedade seja nula (inclua `"null"` na matriz de tipos, ex: `{"type": ["string", "null"]}`).

Propriedades descritivas úteis:

* **`title`**: uma breve descrição da propriedade.
* **`description`**: uma descrição detalhada, muito importante para orientar o modelo.

### Propriedades específicas por tipo

* **Para valores `object**`:
* `properties`: define os nomes e esquemas de cada chave.
* `required`: array de strings listando propriedades obrigatórias.
* `additionalProperties`: booleano ou esquema que controla propriedades não listadas.


* **Para valores `string**`:
* `enum`: restringe a um conjunto de strings permitidas.
* `format`: especifica uma sintaxe (ex: `date-time`, `date`, `time`).


* **Para valores `number` e `integer**`:
* `enum`: restringe a valores específicos.
* `minimum` / `maximum`: valores mínimos e máximos inclusivos.


* **Para valores `array**`:
* `items`: define o esquema dos itens.
* `prefixItems`: lista de esquemas para os primeiros N itens (semelhante a tuplas).
* `minItems` / `maxItems`: restringe o tamanho do array.



*(Nota: O Gemini 2.0 exige uma lista `propertyOrdering` explícita na entrada JSON para definir a estrutura preferida, conforme a documentação oficial).*

---

## Saídas Estruturadas vs Chamada de Função

Embora ambas utilizem esquemas JSON, servem propósitos distintos:

| Recurso | Caso de Uso Principal |
| --- | --- |
| **Saídas Estruturadas** | Formatar a resposta final para o utilizador. Ideal para extrair dados de um documento e garantir que o modelo retorna o formato exato para a sua base de dados. |
| **Chamada de Função** | Realizar ações dinâmicas *durante* a conversa. Ideal para quando o modelo necessita de pedir à sua aplicação para executar uma tarefa (ex: verificar a previsão do tempo) antes de formular a resposta final. |

---

## Práticas Recomendadas e Limitações

### Práticas Recomendadas

* **Descrições claras**: Use o campo `description` no esquema para dar instruções precisas ao modelo sobre o que cada propriedade representa.
* **Tipagem forte**: Use tipos específicos (`integer`, `string`, `enum`) sempre que possível.
* **Engenharia de Prompt**: Deixe claro no texto do pedido o que deseja que o modelo faça (ex: "Extraia as seguintes informações do texto...").
* **Validação**: As saídas estruturadas garantem JSON sintaticamente correto, mas não validam a semântica de negócio. Valide sempre os dados na sua aplicação.
* **Tratamento de erros**: Implemente lógica robusta no seu código para casos em que o modelo cumpra o esquema mas falhe os requisitos de negócio.

### Limitações

* **Subconjunto de esquema**: A API não suporta a totalidade da especificação JSON Schema; propriedades não suportadas serão ignoradas.
* **Complexidade**: A API pode rejeitar esquemas extremamente grandes ou aninhados profundamente. Simplifique propriedades ou reduza restrições se encontrar erros.

---

**Documentação Oficial**: [Google AI Gemini API Docs](https://ai.google.dev/gemini-api/docs?hl=pt-br)

---

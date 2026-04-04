# PROVIDER_MAPPINGS

Cheat Sheet de mapeamentos do `OmniAi-SDK` entre contratos de providers e dominio comum.

- **Inbound Translators**: contrato provider/client -> dominio (`CommonRequest`, `CommonResponse`, `CommonResponseEvent`).
- **Outbound Translators**: dominio -> contrato provider/client, incluindo stream event parsing.

## 1) Mapeamento de Roles (Papeis)

| Domain Role | OpenAI | Anthropic | Gemini | Edge Cases / TODOs |
|---|---|---|---|---|
| `SYSTEM` | `system` (`OpenAiInboundTranslator.toOpenAiRole`, `OpenAiOutboundTranslator.toOpenAiRole`) | `assistant` (`AnthropicInboundTranslator.toAnthropicRole`, `AnthropicOutboundTranslator.toAnthropicRole`) | `model` (`GeminiInboundTranslator.toGeminiRole`, `GeminiOutboundTranslator.toGeminiRole`) | Anthropic e lossy para `SYSTEM` (vira `assistant`). |
| `USER` | `user` | `user` | `user` | Sem gaps relevantes. |
| `ASSISTANT` | `assistant` | `assistant` | `model` | Gemini colapsa `assistant` -> `model`. |
| `TOOL` | `tool` | `assistant` | `function` em `GeminiInboundTranslator.toGeminiRole`; `tool` em `GeminiOutboundTranslator.toGeminiRole` | **Inconsistencia critica Gemini** (`function` vs `tool`) entre inbound/outbound. |
| Provider -> Domain (parse) | `system/assistant/tool/user` -> dominio (`OpenAiInboundTranslator.toCommonRole`, `OpenAiOutboundTranslator.toCommonRole`) | `user/assistant/tool/system` -> dominio (`AnthropicInboundTranslator.toCommonRole`, `AnthropicOutboundTranslator.toCommonRole`) | `model/assistant` -> `ASSISTANT`; `tool/function` -> `TOOL` (`GeminiInboundTranslator.toCommonRole`, `GeminiOutboundTranslator.toCommonRole`) | Fallback de unknown role em todos: `USER` (pode mascarar erro de contrato). |

## 2) Gestao de IDs (Maior Ponto de Falha)

| Contexto | OpenAI | Anthropic | Gemini | Estrategia do SDK |
|---|---|---|---|---|
| REST response sem `id` | `OpenAiInboundTranslator.fromDomain` gera `chatcmpl-{uuid}` (`generateOpenAiID`) | Nao gera fallback em `AnthropicInboundTranslator.fromDomain` (usa `domainResponse.id`) | `GeminiOutboundTranslator.toDomain` gera UUID local se `responseId` ausente (`generateGeminiId`) | OpenAI/Gemini fazem fallback; Anthropic depende da origem. |
| Streaming correlacao `id/model` | `OpenAiOutboundTranslator.toDomainEvent` usa `runningFold(OpenAiEventContext)` | `AnthropicOutboundTranslator.toDomainEvent` usa `runningFold(AnthropicEventContext)` | `GeminiOutboundTranslator.toDomainEvent` usa `runningFold(GeminiEventContext)` | Estado injetado no fold para chunks incompletos. |
| **Anthropic streaming sem `id/model` em varios eventos** | [N/A] | `toDomainStreamEvent(receivedId, receivedModel)` reaproveita estado para `ContentBlock*`, `MessageDelta`, `MessageStop`, `Error` | [N/A] | **Obrigatorio state injection**; sem isso, eventos ficam sem correlacao. |
| Tool call id fallback | `openai-tool-call-{index}` (`OpenAiInboundTranslator.toDomainPart`, `OpenAiOutboundTranslator.toDomainToolCallPart`, stream fallback no `toDomainChunkEvent`) | `anthropic-tool-use-{index}` (`AnthropicInboundTranslator.toDomainPart`) | `gemini-tool-call-{index}` (`GeminiInboundTranslator.toDomainPart`), `gemini-tool-call-{random}` no streaming (`GeminiOutboundTranslator.toDomainChunkEvent`) | IDs sinteticos evitam null, mas enfraquecem deduplicacao/replay. |
| Choice/model defaults | `model` vazio preservado via contexto no stream | idem por contexto | `GeminiInboundTranslator.toDomain` usa `"NO MODEL"`; `GeminiOutboundTranslator.toDomain` usa `""` para modelVersion nulo | Sentinel/hardcode deve virar erro de validacao ou `ResponseErrored`. |

## 3) Function Calling / Tools (Estrutura + Streaming)

| Domain | OpenAI | Anthropic | Gemini | Estado Atual & TODOs |
|---|---|---|---|---|
| Tool declaration (`CommonTool`) | `CommonTool.toOpenAiTool` -> `OpenAiTool(function.parameters)` | `CommonTool.toAnthropicToolDefinition` -> `inputSchema` | `List<CommonTool>.toGeminiTools` -> `GeminiTool(functionDeclarations)` + `cleanGeminiParameters` | Gemini remove chaves JSON Schema (`$schema`, `additionalProperties`, etc.). |
| Tool choice (`ToolChoice`) | `toOpenAiToolChoice`: `auto/none/required/specific(first)` | `toAnthropicToolChoice`: `auto/none/any/tool(name)` | `toGeminiToolConfig`: `AUTO/NONE/ANY` + `allowedFunctionNames` | `ToolChoice.Specific` em OpenAI/Anthropic usa apenas `first` (potencial perda para lista). |
| Tool call -> Domain (REST) | `OpenAiOutboundTranslator.toDomainToolArguments`: args chegam `String`; SDK faz parse JSON (inclui nested JSON string) | `AnthropicOutboundTranslator.toDomainContentPart` usa `input` direto | `GeminiOutboundTranslator.GeminiResponsePart.toDomainPart` usa `functionCall.args` | **OpenAI parse robusto**; Anthropic/Gemini dependem de JSON ja estruturado. |
| Tool result (request side) | `CommonRequestMessage.toOpenAiMessageInput`: role `tool` usa `toolCallId` + primeiro content serializado | `List<RequestContentPart>.toAnthropicContent`: `ToolResult` com `content.firstOrNull()?.toRawAny()?.toString()` | `RequestContentPart.toGeminiPart`: `functionResponse(name = toolCallId, response = first content)` | Apenas primeiro elemento de `ToolResultPart.content` e usado (restante ignorado). |
| Streaming tool start | `ToolCallStartedEvent` -> `OpenAiDelta.toolCalls[function.arguments=""]` (`OpenAiInboundTranslator.toOpenAiEvent`) | `ToolCallStartedEvent` -> `ContentBlockStart.ToolUse(input={})` | `ToolCallStartedEvent` -> `GeminiResponsePart.functionCall(args={})` | Placeholders vazios sao usados para abrir estrutura incremental. |
| Streaming args delta | `ToolCallArgumentsDeltaEvent` -> `function.arguments = fragment` | `InputJsonDelta(partialJson=fragment)` | `GeminiInboundTranslator.toGeminiEvent` manda `functionCall(id=null,name="",args={partialJson:fragment})` | **[EM FALTA / TODO CRITICO]** Gemini nao bufferiza deltas por tool/candidate ate `ChoiceFinished`; fragmentos saem invalidos/parciais e podem gerar `functionCall` sem payload JSON final valido. Necessario acumulador por `(responseId, choiceIndex, toolCallIndex)` e flush atomico ao fim da choice. |

## 4) Mapeamento Geral de Request / Response (REST)

| Funcionalidade | OpenAI | Anthropic | Gemini | Estado Atual |
|---|---|---|---|---|
| Request principal | `OpenAiOutboundTranslator.fromDomain` -> `OpenAiChatCompletionsRequest` | `AnthropicOutboundTranslator.fromDomain` -> `AnthropicMessagesRequest` | `GeminiOutboundTranslator.fromDomain` -> `GeminiGenerateContentRequest` | Cobertura base OK. |
| Response principal | `OpenAiOutboundTranslator.toDomain` | `AnthropicOutboundTranslator.toDomain` | `GeminiOutboundTranslator.toDomain` | Cobertura base OK. |
| `jsonResponse` (Domain -> Provider) | `responseFormat = json_object` (`OpenAiOutboundTranslator.fromDomain`) | Sem suporte nativo; comentario explicito de lacuna em `AnthropicOutboundTranslator.fromDomain` | `generationConfig.responseMimeType = application/json`; schema via `providerOptions.responseJsonSchema` | OpenAI nao diferencia `json_object` vs `json_schema`; Anthropic forcado a `false` no inbound (`AnthropicInboundTranslator.toDomain`). |
| `jsonResponse` (Provider -> Domain) | `responseFormat.type in {json_object,json_schema}` -> `jsonResponse=true` (`OpenAiInboundTranslator.toDomain`) | sempre `false` (`AnthropicInboundTranslator.toDomain`) | `responseMimeType==application/json` ou `responseJsonSchema!=null` -> `true` (`GeminiInboundTranslator.toDomain`) | Gap estrutural Anthropic. |
| Config defaults/hard limits | `maxTokens` default `1000`, clamp `<=4000` | `maxTokens` default `1024` | sem clamp, sem max default local no mapper | Hardcodes de negocio estao no translator (deviam ser policy configuravel). |
| Provider options pass-through | `frequencyPenalty`, `presencePenalty`, `n`, `seed`, `user`, `logitBias`, `logProbs`, `topLogProbs`, etc. | `stream`, `topK`, `stopToken`, `thinking`, `metadata` | `topK`, `thinkingConfig`, `responseMimeType`, `responseJsonSchema` | Bom para extensibilidade, mas sem schema/type safety central. |

## 5) Streaming Events (`CommonResponseEvent`)

| Domain Event | OpenAI Equivalent | Anthropic Equivalent | Gemini Equivalent |
|---|---|---|---|
| `ResponseStarted` | chunk vazio sem choices (`OpenAiInboundTranslator.toOpenAiEvent`) / `choices.isEmpty && usage==null` (`OpenAiOutboundTranslator.toDomainChunkEvent`) | `MessageStart`; tambem `Ping` vira `ResponseStarted` no outbound | chunk sem candidates/usage (`response_start`) |
| `ChoiceStarted` | `delta.role` | `ContentBlockStart(Text|Thinking)` | `candidate.content.role` |
| `TextDeltaEvent` | `delta.content` | `ContentBlockDelta.TextDelta` (e tambem `ThinkingDelta`/`SignatureDelta` no outbound) | `candidate.content.parts[].text` |
| `ToolCallStartedEvent` | `delta.tool_calls[].function(name, id)` | `ContentBlockStart.ToolUse` | `parts[].functionCall(name,id,args={})` |
| `ToolCallArgumentsDeltaEvent` | `delta.tool_calls[].function.arguments` | `ContentBlockDelta.InputJsonDelta.partialJson` | `functionCall.args.partialJson` (convencao interna do SDK) |
| `ChoiceFinished` | `finish_reason` | `ContentBlockStop` ou `MessageDelta(stopReason)` | `candidate.finishReason` |
| `UsageReported` | chunk com `usage` | `MessageDelta(usage)` | chunk com `usageMetadata` |
| `ResponseCompleted` | evento `[DONE]` / chunk nao reconhecido | `MessageStop` | evento `Done` |
| `ResponseErrored` | `OpenAiEventStream.Error` -> `ResponseErrored`; **mas no caminho inverso (`OpenAiInboundTranslator.toOpenAiEvent`) e convertido em `delta.content`** | `AnthropicStreamEvent.Error` <-> `ResponseErrored` | `GeminiEventStream.Error` ou `promptFeedback.blockReason` -> `ResponseErrored` |

## 6) TODOs, Hardcodes e Implementacoes Pendentes (CRITICO)

| Severidade | Local (classe.metodo) | Problema | Impacto | Acao recomendada |
|---|---|---|---|---|
| CRITICO | `GeminiInboundTranslator.toGeminiEvent` + `GeminiOutboundTranslator.toDomainChunkEvent` | Streaming de argumentos de tool call usa fragmentos (`partialJson`) sem buffer final; regra atual decide delta apenas quando `functionCall.name` vazio. | Pode emitir JSON invalido, perder argumentos, duplicar `ToolCallStartedEvent`. | Implementar buffer por choice/tool + flush no `ChoiceFinished`/fim de candidate; separar claramente `started` vs `arguments_delta`. |
| CRITICO | `OpenAiInboundTranslator.toOpenAiEvent` (ramo `ResponseErrored`) | Comentario `// this is not ritgh`; erro vira texto de assistente (`delta.content`) em vez de erro estruturado. | Consumidor nao recebe semantica de falha real. | Criar contrato/evento de erro OpenAI no fluxo inverso ou mapear para shape de erro apropriado. |
| ALTO | `GeminiInboundTranslator.toGeminiRole` vs `GeminiOutboundTranslator.toGeminiRole` | `TOOL` mapeia para `function` num lado e `tool` no outro. | Inconsistencia de role em round-trip e stream. | Unificar convencao Gemini (documentar e aplicar em ambos os translators). |
| ALTO | `AnthropicOutboundTranslator.toDomainEvent` (`runningFold`) | Necessita estado externo (`receivedId`, `receivedModel`) para eventos sem identificadores. | Bugs de correlacao se ordem/eventos mudarem ou reconexao ocorrer sem contexto. | Introduzir state object resiliente por stream/session + reset explicito em `MessageStart/Stop`. |
| ALTO | `OpenAiOutboundTranslator.fromDomain`, `AnthropicOutboundTranslator.fromDomain` | Hardcodes de negocio (`maxTokens ?: 1000`, clamp `4000`, `maxTokens ?: 1024`). | Comportamento inesperado por provider/modelo; tuning limitado. | Mover defaults/limits para policy configuravel por modelo/provider. |
| ALTO | `GeminiInboundTranslator.toDomain` | fallback de modelo `"NO MODEL"`; `GeminiOutboundTranslator.toDomain` usa `""` se `modelVersion` ausente. | IDs/modelos invalidos entram no dominio silenciosamente. | Falhar rapido com erro de validacao (`ResponseErrored` ou exception controlada). |
| MEDIO | `OpenAiOutboundTranslator.toDomainChunkEvent`, `GeminiOutboundTranslator.toDomainChunkEvent` | Parse de tool-call stream depende de heuristicas (`name` blank, first call only). | Perda de multichamada/multichoice no mesmo chunk. | Iterar todas as calls/parts por `index`; emitir eventos por item. |
| MEDIO | `AnthropicOutboundTranslator.toDomainStreamEvent` | `Ping` mapeado para `ResponseStarted`. | Pode gerar inicios falsos e inflar telemetria. | Criar evento interno de heartbeat ou ignorar `Ping`. |
| MEDIO | `AnthropicOutboundTranslator.toDomainStreamEvent` | `SignatureDelta` e `ThinkingDelta` viram `TextDeltaEvent`; `Thinking` REST vira `RefusalPart`. | Mistura semanticas (pensamento/assinatura/refusal/texto). | Criar tipos de evento/part dedicados para thinking/signature. |
| MEDIO | `OpenAiOutboundTranslator.CommonRequestMessage.toOpenAiMessageInput` | Tool args serializados via `.toString()` de map; nao garante JSON canonico. | Risco de serializacao ambigua em casos complexos. | Serializar com encoder JSON dedicado (`Json.encodeToString`). |
| MEDIO | `CommonRequestMessage` mappers (`OpenAI/Anthropic/Gemini`) | `ToolResultPart.content` usa apenas `firstOrNull()`. | Perda de dados multimodais/multipart. | Suportar todos os itens de `content` na traducao provider-specific. |
| MEDIO | `ToolChoice.Specific` em `OpenAiOutboundTranslator.toOpenAiToolChoice` e `AnthropicOutboundTranslator.toAnthropicToolChoice` | Usa apenas `toolNames.first()`. | Lista de funcoes permitidas perde cardinalidade. | Validar cardinalidade ou suportar lista conforme API provider. |
| BAIXO | `OpenAiInboundTranslator.toDomainPart` | `function.arguments` entra como `{"raw": "..."}` em vez de parse direto. | Contrato de argumentos heterogeneo entre providers. | Normalizar parse para `JsonObjectMap` quando possivel; fallback explicito com flag de parse error. |
| BAIXO | `AnthropicInboundTranslator.toDomain` | `jsonResponse = false` fixo. | Funcionalidade de structured output nao representada no dominio para Anthropic. | Definir estrategia de emulacao/documentacao (`tool`/prompt contract). |
| BAIXO | `GeminiOutboundTranslator.toDomainChoice` | `hasToolCalls => FinishReason.TOOL_CALL` sobrescreve `candidate.finishReason`. | Possivel perda de motivo real do provider. | Preservar ambos: reason original + derivada. |


# Resumo do Que Foi Feito (Autenticacao Gateway)

## Objetivo
Preparar a arquitetura da `OmniAiGateway` para suportar dois tipos de token:
- `JWT` (validacao local)
- `Opaque` (introspection em AS no futuro)

Sem implementar o Authorization Server agora, mas deixando tudo pronto para plugar depois (ex.: Keycloak).

---

## Principais mudancas implementadas

### 1) Extracao e propagacao do token desde o inbound
- Foi adicionada extracao do header `Authorization: Bearer ...` no inbound.
- O token passa a ser guardado em metadata (`TypedMap`) por request.

Ficheiros relevantes:
- `OmniAiGateway/inbound/src/main/kotlin/org/omniai/gateway/inbound/web/common.kt`
- `OmniAiGateway/inbound/src/main/kotlin/org/omniai/gateway/inbound/web/OpenAiRoute.kt`
- `OmniAiGateway/inbound/src/main/kotlin/org/omniai/gateway/inbound/web/AnthropicRoute.kt`
- `OmniAiGateway/inbound/src/main/kotlin/org/omniai/gateway/inbound/web/GeminiRoute.kt`

### 2) Propagacao da metadata para o request de dominio
- Os adapters inbound passaram a fazer merge da metadata em `providerOptions`.
- Isto permite que a pipeline core veja token/contexto.

Ficheiros relevantes:
- `OmniAi-SDK/inbound/openai/src/commonMain/kotlin/org/omniai/sdk/inbound/openai/OpenAiInboundAdapter.kt`
- `OmniAi-SDK/inbound/anthropic/src/commonMain/kotlin/org/omniai/sdk/inbound/anthropic/AnthropicInboundAdapter.kt`
- `OmniAi-SDK/inbound/gemini/src/commonMain/kotlin/org/omniai/sdk/inbound/gemini/GeminiInboundAdapter.kt`
- `OmniAi-SDK/core/src/commonMain/kotlin/org/omniai/sdk/core/commom/TypedMap.kt`

### 3) Interceptor de autenticacao no core pipeline
- O `AuthContextInterceptor` foi colocado no `core` para centralizar a decisao de autenticacao.
- Faz deteccao de tipo de token (`JWT` vs `OPAQUE`).

Ficheiro relevante:
- `OmniAi-SDK/core/src/commonMain/kotlin/org/omniai/sdk/core/pipeline/AuthContextInterceptor.kt`

### 4) Chaves de metadata de autenticacao
- Foram definidas chaves comuns para token bruto, tipo de token e claims opaque.

Ficheiro relevante:
- `OmniAi-SDK/core/src/commonMain/kotlin/org/omniai/sdk/domain/common/AuthMetadataKeys.kt`

### 5) Estrategias de autenticacao na gateway
- Foi criado wiring por modo (`AUTH_MODE`) no modulo de interceptors da gateway.
- Foi integrado autenticador JWT com JOSE e scaffold para opaque introspection.

Ficheiros relevantes:
- `OmniAiGateway/interceptors/src/main/kotlin/org/omniai/gateway/interceptors/GatewayInterceptors.kt`
- `OmniAiGateway/interceptors/src/main/kotlin/org/omniai/gateway/interceptors/auth/JoseJwtTokenAuthenticator.kt`
- `OmniAiGateway/interceptors/src/main/kotlin/org/omniai/gateway/interceptors/auth/OpaqueIntrospectionAuth.kt`
- `OmniAiGateway/interceptors/build.gradle.kts`

### 6) Documentacao
- README atualizado com variaveis de ambiente de autenticacao.

Ficheiro relevante:
- `OmniAiGateway/README.md`

---

## Fluxo end-to-end (resumido)
1. App cliente chama endpoint da gateway.
2. Inbound extrai bearer token e cria metadata (`TypedMap`).
3. Adapter traduz request para dominio e copia metadata para `providerOptions`.
4. Pipeline executa `AuthContextInterceptor`.
5. Interceptor classifica token como `JWT` ou `OPAQUE`.
6. `TokenAuthenticator` decide permitir/negar conforme modo de auth.
7. Pedido segue para outbound/provider e resposta volta para a app.

---

## Modos de autenticacao
Definidos em `GatewayInterceptors.kt`:
- `OFF`: pass-through (sem validacao)
- `JWT_ONLY`: validacao JWT com JOSE/JWKS
- `JWT_OPAQUE_PREPARED`: JWT + caminho preparado para introspection de opaque

Flags relevantes:
- `AUTH_MODE`
- `AUTH_JWT_ISSUER`
- `AUTH_JWT_AUDIENCE`
- `AUTH_JWKS_URL`
- `AUTH_OPAQUE_INTROSPECTION_ENABLED`

---

## Estado atual (importante)

### Ja esta feito
- Extracao do token no inbound
- Propagacao do token ate ao core pipeline
- Deteccao de `JWT` vs `OPAQUE`
- Estrutura para plugar introspection de opaque
- Validacao JWT via JOSE (quando modo JWT esta ativo e configurado)

### Ainda nao esta implementado (de proposito)
- Cliente real de introspection contra AS (ex.: Keycloak)
- Chamada real ao endpoint `/introspect`

Ou seja: **arquiteturalmente pronto para AS, mas AS real ainda nao ligado**.

---

## Proximo passo quando quiser ativar AS real
1. Substituir `StubOpaqueTokenIntrospector` por implementacao real RFC7662.
2. Configurar credenciais/endpoint do AS.
3. Ligar modo correspondente (`AUTH_MODE` + flag de introspection).
4. (Opcional) melhorar mapeamento de erros de auth para 401/403.


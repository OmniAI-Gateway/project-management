# Authorization Server

Standalone Authorization Server for the AI Gateway, organized by bounded contexts and Clean Architecture layers.

## Bounded Contexts

- **Identity**: human login, OAuth/OIDC flows (Google OAuth first).
- **Management**: dashboard APIs for API key lifecycle and federation setup.
- **Token Engine**: low-latency token exchange endpoints.

## Folder Structure

```text
src/
  app/
    buildServer.ts
    routes.ts
  contexts/
    identity/
      domain/
      application/
      infrastructure/
    management/
      domain/
        entities/
          ApiKey.ts
      application/
      infrastructure/
    token-engine/
      application/
        ExchangeKeyUseCase.ts
        ports/
          ApiKeyReadRepository.ts
          TokenSigner.ts
      infrastructure/
        http/
          TokenController.ts
        persistence/
          InMemoryApiKeyReadRepository.ts
        security/
          JoseTokenSigner.ts
  main.ts
```

## Quick Start

```bash
npm install
npm run dev
```

Seeded API key for local testing: `sk-demo-123`

Exchange endpoint:

```http
POST /token/exchange-key
Content-Type: application/json

{ "apiKey": "sk-demo-123" }
```


# OmniAiGateway Configuration

This document describes all configuration options supported by `OmniAiGateway/src/main/resources/application.conf`, based on the current parsing logic in `Main.kt`.

## 1) Global options

| Key | Type | Default | Env override | Notes |
|---|---|---|---|---|
| `gateway.port` | int | `1900` | `PORT` | If `PORT` is present and valid int, it is used first. |
| `gateway.metrics.enabled` | boolean | `true` | - | Enables/disables telemetry interceptor setup. |

## 2) Provider activation

A provider is only enabled if its block exists:

- `gateway.providers.openai { ... }`
- `gateway.providers.gemini { ... }`
- `gateway.providers.anthropic { ... }`

If a provider block does not exist, its API key is **not** required.

At least one provider block must be configured, or startup fails.

## 3) Provider options

For each provider block `gateway.providers.<provider>`:

| Key | Type | Required | Env override | Notes |
|---|---|---|---|---|
| `baseUrl` | string | yes | `<PROVIDER>_BASE_URL` | Must be non-blank. |
| `model` | string | conditional | `<PROVIDER>_MODEL` | Used as fallback when list is not provided. |
| `models` | list<string> | conditional | `<PROVIDER>_MODELS` | Multiple models. |

Where `<PROVIDER>` is:

- `OPENAI`
- `GEMINI`
- `ANTHROPIC`

API key env vars (required only when provider block exists):

- `OPENAI_API_KEY`
- `GEMINI_API_KEY`
- `ANTHROPIC_API_KEY`

## 4) Model resolution precedence

For each enabled provider, models are resolved in this order:

1. `<PROVIDER>_MODELS` (CSV, e.g. `a,b,c`)
2. `gateway.providers.<provider>.models` (list in conf)
3. `<PROVIDER>_MODEL` env or `gateway.providers.<provider>.model` (single model)

If none resolves to a non-empty value, startup fails for that provider.

## 5) Example: single model per provider

```hocon
gateway {
  port = 1900

  metrics {
    enabled = true
  }

  providers {
    openai {
      model = "llama-3.3-70b-versatile"
      baseUrl = "https://api.groq.com/openai/v1"
    }

    gemini {
      model = "gemini-2.5-flash"
      baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    }
  }
}
```

## 6) Example: model list for one provider

```hocon
gateway {
  providers {
    gemini {
      models = ["gemini-2.5-flash", "gemini-1.5-pro"]
      baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    }
  }
}
```

Equivalent env override:

```powershell
$env:GEMINI_MODELS = "gemini-2.5-flash,gemini-1.5-pro"
```

## 7) Quick validation checklist

- Provider block exists only for providers you want active.
- Matching `<PROVIDER>_API_KEY` env var is set for each active provider.
- `baseUrl` is set (or `<PROVIDER>_BASE_URL` env var).
- At least one model exists via `models` or `model` resolution.


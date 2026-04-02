# OmniAiGateway

HTTP gateway for OpenAI, Gemini, and Anthropic formats, with interceptor pipeline and dynamic outbound wiring.

## Structure

- `app`: composition root (env/config + server bootstrap)
- `inbound:web`: Ktor routes and request parsing
- `services`: business service assembly and pipeline-backed service
- `interceptors`: gateway interceptors (logging + metrics)
- `outbound:builder`: dynamic outbound build from `KClass`
- `outbound:ollama`: gateway-specific outbound placeholder

## Environment variables

- `PORT` (default `1900`)
- `OPENAI_API_KEY` (required)
- `OPENAI_MODEL` (default `llama-3.3-70b-versatile`)
- `OPENAI_BASE_URL` (default `https://api.groq.com/openai/v1`)
- `GEMINI_API_KEY` (required)
- `GEMINI_MODEL` (default `gemini-2.5-flash:generateContent`)
- `GEMINI_BASE_URL` (default `https://generativelanguage.googleapis.com/v1beta/models/{model}`)
- `ANTHROPIC_API_KEY` (optional)
- `ANTHROPIC_MODEL` (default `claude-3-7-sonnet-latest`)
- `ANTHROPIC_BASE_URL` (default `https://api.anthropic.com/v1`)

## Endpoints

- `POST /v1/messages` (Anthropic)
- `POST /v1/chat/completions` (OpenAI)
- `POST /v1beta/models/{model}:generateContent` (Gemini)

## Run

```powershell
cd C:\ISEL\TerceiroAno\ProjetoFinal\projectManagement\project-management\OmniAiGateway
.\gradlew :app:run
```



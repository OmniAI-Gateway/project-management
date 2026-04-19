# Running OmniAiGateway with Config File and API Keys

This guide explains:
- how to provide an external configuration file (`.conf`)
- which environment variables are required
- the format for model-related environment variables
- how to run the app using a standard distribution or an uber JAR on Windows PowerShell

---

## 1) How configuration is loaded

The app uses `ConfigFactory.load()` (Typesafe Config), so configuration is resolved from:

1. JVM/system overrides (`-Dconfig.file`, `-Dconfig.resource`, `-Dconfig.url`)
2. `application.conf` on classpath (for example, inside the JAR)
3. environment variables explicitly read by code (`PORT`, provider API keys, model/base URL overrides)

### Practical implication
- If your artifact already contains `application.conf`, you do not need a local config file.
- If you want to use an external file, pass it with `-Dconfig.file=...`.

---

## 2) Provider activation and required API keys

A provider is only active if its block exists under `gateway.providers.<provider>` in config.

For each active provider, set the matching API key:
- `OPENAI_API_KEY`
- `GEMINI_API_KEY`
- `ANTHROPIC_API_KEY`

If a provider block does not exist, that provider API key is not required.

---

## 3) Environment variable format

Supported provider env vars (`<PROVIDER>` = `OPENAI`, `GEMINI`, `ANTHROPIC`):

- `<PROVIDER>_API_KEY` (required for active provider)
- `<PROVIDER>_BASE_URL` (optional override; provider still needs a non-blank base URL)
- `<PROVIDER>_MODEL` (single model)
- `<PROVIDER>_MODELS` (comma-separated list)

Global env vars:
- `PORT` (optional integer)

Examples:

```powershell
$env:OPENAI_MODEL="llama-3.3-70b-versatile"
$env:GEMINI_MODELS="gemini-2.5-flash,gemini-1.5-pro"
$env:PORT="1900"
```

---

## 4) Model resolution precedence

For each active provider, model selection follows this order:

1. `<PROVIDER>_MODELS` (CSV env var)
2. `gateway.providers.<provider>.models` (list in config)
3. `<PROVIDER>_MODEL` env var or `gateway.providers.<provider>.model` (single value)

If no non-empty model is resolved, startup fails.

---

## 5) Use external config file (`.conf`)

Run with explicit config path:

```powershell
java -Dconfig.file="C:\configs\gateway.conf" -jar .\build\libs\<your-jar>.jar
```

Run with config resource in classpath:

```powershell
java -Dconfig.resource=application.conf -jar .\build\libs\<your-jar>.jar
```

---

## 6) Build and run: current project tasks

In this project, the verified Gradle tasks include:
- `jar`
- `run`
- `distZip`
- `distTar`
- `installDist`

There is no `shadowJar` task by default.

### 6.1 Build and run distribution (recommended without uber JAR)

```powershell
cd C:\ISEL\TerceiroAno\ProjetoFinal\projectManagement\project-management\OmniAiGateway
.\gradlew.bat clean installDist
```

Then run:

```powershell
cd C:\ISEL\TerceiroAno\ProjetoFinal\projectManagement\project-management\OmniAiGateway
$env:OPENAI_API_KEY="YOUR_KEY"
$env:OPENAI_BASE_URL="https://api.groq.com/openai/v1"
$env:OPENAI_MODEL="llama-3.3-70b-versatile"
$env:PORT="1900"
.\build\install\OmniAiGateway\bin\OmniAiGateway.bat
```

To use an external config file with the start script:

```powershell
$env:JAVA_OPTS='-Dconfig.file="C:\configs\gateway.conf"'
.\build\install\OmniAiGateway\bin\OmniAiGateway.bat
```

---

## 7) Build and run an uber JAR (fat JAR)

If you want a single executable JAR with dependencies, add a fat-jar plugin (commonly Shadow) to `build.gradle.kts`, then build and run.

Typical commands after adding the plugin/task:

```powershell
cd C:\ISEL\TerceiroAno\ProjetoFinal\projectManagement\project-management\OmniAiGateway
.\gradlew.bat clean shadowJar
```

Run the uber JAR:

```powershell
cd C:\ISEL\TerceiroAno\ProjetoFinal\projectManagement\project-management\OmniAiGateway
$env:OPENAI_API_KEY="YOUR_KEY"
$env:OPENAI_BASE_URL="https://api.groq.com/openai/v1"
$env:OPENAI_MODEL="llama-3.3-70b-versatile"
java -Dconfig.file="C:\configs\gateway.conf" -jar .\build\libs\OmniAiGateway-<version>-all.jar
```

---

## 8) Minimal `gateway.conf` example

```hocon
gateway {
  port = 1900

  metrics {
    enabled = true
  }

  providers {
    openai {
      baseUrl = "https://api.groq.com/openai/v1"
      model = "llama-3.3-70b-versatile"
    }

    gemini {
      baseUrl = "https://generativelanguage.googleapis.com/v1beta"
      models = ["gemini-2.5-flash", "gemini-1.5-pro"]
    }
  }
}
```

---

## 9) Troubleshooting

- `No gateway providers configured`
  - Add at least one provider block under `gateway.providers.*`.

- `Environment variable '<X>_API_KEY' is required`
  - Set the API key env var for each active provider.

- `Missing baseUrl for provider ...`
  - Set `baseUrl` in config or `<PROVIDER>_BASE_URL` env var.

- Provider configured but no model defined
  - Set one of: `<PROVIDER>_MODELS`, `models`, `<PROVIDER>_MODEL`, or `model`.


import os
import subprocess
import sys

def run_claude():
    api_key = "no KEY"
    api_uri = "http://localhost:1900/"
    os.environ["ANTHROPIC_API_KEY"] = api_key
    os.environ["ANTHROPIC_BASE_URL"] = api_uri

    # Garante que o output seja exibido corretamente no terminal
    os.environ["PYTHONUNBUFFERED"] = "1"

    print(f"Iniciando Claude Code com URI: {api_uri}")

    try:
        subprocess.run(["claude", "--model" ,"randomModel"], check=True)
    except FileNotFoundError:
        print("Erro: O comando 'claude' não foi encontrado no seu PATH.")
        print("Certifique-se de que o Claude Code está instalado corretamente.")
    except KeyboardInterrupt:
        print("Encerrando Claude Code...")
        sys.exit(0)

if __name__ == "__main__":
    run_claude()
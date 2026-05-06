import os
import subprocess
import sys
import json
import urllib.request
import urllib.parse

def get_logto_token():
    """Obtém um token JWT do Logto usando Client Credentials."""
    url = "http://localhost:3001/oidc/token"
    data = {
        "grant_type": "client_credentials",
        "client_id": "jpnjkg638x2vhkdkl16rf",
        "client_secret": "eZg6p2wjtotiluJgXwf21Ndw07VbGrMx",
        "resource": "https://api.omniai.com",
        "scope": "all"
    }
    
    encoded_data = urllib.parse.urlencode(data).encode("utf-8")
    req = urllib.request.Request(url, data=encoded_data, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")
    
    try:
        print("A solicitar token JWT ao Logto...")
        with urllib.request.urlopen(req, timeout=5) as response:
            res_data = json.loads(response.read().decode("utf-8"))
            token = res_data.get("access_token")
            if token:
                print("Token JWT obtido com sucesso.")
                return token
            else:
                print("Erro: Resposta do Logto não contém access_token.")
                return None
    except Exception as e:
        print(f"Aviso: Não foi possível obter o token do Logto ({e}).")
        return None

def run_claude():
    # Tenta obter o token dinamicamente
    token = get_logto_token()
    
    api_uri = "http://localhost:1900/"
    # A API Key enviada no header 'x-api-key' (pode ser dummy se o Gateway não a exigir)
    os.environ["ANTHROPIC_API_KEY"] = "no KEY"
    os.environ["ANTHROPIC_BASE_URL"] = api_uri

    # Injeta o JWT no header 'Authorization' de forma independente
    if token:
        os.environ["ANTHROPIC_CUSTOM_HEADERS"] = f"Authorization: Bearer {token}"
        print("Header 'Authorization' configurado com o token JWT.")
    else:
        # Limpa o header se não houver token
        os.environ.pop("ANTHROPIC_CUSTOM_HEADERS", None)

    # Garante que o output seja exibido corretamente no terminal
    os.environ["PYTHONUNBUFFERED"] = "1"

    print(f"Iniciando Claude Code com URI: {api_uri}")

    try:
        subprocess.run(["claude", "--model" ,"randomModel"], check=True)
    except FileNotFoundError:
        print("Erro: O comando 'claude' não foi encontrado no seu PATH.")
        print("Certifique-se de que o Claude Code está instalado corretamente.")
    except KeyboardInterrupt:
        print("\nEncerrando Claude Code...")
        sys.exit(0)


if __name__ == "__main__":
    run_claude()
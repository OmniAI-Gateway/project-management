import os
import subprocess
import sys
import json
import urllib.request
import urllib.parse
import base64
import hashlib
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer

# ==========================================
# LOGTO CONFIGURATIONS
# ==========================================
LOGTO_OIDC_ENDPOINT = "http://localhost:3001/oidc"
# WARNING: Use a "Native App" Client ID, not the previous M2M one!
CLIENT_ID = "9gmdflfyxl7skacyvdbzs"
REDIRECT_URI = "http://localhost:8080/callback"
RESOURCE = "https://api.omniai.com"
SCOPE = "openid profile email omniai-api-access" # Kept your 'all' scope and added 'openid' to ensure OIDC flow

# Global variable to store the redirection code
auth_code = None

# ==========================================
# SECURITY FUNCTIONS (PKCE) AND SERVER
# ==========================================
def generate_pkce_pair():
    """Generates the code_verifier and code_challenge for PKCE."""
    verifier = base64.urlsafe_b64encode(os.urandom(40)).rstrip(b'=').decode('utf-8')
    challenge = base64.urlsafe_b64encode(
        hashlib.sha256(verifier.encode('utf-8')).digest()
    ).rstrip(b'=').decode('utf-8')
    return verifier, challenge

class CallbackHandler(BaseHTTPRequestHandler):
    """Micro-server to catch the code after browser login."""
    def do_GET(self):
        global auth_code
        query = urllib.parse.urlparse(self.path).query
        params = urllib.parse.parse_qs(query)

        if 'code' in params:
            auth_code = params['code'][0]
            self.send_response(200)
            self.send_header('Content-type', 'text/html; charset=utf-8')
            self.end_headers()
            html = """
            <html><body>
                <h1 style="color: green;">Authentication successful!</h1>
                <p>The JWT has been generated. You can close this window and return to the terminal.</p>
                <script>window.close();</script>
            </body></html>
            """
            self.wfile.write(html.encode('utf-8'))
        else:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b"Error: Authorization code not received.")

    # Hides HTTP server logs in the terminal
    def log_message(self, format, *args):
        pass

# ==========================================
# TOKEN RETRIEVAL LOGIC
# ==========================================
def get_logto_token():
    """Retrieves a JWT token from Logto by opening the browser and using PKCE."""
    print("Starting browser login process...")

    code_verifier, code_challenge = generate_pkce_pair()

    # 1. Build the Authorization URL
    auth_url = (
        f"{LOGTO_OIDC_ENDPOINT}/auth?"
        f"client_id={CLIENT_ID}&"
        f"redirect_uri={urllib.parse.quote(REDIRECT_URI)}&"
        f"response_type=code&"
        f"scope={urllib.parse.quote(SCOPE)}&"
        f"resource={urllib.parse.quote(RESOURCE)}&"
        f"code_challenge={code_challenge}&"
        f"code_challenge_method=S256"
    )

    # 2. Open the browser
    webbrowser.open(auth_url)

    # 3. Start the temporary local server and wait for the redirect
    print("Waiting for browser authentication...")
    server = HTTPServer(('localhost', 8080), CallbackHandler)
    server.handle_request() # Blocks until 1 request is received

    if not auth_code:
        print("Warning: Could not retrieve the authorization code.")
        return None

    print("Code received! Exchanging for JWT token...")

    # 4. Exchange the code for the Access Token
    token_url = f"{LOGTO_OIDC_ENDPOINT}/token"
    data = {
        "grant_type": "authorization_code",
        "client_id": CLIENT_ID,
        "redirect_uri": REDIRECT_URI,
        "code": auth_code,
        "code_verifier": code_verifier,
        "resource": RESOURCE,
        "scope": SCOPE
    }

    encoded_data = urllib.parse.urlencode(data).encode("utf-8")
    req = urllib.request.Request(token_url, data=encoded_data, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")

    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            res_data = json.loads(response.read().decode("utf-8"))
            token = res_data.get("access_token")
            if token:
                print("JWT token successfully retrieved.")
                return token
            else:
                print("Error: Logto response does not contain an access_token.")
                print(res_data)
                return None
    except Exception as e:
        print(f"Error retrieving token from Logto: {e}")
        return None

# ==========================================
# MAIN LOGIC (Claude)
# ==========================================
def run_claude():
    # Gets the token by opening the browser
    token = get_logto_token()

    api_uri = "http://localhost:1900/"
    os.environ["ANTHROPIC_API_KEY"] = "no KEY"
    os.environ["ANTHROPIC_BASE_URL"] = api_uri

    # Injects the JWT into the 'Authorization' header independently
    if token:
        os.environ["ANTHROPIC_CUSTOM_HEADERS"] = f"Authorization: Bearer {token}"
        print("'Authorization' header configured with the JWT token to send to the Gateway.")
    else:
        # If login fails, we can choose not to run claude or run without the header
        print("Authentication failed. Exiting.")
        sys.exit(1)

    # Ensures output is displayed correctly in the terminal
    os.environ["PYTHONUNBUFFERED"] = "1"

    print(f"Starting Claude Code with URI: {api_uri}")

    try:
        # Replace with your actual model later
        subprocess.run(["claude", "--model" ,"randomModel"], check=True)
    except FileNotFoundError:
        print("Error: The 'claude' command was not found in your PATH.")
        print("Make sure Claude Code is installed correctly.")
    except KeyboardInterrupt:
        print("\nExiting Claude Code...")
        sys.exit(0)

if __name__ == "__main__":
    run_claude()
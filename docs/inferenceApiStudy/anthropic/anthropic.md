Anthopic Inference api documentation
De notar que estes pedidos foram executados usando um modelo local via Ollama, por motivos de nao existir maneira gratuita de testar os modelos da anthropic reais 
de forma gratuita, o ollama oference total compatibilidade com a anthropic 
pedido:
POST https://api.anthropic.com/v1/messages
x-api-key: {{ANTHROPIC_API_KEY}}
anthropic-version: 2023-06-01
Content-Type: application/json

{
"model": "claude-3-5-haiku-20241022",
"max_tokens": 1024,
"messages": [
{
"role": "user",
"content": "How does AI work?"
}
]
}

resposta:

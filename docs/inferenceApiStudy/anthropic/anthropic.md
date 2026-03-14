Anthopic Inference api documentation
De notar que estes pedidos foram executados usando um modelo local via Ollama, por motivos de nao existir maneira gratuita de testar os modelos da anthropic reais 
de forma gratuita, o ollama oference compatibilidade com a anthropic , a api usada para a gateway ser com base na fornecida pelo ollama

pedido:
# curl https://api.anthropic.com/v1/messages
#  -H "x-api-key: $ANTHROPIC_API_KEY"
#  -H "anthropic-version: 2023-06-01"
#  -H "content-type: application/json"
#  -d '{
#    "model": "claude-3-5-haiku-20241022",
#    "max_tokens": 1024,
#    "messages": [
#      {
#        "role": "user",
#        "content": "How does AI work?"
#      }
#    ]
#  }'
resposta:

FROM ollama/ollama:latest

# copiar script que faz pull do modelo
COPY pull-model.sh /pull-model.sh

RUN chmod +x /pull-model.sh

# arrancar ollama, baixar modelo e parar
RUN /pull-model.sh

EXPOSE 11434

CMD ["serve"]
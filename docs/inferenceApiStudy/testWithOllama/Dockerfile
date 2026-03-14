FROM ollama/ollama:latest

ARG MODEL_NAME=llama3.1:8b

ENV MODEL_NAME=$MODEL_NAME

COPY pull-model.sh /pull-model.sh

RUN chmod +x /pull-model.sh && /pull-model.sh

EXPOSE 11434
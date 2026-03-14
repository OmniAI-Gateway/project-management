#!/bin/bash
set -e

MODEL=${MODEL_NAME:-llama3.1:8b}

echo "A descarregar o modelo: $MODEL..."

ollama serve &
pid=$!

sleep 5

ollama pull $MODEL

kill $pid
wait $pid
#!/bin/bash

set -e

# iniciar ollama em background
ollama serve &
pid=$!

# esperar API arrancar
sleep 5

# baixar modelo leve
ollama pull phi3:mini

# parar servidor
kill $pid

#!/bin/bash
# Mini-train script for quick testing
# Usage: ./mini_train.sh [nlu|stt|tts]

set -e

MODEL_TYPE=${1:-nlu}

echo "========================================"
echo "MINI-TRAIN MODE - Quick Test"
echo "Model: $MODEL_TYPE"
echo "========================================"

export MINI_TRAIN=true

case $MODEL_TYPE in
  nlu)
    echo "Training NLU model with mini dataset..."
    python nlu/generate_from_templates.py
    python nlu/train.py
    ;;
  stt)
    echo "Training STT model with mini dataset..."
    python stt/train.py
    ;;
  tts)
    echo "Training TTS model with mini dataset..."
    python tts/train.py
    ;;
  *)
    echo "Invalid model type. Use: nlu, stt, or tts"
    exit 1
    ;;
esac

echo ""
echo "========================================"
echo "MINI-TRAIN COMPLETED"
echo "========================================"

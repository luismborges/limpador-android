#!/bin/bash
# =============================================================
# Script: Sobe o Limpador Android para o GitHub e gera o APK
# Uso: ./subir-github.sh SEU_USUARIO_GITHUB
# Exemplo: ./subir-github.sh luisborges
# =============================================================

set -e

USUARIO="${1}"
REPO="limpador-android"

if [ -z "$USUARIO" ]; then
  echo "❌ Informe seu usuário do GitHub:"
  echo "   ./subir-github.sh SEU_USUARIO"
  exit 1
fi

echo ""
echo "========================================"
echo "  Limpador Android → GitHub"
echo "========================================"
echo ""

# 1. Verificar se gh CLI está instalado
if ! command -v gh &> /dev/null; then
  echo "📦 Instalando GitHub CLI..."
  type -p curl >/dev/null || (sudo apt update && sudo apt install curl -y)
  curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
  sudo apt update && sudo apt install gh -y
fi

# 2. Login no GitHub (abre browser)
echo "🔑 Fazendo login no GitHub..."
echo "   (vai abrir o navegador para autorizar)"
gh auth login --web --git-protocol https

# 3. Criar repositório no GitHub
echo ""
echo "📁 Criando repositório '$REPO' no GitHub..."
gh repo create "$REPO" \
  --public \
  --description "App limpador de celulares Android — libera armazenamento e acelera o dispositivo" \
  --source=. \
  --remote=origin \
  --push || {
    # Se o repo já existe, só faz o push
    echo "   Repositório já existe. Fazendo push..."
    git remote set-url origin "https://github.com/$USUARIO/$REPO.git" 2>/dev/null || \
    git remote add origin "https://github.com/$USUARIO/$REPO.git"
    git push -u origin main
  }

echo ""
echo "✅ Código enviado com sucesso!"
echo ""
echo "========================================"
echo "  PRÓXIMOS PASSOS:"
echo "========================================"
echo ""
echo "1. Acesse: https://github.com/$USUARIO/$REPO/actions"
echo "   → O build do APK está rodando automaticamente!"
echo ""
echo "2. Aguarde ~5 minutos até o build terminar"
echo ""
echo "3. Quando aparecer ✅ verde, clique em:"
echo "   'Build APK' → 'LimpadorAndroid-debug' → baixe o ZIP"
echo ""
echo "4. Extraia o ZIP — dentro estará o arquivo:"
echo "   app-debug.apk"
echo ""
echo "5. Transfira para o Moto E7 (WhatsApp/USB/Drive)"
echo "   e instale habilitando 'Fontes desconhecidas'"
echo ""
echo "🔗 Link direto para os Actions:"
echo "   https://github.com/$USUARIO/$REPO/actions"
echo ""

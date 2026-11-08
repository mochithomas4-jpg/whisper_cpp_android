#!/usr/bin/env bash
set -euo pipefail

# ==========================================
# Android SDK + Java 17 installer for Codespaces
# ==========================================

SDK_DIR="$HOME/Android/Sdk"
JAVA_DIR="$HOME/java/jdk-17"
TMP_DIR="$(mktemp -d)"

ANDROID_ZIP="commandlinetools-linux-15859902_latest.zip"
ANDROID_URL="https://dl.google.com/android/repository/$ANDROID_ZIP"
JAVA_URL="https://api.adoptium.net/v3/binary/latest/17/ga/linux/$(case "$(uname -m)" in x86_64|amd64) echo x64 ;; aarch64|arm64) echo aarch64 ;; *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;; esac)/jdk/hotspot/normal/eclipse"

trap 'rm -rf "$TMP_DIR"' EXIT

echo "=========================================="
echo " Installing Android SDK + JDK 17"
echo "=========================================="

# Dependencies
sudo apt-get update
sudo apt-get install -y wget unzip tar

# ------------------------------------------
# Android SDK Command Line Tools
# ------------------------------------------

echo "[1/5] Download Android Command Line Tools..."

wget -q --show-progress "$ANDROID_URL" \
    -O "$TMP_DIR/$ANDROID_ZIP"

echo "[2/5] Extract Android Command Line Tools..."

mkdir -p "$SDK_DIR/cmdline-tools"

unzip -q "$TMP_DIR/$ANDROID_ZIP" -d "$TMP_DIR/android"

rm -rf "$SDK_DIR/cmdline-tools/latest"

mv "$TMP_DIR/android/cmdline-tools" \
    "$SDK_DIR/cmdline-tools/latest"

# Remove ZIP
rm -f "$TMP_DIR/$ANDROID_ZIP"

# ------------------------------------------
# Java 17
# ------------------------------------------

echo "[3/5] Download Java JDK 17..."

wget -q --show-progress "$JAVA_URL" \
    -O "$TMP_DIR/jdk17.tar.gz"

echo "[4/5] Extract Java JDK 17..."

mkdir -p "$JAVA_DIR"

tar -xzf "$TMP_DIR/jdk17.tar.gz" \
    -C "$JAVA_DIR" \
    --strip-components=1

# Remove Java archive
rm -f "$TMP_DIR/jdk17.tar.gz"

# ------------------------------------------
# Environment variables
# ------------------------------------------

echo "[5/5] Configure environment..."

# Remove previous managed configuration
sed -i '/# >>> ANDROID SDK + JAVA 17 >>>/,/# <<< ANDROID SDK + JAVA 17 <<</d' \
    "$HOME/.bashrc"

cat >> "$HOME/.bashrc" <<'EOF'

# >>> ANDROID SDK + JAVA 17 >>>
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

export JAVA_HOME="$HOME/java/jdk-17"

export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
# <<< ANDROID SDK + JAVA 17 <<<
EOF

# Activate immediately in current shell
export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export JAVA_HOME="$JAVA_DIR"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo
echo "=========================================="
echo " INSTALLATION COMPLETE"
echo "=========================================="

echo "ANDROID_HOME = $ANDROID_HOME"
echo "JAVA_HOME    = $JAVA_HOME"

echo
java -version
echo
sdkmanager --version

echo
echo "Restart terminal or run: source ~/.bashrc"

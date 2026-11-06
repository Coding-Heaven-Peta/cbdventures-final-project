#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Installs Temurin JDK 25 (the version this project is built with) if it is
# not already present, then prints the JAVA_HOME to use.
#
#   source tools/setup-jdk.sh            # installs and exports JAVA_HOME
#   bash tools/setup-jdk.sh              # installs and prints JAVA_HOME
#
# The sandbox image only ships JDK 11, so this script keeps `./mvnw` working
# after the environment is reset. On a machine that already has JDK 25 it is a
# no-op.
# ---------------------------------------------------------------------------
set -euo pipefail

JDK_DIR="/usr/lib/jvm/java-25-openjdk-amd64"

if [ -x "$JDK_DIR/bin/java" ] && "$JDK_DIR/bin/java" -version 2>&1 | grep -q '"25'; then
  echo "JDK 25 already installed at $JDK_DIR"
else
  echo "Installing Temurin JDK 25..."
  TMP="$(mktemp -d)"
  curl -sL --max-time 900 -o "$TMP/jdk.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse"
  tar xzf "$TMP/jdk.tar.gz" -C "$TMP"
  SRC="$(find "$TMP" -maxdepth 1 -type d -name 'jdk-25*' | head -1)"
  if [ ! -d "$SRC" ]; then
    echo "Download did not contain a JDK 25 folder" >&2
    exit 1
  fi
  # /usr/lib/jvm needs root; fall back to a user-local JDK when sudo is absent.
  if sudo -n true 2>/dev/null; then
    sudo rm -rf "$JDK_DIR"
    sudo mv "$SRC" "$JDK_DIR"
  else
    JDK_DIR="$HOME/jdk25"
    rm -rf "$JDK_DIR"
    cp -r "$SRC" "$JDK_DIR"
  fi
  rm -rf "$TMP"
  echo "Installed at $JDK_DIR"
fi

"$JDK_DIR/bin/java" -version

export JAVA_HOME="$JDK_DIR"
if [ "${BASH_SOURCE[0]}" != "${0}" ]; then
  echo "JAVA_HOME exported for this shell"
else
  echo "Add this to your shell: export JAVA_HOME=$JDK_DIR"
fi

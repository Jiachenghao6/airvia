#!/bin/bash
# One-shot release for the fork: bump version, build, commit, push, publish.
#
#   bash scripts/release.sh "这次改了什么"
#
# The APK is always uploaded as Airvia.apk, so this link never changes:
#   https://github.com/Jiachenghao6/airvia/releases/latest/download/Airvia.apk
#
# Build on this machine only: the APK is signed with ~/.android/debug.keystore,
# and Android refuses to update over an install signed with a different key.
set -euo pipefail
cd "$(dirname "$0")/.."

NOTES="${1:?usage: bash scripts/release.sh \"release notes\"}"
GRADLE=app/build.gradle.kts

if [ -n "$(git status --porcelain)" ]; then
  echo "Working tree not clean — commit or stash first." >&2
  exit 1
fi

# versionCode must grow or the phone won't install over the old one.
code=$(grep -oE 'versionCode = [0-9]+' "$GRADLE" | grep -oE '[0-9]+')
fork=$(grep -oE 'versionName = "1\.2\.4-fork(\.[0-9]+)?"' "$GRADLE" | grep -oE 'fork(\.[0-9]+)?' | grep -oE '[0-9]+$' || echo 0)
next_code=$((code + 1))
next_fork=$((fork + 1))
name="1.2.4-fork.$next_fork"
sed -i -E "s/versionCode = [0-9]+/versionCode = $next_code/; s/versionName = \"[^\"]+\"/versionName = \"$name\"/" "$GRADLE"

export JAVA_HOME="${JAVA_HOME:-$HOME/scoop/apps/temurin17-jdk/current}"
./gradlew assembleRelease --console=plain -q

out=$(mktemp -d)
cp app/build/outputs/apk/release/app-release.apk "$out/Airvia.apk"
sum=$(sha256sum "$out/Airvia.apk" | cut -c1-64)

git add "$GRADLE"
git commit -q -m "发版 $name"
git push -q origin main

gh release create "v$name" "$out/Airvia.apk" \
  -R Jiachenghao6/airvia --target main --latest \
  --title "Airvia $name" \
  --notes "$NOTES

SHA-256: $sum"

echo "Released $name (versionCode $next_code)"
echo "https://github.com/Jiachenghao6/airvia/releases/latest/download/Airvia.apk"

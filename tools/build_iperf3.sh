#!/usr/bin/env bash
# iperf3를 안드로이드(arm64)용으로 빌드해서 jniLibs에 libiperf3.so 이름으로 복사합니다.
# 사용법: ANDROID_NDK_HOME=/path/to/ndk ./tools/build_iperf3.sh
# (Linux, macOS, Windows WSL에서 실행)
set -euo pipefail

: "${ANDROID_NDK_HOME:?ANDROID_NDK_HOME 환경변수에 NDK 경로를 지정하세요}"
IPERF_TAG="${IPERF_TAG:-3.15}"   # 3.16 이상은 pthread_cancel 때문에 bionic에서 빌드가 실패할 수 있음
API=34
TARGET=aarch64-linux-android

case "$(uname -s)" in
  Linux)  HOST_TAG=linux-x86_64 ;;
  Darwin) HOST_TAG=darwin-x86_64 ;;
  *) echo "지원하지 않는 OS입니다. WSL을 사용하세요."; exit 1 ;;
esac

TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG"
export CC="$TOOLCHAIN/bin/${TARGET}${API}-clang"
export AR="$TOOLCHAIN/bin/llvm-ar"
export RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
export STRIP="$TOOLCHAIN/bin/llvm-strip"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/tools/_build"
OUT="$ROOT/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$WORK" "$OUT"
cd "$WORK"

if [ ! -d iperf ]; then
  git clone https://github.com/esnet/iperf.git
fi
cd iperf
git fetch --tags
git checkout "$IPERF_TAG"
git clean -fdx >/dev/null

./configure --host="$TARGET" --disable-shared --enable-static --without-openssl \
  CFLAGS="-fPIE -O2" \
  LDFLAGS="-pie -Wl,-z,max-page-size=16384"
make -j"$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)"

"$STRIP" src/iperf3
cp src/iperf3 "$OUT/libiperf3.so"
echo "완료: $OUT/libiperf3.so"

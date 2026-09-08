#!/usr/bin/env bash
set -euo pipefail

tarball=$(realpath "${1:?Expected a Linux release tarball}")
arch=${2:?Expected x86_64 or aarch64}
version=${3:?Expected a semantic version}
case "$arch" in
  x86_64|aarch64) ;;
  *) echo "Unsupported AppImage architecture: $arch" >&2; exit 1 ;;
esac
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
test -s "$tarball"

repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)
output_dir="$repo_dir/desktopApp/build/appimage"
mkdir -p "$output_dir"
work_dir=$(mktemp -d "$output_dir/work-$arch.XXXXXX")
trap 'rm -rf -- "$work_dir"' EXIT
app_dir="$work_dir/Keyguard.AppDir"
mkdir -p "$app_dir/usr"
tar -xzf "$tarball" -C "$app_dir/usr" --strip-components=1

app_id=com.artemchep.keyguard
test -s "$app_dir/usr/share/applications/$app_id.desktop"
test -s "$app_dir/usr/share/metainfo/$app_id.metainfo.xml"
test -s "$app_dir/usr/share/icons/hicolor/scalable/apps/$app_id.svg"
ln -s "usr/share/applications/$app_id.desktop" "$app_dir/$app_id.desktop"
ln -s "usr/share/icons/hicolor/scalable/apps/$app_id.svg" "$app_dir/$app_id.svg"
cp "$repo_dir/desktopApp/icon.png" "$app_dir/.DirIcon"
cp "$repo_dir/desktopApp/appimage/AppRun" "$app_dir/AppRun"
chmod 755 "$app_dir/AppRun"

# Pin both downloads: appimagetool otherwise fetches a moving runtime release.
tool_version=1.9.1
runtime_version=20251108
tool="$work_dir/appimagetool-$arch.AppImage"
runtime="$work_dir/runtime-$arch"
curl --fail --location --retry 3 --output "$tool" \
  "https://github.com/AppImage/appimagetool/releases/download/$tool_version/appimagetool-$arch.AppImage"
curl --fail --location --retry 3 --output "$runtime" \
  "https://github.com/AppImage/type2-runtime/releases/download/$runtime_version/runtime-$arch"
(
  cd "$work_dir"
  sha256sum --check "$repo_dir/.github/actions/build_appimage/$arch.sha256"
)
chmod 755 "$tool"

filename="Keyguard-$version-linux-$arch.AppImage"
appimage="$work_dir/$filename"
ARCH="$arch" VERSION="$version" "$tool" --appimage-extract-and-run \
  --runtime-file "$runtime" "$app_dir" "$appimage"
test -s "$appimage"
test -x "$appimage"

# Exercise the final AppImage's runtime and launcher without requiring FUSE.
APPIMAGE_EXTRACT_AND_RUN=1 python3 "$repo_dir/scripts/run_native_crypto_desktop_package_smoke.py" \
  "$app_dir" --platform linux --launcher "$appimage"

# Publish the output only after all validation succeeds.
mv -f -- "$appimage" "$output_dir/$filename"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "path=$output_dir/$filename" >> "$GITHUB_OUTPUT"
fi
echo "Built $output_dir/$filename"

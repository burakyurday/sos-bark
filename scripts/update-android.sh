#!/usr/bin/env bash
# SOS Bark v1.1.0 - Mac-to-Android update preserving existing private settings.
set -euo pipefail
umask 077

PACKAGE="com.mutfakajans.sos.redmi"
APK_URL="https://raw.githubusercontent.com/burakyurday/sos-bark/main/downloads/SOS-RedmiNote8-v1.1.0.apk"
APK_PATH="$HOME/Downloads/SOS-v1.1.0.apk"
BACKUP_PATH="$HOME/Downloads/.sos-bark-private-backup.xml"

info() { printf "\n%s\n" "$1"; }
fail() { printf "\nHATA: %s\n" "$1" >&2; exit 1; }

command -v adb >/dev/null 2>&1 || fail "ADB yok. Önce: brew install android-platform-tools"
command -v curl >/dev/null 2>&1 || fail "curl komutu bulunamadi."

CONNECTED="$(adb devices | awk 'NR>1 && $2=="device" {n++} END {print n+0}')"
[ "$CONNECTED" = "1" ] || fail "Tam bir adet yetkilendirilmis Android telefon bagla (adb devices -> device)."

mkdir -p "$HOME/Downloads"
info "1/4 - SOS v1.1.0 APK indiriliyor..."
curl -fLsS --retry 2 "$APK_URL" -o "$APK_PATH" || fail "APK indirilemedi. GitHub Actions derlemesini kontrol et."
[ -s "$APK_PATH" ] || fail "APK bos indi."

HAVE_EXISTING=false
if adb shell pm path "$PACKAGE" 2>/dev/null | grep -q '^package:'; then
    HAVE_EXISTING=true
    info "2/4 - Mevcut Bark anahtari telefondan guvenli sekilde Mac'e yedekleniyor..."
    # run-as works because both versions are signed as debug APKs.
    if ! adb exec-out run-as "$PACKAGE" cat shared_prefs/sos_private.xml > "$BACKUP_PATH"; then
        rm -f "$BACKUP_PATH"
        fail "Mevcut uygulama ayarlari okunamadi. Eski uygulama KALDIRILMADI. Anahtari kaybetmemek icin durduruldu."
    fi
    chmod 600 "$BACKUP_PATH"
    if ! grep -q 'name="bark_key"' "$BACKUP_PATH"; then
        rm -f "$BACKUP_PATH"
        fail "Yedekte Bark anahtari bulunamadi. Eski uygulama KALDIRILMADI."
    fi
    info "Mevcut ayarlar yedeklendi. Eski surum kaldiriliyor..."
    adb uninstall "$PACKAGE" || fail "Eski uygulama kaldirilamadi. Yedek dosyasi: $BACKUP_PATH"
else
    info "2/4 - Eski SOS Alarm kurulu degil; ilk yukleme yapilacak."
fi

info "3/4 - Yeni SOS Alarm kuruluyor..."
adb install "$APK_PATH" || fail "Kurulum reddedildi. Daha onceki ayar yedegin varsa $BACKUP_PATH konumunda korundu. Telefonun USB uzerinden yukle iznini kontrol et."

if [ "$HAVE_EXISTING" = true ]; then
    info "4/4 - Bark anahtari yeniden uygulamaya aktariliyor..."
    adb shell run-as "$PACKAGE" mkdir -p shared_prefs || fail "Yeni uygulama ayarlar klasoru acilamadi. Yedek: $BACKUP_PATH"
    adb shell -T "run-as $PACKAGE sh -c 'cat > shared_prefs/sos_private.xml'" < "$BACKUP_PATH" || fail "Ayarlari geri yukleyemedim. Yedek: $BACKUP_PATH"
    adb shell run-as "$PACKAGE" chmod 600 shared_prefs/sos_private.xml || fail "Yedek dosyasi izinleri ayarlanamadi. Yedek: $BACKUP_PATH"
    if ! adb exec-out run-as "$PACKAGE" cat shared_prefs/sos_private.xml | cmp - "$BACKUP_PATH" >/dev/null; then
        fail "Ayar dogrulama basarisiz. Yedek dosyasi KORUNDU: $BACKUP_PATH"
    fi
    rm -f "$BACKUP_PATH"
else
    info "4/4 - Ilk kurulum: Bark anahtarini Ayarlar ekranindan gir."
fi

adb shell am start -n "$PACKAGE/com.mutfakajans.sos.MainActivity" || true
printf "\nBASARILI: SOS Alarm v1.1.0 kuruldu. Kayitli Bark bilgilerin korundu.\n"

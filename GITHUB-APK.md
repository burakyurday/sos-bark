# Android APK kurulumu

1. [Doğrudan debug APK](https://github.com/burakyurday/sos-bark/raw/refs/heads/main/downloads/SOS-RedmiNote8-v1.1.0.apk) veya [Actions](https://github.com/burakyurday/sos-bark/actions/workflows/build-apk.yml) üzerinden son başarılı derlemedeki `SOS-RedmiNote8-v1.1.0-APK` artifact'ını kullan.
2. Artifact ZIP olarak indiyse `app-debug.apk` dosyasını çıkar. APK'yı Android dosya yöneticisinden aç. İşletim sistemi uyarılarını değerlendir ve yalnızca doğruladığın kaynaktan yükle.
3. iPhone'da Bark'ı kur ve bildirim izinlerini yapılandır. Bark anahtarını yalnızca Android uygulamasına gir.
4. Önce test butonunu kullan ve bildirimin gerçek iPhone'da alındığını doğrula.

Bu debug sürümünün paket adı `com.mutfakajans.sos.redmi`. Debug imzası değiştiğinde güncelleme için yeniden kurulum gerekebilir. Gerçek acil durumlarda 112'yi kullan; API isteğinin kabul edilmesi kesin teslimat kanıtı değildir.

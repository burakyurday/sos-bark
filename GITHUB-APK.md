# GitHub Actions ile APK oluşturma

1. Repository'de **Actions** sekmesini aç.
2. **SOS Android APK** iş akışına gir. `main` dalına kod gönderildiğinde otomatik çalışır.
3. Yeşil sonuç alındığında `SOS-Bark-debug-APK` artifact'ını indir. İçinde `app-debug.apk` vardır.
4. Android'de bilinmeyen kaynaklardan yüklemeye izin verip APK'yı kur.
5. iPhone'da Bark'ı kurup Kritik Uyarılar iznini aç. Bark anahtarını sadece Android cihazdaki uygulama alanına gir; GitHub'a ekleme.

Gerçek acil durumlarda 112 kullanılmalıdır. Bark sunucusunun isteği kabul etmesi iPhone'a kesin teslim kanıtı değildir.

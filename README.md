# SOS Bark Android

**A lightweight Android-to-iPhone SOS notification prototype.**

Android application that sends an SOS request through Bark to an iPhone. Built for personal notification experiments, not as a replacement for emergency services.

`Android` · `Java 17` · `Gradle` · `Bark API`

> **Development status:** Prototype. Notification delivery depends on network access, Bark and iOS configuration. Test end-to-end before relying on it.

---

Arkadaşının Android telefonundan iPhone'daki Bark uygulamasına kritik bildirim göndermek için basit SOS prototipi.

## Redmi Note 8 için kurulum
- **Actions** sekmesindeki son başarılı iş akışından `SOS-RedmiNote8-APK` ZIP'ini indir.
- ZIP içindeki **app-debug.apk** dosyasını Android telefondaki **İndirilenler** klasörüne çıkar. ZIP dosyasını doğrudan yüklemeye çalışma.
- Telefonun **Dosya Yöneticisi** uygulamasına, gerekirse `Ayarlar → Özel uygulama erişimi → Bilinmeyen uygulamaları yükle` üzerinden izin ver.
- Önceki `SOS` sürümüyle çakışmayı önlemek için bu sürümün paket kimliği `com.mutfakajans.sos.redmi` olarak değiştirildi.
- Uygulama açıldığında Bark anahtarını ve arkadaşının adını gir, önce **test** butonunu kullan.

**Önemli:** Bark anahtarını GitHub'a koyma. Bark sunucusunun isteği kabul etmesi bildirimin iPhone'a ulaştığı anlamına gelmez. Gerçek acil durumlarda resmi acil yardım numarasını (Türkiye'de 112) ara.

Bu sürüm debug imzasıyla üretilir. GitHub'ın her yeni bulut derlemesinde debug anahtarı değişebileceğinden ilerideki APK'larda uygulamayı kaldırıp yeniden kurman gerekebilir; kalıcı güncelleme için özel imzalama anahtarı kurulmalıdır.

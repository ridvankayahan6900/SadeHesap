# Sade Hesap

Sade, hızlı ve dikkat dağıtmayan bir hesap makinesi uygulaması. Reklam yok, internet izni
yok, üçüncü taraf SDK yok, analitik yok — sadece dört işlem, yüzde, işaret değiştirme ve
son 20 hesabı gösteren bir geçmiş.

## Özellikler

- Jetpack Compose + Material 3, Android 12+ cihazlarda dinamik renk (duvar kâğıdına göre
  Material You paleti), öncesinde sabit yeşil tema; sistem açık/koyu temasını izler.
- Dikey ve yatay yerleşim: yatayda geçmiş yan panelde, dikeyde üstte gösterilir.
- İşlem önceliği doğru uygulanır (× ve ÷ önce), hesaplamalar `BigDecimal` ile 20 basamak
  hassasiyetle yapılır, sonuç en fazla 12 anlamlı basamakla gösterilir.
- Ondalık ayırıcı görünümde yerel ayara göre değişir (virgül/nokta), hesap her zaman nokta
  ile yapılır.
- Sonuca uzun basınca panoya kopyalanır.
- Türkçe (varsayılan), İngilizce ve Fransızca dil desteği.
- Erişilebilirlik: her tuşun `contentDescription`'ı, en az 48dp dokunma hedefi.
- Kalıcı depolama yok; geçmiş yalnızca `rememberSaveable` ile döndürme/yapılandırma
  değişikliklerinde korunur, uygulama kapatılınca sıfırlanır.

## Nasıl derlenir

Gereksinimler: JDK 17, internet erişimi (ilk derlemede Gradle/AGP/Kotlin bağımlılıkları
indirilir).

```bash
# Testler
./gradlew.bat testDebugUnitTest

# Hata ayıklama APK'sı
./gradlew.bat assembleDebug

# Yayın (release) APK'sı — imzalanmamış, minify + kaynak küçültme açık
./gradlew.bat assembleRelease

# Play Store'a yüklenecek App Bundle
./gradlew.bat bundleRelease
```

`local.properties` içine `UPLOAD_STORE_FILE`, `UPLOAD_STORE_PASSWORD`, `UPLOAD_KEY_ALIAS`,
`UPLOAD_KEY_PASSWORD` değerleri girilirse `assembleRelease`/`bundleRelease` çıktısı bu
anahtarla imzalanır; girilmezse imzasız üretilir (Play Console'a yüklemeden önce
imzalanması gerekir, ya da Play App Signing kullanılabilir).

## Play Store süreci notu

1. `bundleRelease` ile üretilen `app/build/outputs/bundle/release/app-release.aab` dosyası
   Play Console'a yüklenir.
2. Uygulama internet izni istemediği ve hiçbir veri toplamadığı için Veri Güvenliği
   (Data Safety) formunda "Veri toplanmıyor" seçilebilir.
3. minSdk 26 (Android 8.0) ve üzerini hedefler.
4. İlk yüklemede kapsamlı bir gizlilik politikasına ihtiyaç yoktur (uygulama hiçbir veri
   toplamaz/göndermez); yine de Play Console mağaza kaydı bir gizlilik politikası URL'si
   isteyebilir.

## Proje yapısı

- `core/Degerlendirici.kt` — saf ifade değerlendirici (`BigDecimal`, işlem önceliği).
- `core/HesapDurumu.kt` — değişmez UI durumu + reducer (tuş → yeni durum).
- `ui/HesapEkrani.kt` — Compose ekranı (ekran, tuş takımı, geçmiş paneli).
- `ui/theme/` — Material 3 tema, renk paleti, tipografi.

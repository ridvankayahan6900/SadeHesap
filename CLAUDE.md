# SadeHesap — proje talimatları

Basit, reklamsız, izinsiz hesap makinesi (Kotlin, Jetpack Compose, Material 3). Paket
`com.ridvan.sadehesap`. Amaç: Rıdvan'ın Play Console hesabında yayın sürecini
çalıştırmak (3 Eylül 2026). HafızAI ile ilgisi yok.

- Yığın: AGP 8.13, Gradle 8.13, Kotlin 2.1.21 + compose plugin, BOM 2025.06.01,
  compileSdk/targetSdk 36, minSdk 26. JAVA_HOME Microsoft jdk-17.
- Saf mantık `core/Degerlendirici.kt` + `core/HesapDurumu.kt` (57 birim test). UI
  `ui/HesapEkrani.kt`. Tüm metinler strings.xml (tr varsayılan, en, fr).
- Release imzası: `local.properties` (git dışı) `UPLOAD_*` değerleri → yükleme anahtarı
  `C:/Users/ridva/keystores/hesapmakinesi-upload.jks` (alias `upload`). Play App Signing
  kullanılacak; bu yalnız YÜKLEME anahtarıdır. Parolayı asla yazdırma.
- Play varlıkları ve metinleri `play/` klasöründe (`play/README.md` hangi alana gireceğini
  anlatır). Gizlilik politikası GitHub Pages'te yayında.
- "Bitti" demeden `./gradlew.bat testDebugUnitTest bundleRelease` yeşil olmalı.

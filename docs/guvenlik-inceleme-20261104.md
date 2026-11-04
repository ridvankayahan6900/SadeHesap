# SadeHesap — Güvenlik ve Gizlilik İncelemesi (4 Kasım 2026)

**Kapsam:** `main` dalı, commit `21d5f81` ve tüm git geçmişi (5 commit, tek yazar).
**Yöntem:** Kaynak kodun, `AndroidManifest.xml`'in, Gradle yapılandırmasının, `play/`
klasörünün ve tüm commit diff'lerinin elle ve `grep` ile taranması. Hiçbir kod
değiştirilmedi, derleme çalıştırılmadı, dış servise (CVE veritabanı dahil) istek atılmadı.

## Özet

SadeHesap'ın saldırı yüzeyi çok küçük: izin yok, ağ kodu yok, WebView yok, kalıcı depolama
yok, üçüncü taraf SDK yok. **Kritik veya Yüksek önem derecesinde bulgu yok.** Bulguların
hepsi sağlamlaştırma (hardening) ve süreç önerisi niteliğinde.

| # | Bulgu | Önem |
|---|-------|------|
| 1 | Gradle wrapper dağıtımı için SHA-256 doğrulaması yok | Orta |
| 2 | Bağımlılık doğrulaması (`verification-metadata.xml`) yok | Düşük |
| 3 | Sayı girişinde uzunluk/basamak sınırı yok | Düşük |
| 4 | Sonuç büyüklüğü (üs) sınırsız; `toPlainString()` çok uzun metin üretebilir | Düşük |
| 5 | `HesapDurumuSaver.restore` bozuk girdide çökebilir | Düşük |
| 6 | Yükleme anahtarının yerel yolu ve alias'ı depoda yazılı | Düşük |
| 7 | `dataExtractionRules` tanımlı değil (Android 12+) | Düşük (bilgi) |
| 8 | Panoya kopyalanan sonuç Android 13+ önizlemesinde görünür | Düşük (bilgi) |

## Temiz çıkan kontroller

- **Sabit kodlanmış sır / API anahtarı:** Kaynakta ve tüm git geçmişinde parola, API
  anahtarı, özel anahtar, `.jks`/`.keystore`, `local.properties`, `google-services.json`
  veya `.env` dosyası **yok**. İmza parolaları yalnızca git dışı `local.properties`'ten
  okunuyor (`app/build.gradle.kts:10-20`, `:39-58`); `.gitignore:1` bu dosyayı ve
  `*.aab`/`*.apk` çıktılarını hariç tutuyor.
- **Güvensiz ağ çağrısı:** Ağ kodu yok, `INTERNET` izni yok, `http://` bağlantısı yok
  (yalnızca XML ad alanı URI'leri).
- **Dışa açık bileşenler:** Tek bileşen `MainActivity`, `exported="true"` yalnızca
  `MAIN/LAUNCHER` filtresi için (`AndroidManifest.xml:14-21`) — zorunlu ve doğru.
  Activity hiçbir `Intent` verisi okumuyor (`MainActivity.kt`). `ui-test-manifest`
  yalnızca `debugImplementation` olduğu için yayın sürümüne dışa açık test activity'si
  girmez.
- **WebView / JS köprüsü:** Yok.
- **Kişisel verinin güvensiz saklanması:** `SharedPreferences`, dosya, veritabanı yok.
  Durum yalnızca `rememberSaveable` ile Bundle'da tutuluyor (`ui/HesapEkrani.kt:104`).
  `android:allowBackup="false"` (`AndroidManifest.xml:5`).
- **Firebase/Firestore kuralları:** Firebase kullanılmıyor.
- **Kişisel verinin loglanması:** `Log.*`, `println` çağrısı yok.
- **Gereksiz izinler:** Manifest hiçbir izin istemiyor.
- **Yayın derleme ayarları:** `isMinifyEnabled = true`, `isShrinkResources = true`
  (`app/build.gradle.kts:62-63`); `debuggable` açıkça ayarlanmamış, yani release'te
  varsayılan `false`. ProGuard kuralı gerekmiyor (`app/proguard-rules.pro`).
- **Bağımlılıklar:** Yalnızca AndroidX/Compose (BOM 2025.06.01, core-ktx 1.15.0,
  lifecycle 2.8.7, activity-compose 1.9.3) ve test için junit 4.13.2 (CVE-2020-15250
  düzeltmesini içerir). Bilinen ve bu uygulamayı etkileyen bir CVE'ye rastlanmadı.
  **Not:** Dış API çağrısı yasak olduğu için OSV/NVD sorgulanmadı; bu değerlendirme
  çevrimdışı bilgiye dayanıyor (bkz. Bulgu 2).
- **Gizlilik politikası / Veri güvenliği formu:** `play/privacy.html` ve
  `play/formlar.md` ("veri toplanmıyor", "izin yok", "internet yok") kodla tutarlı.

## Bulgular

### 1. Gradle wrapper dağıtımı için SHA-256 doğrulaması yok — **Orta**

- **Dosya:** `gradle/wrapper/gradle-wrapper.properties:3`
- **Sorun:** `distributionUrl` var ama `distributionSha256Sum` satırı yok. Ayrıca depoda
  ikili `gradle/wrapper/gradle-wrapper.jar` bulunuyor ve CI'da wrapper doğrulaması
  (ör. `gradle/actions/wrapper-validation`) yok.
- **Neden önemli:** Derleme zinciri imza anahtarına erişiyor. Değiştirilmiş bir wrapper
  JAR'ı veya araya girilmiş bir Gradle dağıtımı `local.properties` içindeki yükleme
  anahtarı parolalarını okuyabilir ya da AAB'ye kötü amaçlı kod ekleyebilir (tedarik
  zinciri riski).
- **Öneri:** `gradle-wrapper.properties`'e Gradle 8.13'ün resmî
  `distributionSha256Sum` değerini ekleyin (`./gradlew wrapper --gradle-version 8.13
  --gradle-distribution-sha256-sum <değer>`); `gradle-wrapper.jar`'ın SHA-256'sını
  gradle.org'daki resmî değerle bir kez karşılaştırın; GitHub Actions eklenirse wrapper
  doğrulama adımını koyun.

### 2. Bağımlılık doğrulaması yok — **Düşük**

- **Dosya:** `gradle/verification-metadata.xml` yok; `settings.gradle.kts:1-15`.
- **Sorun:** Bağımlılıklar `google()`/`mavenCentral()`'dan sağlama toplamı doğrulaması
  olmadan indiriliyor; otomatik bağımlılık/CVE taraması (Dependabot vb.) yok.
- **Neden önemli:** Bağımlılık listesi kısa ve güvenilir kaynaklardan olduğu için risk
  düşük, ama tehlikeye girmiş bir yapıt fark edilmeden derlemeye girebilir; ileride
  eklenecek bağımlılıklarda bilinen açıklar gözden kaçabilir.
- **Öneri:** `./gradlew --write-verification-metadata sha256 help` ile doğrulama
  dosyası üretip commit'leyin; depoda `.github/dependabot.yml` (gradle ekosistemi)
  etkinleştirin.

### 3. Sayı girişinde uzunluk sınırı yok — **Düşük**

- **Dosya:** `app/src/main/kotlin/com/ridvan/sadehesap/core/HesapDurumu.kt:75-87`
  (`rakamGir`); ayrıca `operatorGir` (`:101-119`) token sayısını sınırlamıyor.
- **Sorun:** Rakamlar sınırsız biçimde token'a ekleniyor, ifade de sınırsız uzayabiliyor.
  Testler 20 haneli girişi bilerek kabul ediyor
  (`SinirSenaryolariTest.kt:106-109`), ama üst sınır yok.
- **Neden önemli:** Güvenlik açığı değil, sağlamlık sorunu: çok uzun girişte arayüz
  yavaşlar; `rememberSaveable` durumu tek bir String olarak Bundle'a yazdığı için
  (`ui/HesapEkrani.kt:73-82`) aşırı büyük durum, arka plana geçişte
  `TransactionTooLargeException` ile çökmeye yol açabilir.
- **Öneri:** Sayı başına makul bir basamak sınırı (ör. 20, `ISLEM_HASSASIYETI` ile
  uyumlu) ve ifade başına token sınırı (ör. 100) koyun; sınırda tuşu yok sayın veya
  kısa bir uyarı gösterin. Sınır için birim testi ekleyin.

### 4. Sonuç üssü sınırsız; düz metin çok uzun olabilir — **Düşük**

- **Dosya:** `app/src/main/kotlin/com/ridvan/sadehesap/core/Degerlendirici.kt:21`,
  `:47-50`, `:98-104`
- **Sorun:** `MathContext(20)` hassasiyeti sınırlıyor ama üssü (scale) sınırlamıyor.
  Sonuç üzerinden tekrar tekrar ×/÷ yapılırsa (ör. sürekli `÷ 99999999999999999999`)
  değer 1E-1000 gibi değerlere iner; `sadeMetne` içindeki `toPlainString()` bunu binlerce
  karakterlik metne çevirir, metin geçmişe ve Bundle'a da yazılır.
- **Neden önemli:** Kullanıcı kendi cihazında arayüz donması/çökme yaşayabilir (yerel
  kendi kendine DoS; dışarıdan tetiklenemez).
- **Öneri:** `degerlendir` sonunda mutlak değer için üst/alt sınır kontrolü ekleyin
  (ör. |x| > 1E100 veya 0 < |x| < 1E-100 ise `Hata` ya da bilimsel gösterim); bu
  durum için birim testi yazın.

### 5. `HesapDurumuSaver.restore` bozuk girdide çökebilir — **Düşük**

- **Dosya:** `app/src/main/kotlin/com/ridvan/sadehesap/ui/HesapEkrani.kt:83-97`
- **Sorun:** `alanlar[1]`…`alanlar[4]` indeksleri, bölme sonucunun 5 alan içerdiği
  kontrol edilmeden okunuyor; token'lar ve geçmiş kayıtları sayı/operatör olarak
  doğrulanmıyor.
- **Neden önemli:** Bundle'ı yalnızca uygulamanın kendisi yazdığı için sömürülebilir
  değil; ancak ileride biçim değişirse (yeni alan eklenirse) eski kayıtlı durumdan geri
  dönüşte `IndexOutOfBoundsException` ile açılışta çökme olabilir.
- **Öneri:** `if (alanlar.size != 5) return HesapDurumu()` gibi bir koruma ve
  `runCatching { … }.getOrElse { HesapDurumu() }` sarmalayıcı ekleyin; geri yüklenen
  token'ları izin verilen karakter kümesine (`0-9 . - + − × ÷`) göre süzün.

### 6. Yükleme anahtarının yolu ve alias'ı depoda yazılı — **Düşük**

- **Dosya:** `CLAUDE.md:11-12`
- **Sorun:** Yükleme anahtar deposunun yerel dosya yolu (Windows kullanıcı adı dahil)
  ve anahtar alias'ı depoya commit'lenmiş. **Parola veya anahtar dosyasının kendisi
  depoda yok.**
- **Neden önemli:** Tek başına sömürülemez, ama depo herkese açıksa saldırgana hedef
  dosyanın tam konumunu ve kullanıcı adını verir (keşif bilgisi). Play App Signing
  kullanıldığı için yükleme anahtarı sızsa bile Google'dan sıfırlatılabilir; bu riski
  azaltıyor.
- **Öneri:** Depo herkese açıksa yolu genel bir ifadeyle değiştirin (ör. "kullanıcı
  klasöründeki keystores dizini"). Anahtar deposunun yedeğini parola yöneticisinde
  şifreli tutun; Play Console'da yükleme anahtarı sıfırlama sürecini not edin.

### 7. `dataExtractionRules` tanımlı değil — **Düşük (bilgi)**

- **Dosya:** `app/src/main/AndroidManifest.xml:4-11`
- **Sorun:** `allowBackup="false"` var, fakat Android 12+ için `android:dataExtractionRules`
  ve eski sürümler için `fullBackupContent` tanımlı değil. Android 12+'da
  `allowBackup="false"` bulut yedeklemeyi kapatır ama cihazdan cihaza aktarım için
  ayrı kurallar önerilir.
- **Neden önemli:** Uygulama şu anda kalıcı veri yazmadığı için pratik etkisi yok;
  ileride kalıcı geçmiş eklenirse bu ayar önem kazanır.
- **Öneri:** `res/xml/data_extraction_rules.xml` ile `<cloud-backup>` ve
  `<device-transfer>` altında tüm alanları `exclude` eden kurallar ekleyip manifestte
  referans verin.

### 8. Panoya kopyalanan sonuç sistem önizlemesinde görünür — **Düşük (bilgi)**

- **Dosya:** `app/src/main/kotlin/com/ridvan/sadehesap/ui/HesapEkrani.kt:148-152`
- **Sorun:** Uzun basışla sonuç panoya kopyalanıyor; Android 13+ kopyalanan içeriği
  ekranda önizler ve pano diğer uygulamalarca (odaktayken) okunabilir.
- **Neden önemli:** Hesap sonuçları genellikle hassas değil ve kopyalama kullanıcının
  açık eylemi; yine de maaş, borç gibi tutarlar başkalarınca görülebilir.
- **Öneri:** Değişiklik zorunlu değil. İstenirse `ClipDescription.EXTRA_IS_SENSITIVE`
  (API 33+) bayrağıyla önizleme gizlenebilir; mevcut snackbar bildirimi korunmalı.

## Sonuç

Uygulama "izinsiz, ağsız, veri toplamayan" iddiasını kod düzeyinde karşılıyor; Play
Veri Güvenliği beyanı ve gizlilik politikası doğru. Yayın öncesi en anlamlı iyileştirme
Bulgu 1 (wrapper doğrulaması); 3–5 numaralı bulgular sağlamlık için küçük düzeltmeler.

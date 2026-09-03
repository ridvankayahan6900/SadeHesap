# play/ klasörü — Play Console yayın malzemeleri

Bu klasör, Sade Hesap Makinesi (`com.ridvan.sadehesap`) uygulamasının Google Play
Console'a girilecek tüm metinlerini içerir. Aşağıda her dosyanın Play Console'da hangi
alana karşılık geldiği listelenmiştir.

## Dosyalar ve karşılık geldikleri Play Console alanları

| Dosya | Play Console konumu | Açıklama |
|---|---|---|
| `listing-tr.md` | Store presence → Main store listing → **Turkish (varsayılan dil)** | Uygulama adı, kısa/tam açıklama, kategori, iletişim e-postası |
| `listing-en.md` | Store presence → Main store listing → **Add translation → English** | Aynı alanların İngilizce çevirisi |
| `listing-fr.md` | Store presence → Main store listing → **Add translation → French** | Aynı alanların Fransızca çevirisi |
| `privacy.html` | Herhangi bir statik barındırmaya (ör. GitHub Pages) yüklenip elde edilen URL, **App content → Privacy policy** alanına ve Play Console hesap ayarlarındaki gizlilik politikası URL'sine girilir | Tek sayfa, TR/EN/FR üç dilli gizlilik politikası |
| `release-notes.md` | Play Console'da yeni bir sürüm (release) oluştururken **"What's new in this release"** kutusuna, ilgili dil sekmesine kopyalanır | 1.0.0 sürümü için TR/EN/FR sürüm notları (her biri ≤500 karakter) |
| `formlar.md` | **App content** bölümündeki anketler: Content rating (IARC), Data safety, Target audience and content, Ads, Government apps, Financial features, Health apps, News apps, COVID-19 contact tracing | Her form için hazır cevap ve gerekçe |
| `README.md` | — (bu dosya) | Klasör içeriğinin haritası |

## Kullanım sırası (önerilen)

1. **Store presence → Main store listing**: `listing-tr.md` içeriğini varsayılan dil
   olarak gir, sonra `listing-en.md` ve `listing-fr.md` içeriklerini "Add translation"
   ile ekle. Grafik varlıkları (ikon, öne çıkan görsel, ekran görüntüleri) bu klasörün
   kapsamı dışındadır.
2. **Gizlilik politikası barındırma**: `privacy.html` dosyasını bir statik barındırma
   hizmetine (ör. `ridvankayahan6900/HafizAI` deposundakine benzer şekilde GitHub Pages)
   yükleyip aldığın URL'yi hem **App content → Privacy policy** hem de Play Console
   hesap profilindeki gizlilik politikası alanına gir.
3. **App content** formlarını `formlar.md`'deki cevaplarla doldur (Content rating, Data
   safety, Target audience, Ads, Government/Financial/Health/News/COVID beyanları).
4. **İlk sürümü yayımlarken** `release-notes.md`'deki üç dildeki metni ilgili sürüm
   notu alanlarına yapıştır.
5. İletişim e-postası her yerde **rdvankayahan@gmail.com**.

## Notlar

- Tüm metinler Play politikasına uygun şekilde yazıldı: abartılı/karşılaştırmalı iddia
  yok ("en iyi", "#1" vb. kullanılmadı).
- `privacy.html` harici hiçbir kaynak (font, script, CDN) çağırmaz; tek dosya olarak
  taşınabilir ve herhangi bir statik barındırmada çalışır.
- Karakter sınırları (`≤30`, `≤80`, `≤4000`, `≤500`) her dilde ayrıca doğrulandı.

## Görsel varlıklar (bu klasörde)

| Dosya | Play Console alanı | Şart |
|---|---|---|
| `icon-512.png` | Mağaza kaydı → Uygulama simgesi | 512×512, 32-bit PNG (alfa) |
| `feature-1024x500.png` | Mağaza kaydı → Öne çıkan görsel | 1024×500, 24-bit PNG |
| `screenshot-1.png` … `screenshot-3.png` | Mağaza kaydı → Telefon ekran görüntüleri (aynıları 7" tablet sekmesine de yüklenebilir) | 1200×1920, oran 10:16 |

Gizlilik politikası (Uygulama içeriği → Gizlilik politikası ve Veri güvenliği formu):
https://ridvankayahan6900.github.io/SadeHesap/play/privacy.html

## Yayın yolu (2026)

1. Hesap 13 Kasım 2023'ten SONRA açılmış kişisel hesapsa: önce **Kapalı test** kanalına
   sürüm yükle, en az 12 test kullanıcısı kesintisiz 14 gün opt-in kalsın, sonra
   "Üretime erişim için başvur". Daha eski ya da kurumsal hesapta bu adım yok.
2. Dahili test → (kapalı test) → Üretim. Her sürümde `app/build/outputs/bundle/release/app-release.aab`.

## Durum — 3 Eylül 2026 gece (Play Console, Pixelrift Studio)

- Uygulama oluşturuldu: App ID `4975792809957831086`. Mağaza kaydı TR/EN/FR, tüm "Uygulama içeriği"
  beyanları, kategori Tools, iletişim e-postası kaydedildi. 17 değişiklik tek pakette Google
  incelemesinde ("In review").
- **Dahili test**: sürüm 1 (1.0.0) canlı. Katılım: https://play.google.com/apps/internaltest/4701590146032132579
- **Kapalı test (Alpha)**: sürüm 1 (1.0.0) incelemede, 177 ülke. Katılım:
  https://play.google.com/apps/testing/com.ridvan.sadehesap
- **Üretim**: "Apply for production" pasif — kapalı testte en az 12 test kullanıcısı 14 gün
  kesintisiz kalmalı (şu an 0/12; liste "İç test" yalnız rdvankayahan@gmail.com).
- Eksik/isteğe bağlı: mağaza etiketleri (Store settings → Manage tags) elle eklenebilir;
  10 inç tablet görselleri boş.

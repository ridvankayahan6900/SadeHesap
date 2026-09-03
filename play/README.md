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

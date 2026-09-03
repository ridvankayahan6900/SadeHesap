# Play Console — Uygulama içeriği formları (hazır cevaplar)

Bu dosya, Google Play Console'daki "Uygulama içeriği" (App content) bölümündeki
anketlerin her biri için Sade Hesap Makinesi'ne uygulanacak cevapları içerir.
Uygulama izin istemez, internete bağlanmaz, veri toplamaz ve reklam göstermez —
aşağıdaki tüm cevaplar bu üç gerçeğe dayanır.

## 1. İçerik derecelendirme anketi (IARC)

Anket akışı Play Console tarafından güncellenebildiği için soru sırası değişebilir;
aşağıdaki cevaplar anketin özü değişmediği sürece geçerlidir.

- **Uygulama kategorisi**: Yardımcı program / Üretkenlik / İletişim veya Diğer
  (Utility, Productivity, Communication, or Other) — hesap makinesi bu kategoriye girer.
- **Şiddet**: Yok → "Hayır"
- **Cinsel içerik / çıplaklık**: Yok → "Hayır"
- **Küfür / müstehcen dil**: Yok → "Hayır"
- **Uyuşturucu, alkol, tütün referansı**: Yok → "Hayır"
- **Kumar (simüle edilmiş dahil)**: Yok → "Hayır"
- **Korku unsurları**: Yok → "Hayır"
- **Kullanıcı tarafından oluşturulan içerik / kullanıcılar arası etkileşim**: Yok
  (uygulamada sohbet, paylaşım veya çok kullanıcılı özellik yok) → "Hayır"
- **Konum paylaşımı**: Yok → "Hayır"
- **Kişisel bilgi paylaşımı**: Yok → "Hayır"
- **Diğer kullanıcılarla etkileşim / dijital satın alma**: Yok → "Hayır"

**Beklenen sonuç**: Tüm sistemlerde en düşük derecelendirme —
ESRB: Everyone, PEGI: 3, USK: 0, ClassInd: Livre, IARC genel: "Herkes" (3+).
Anket bu cevaplarla otomatik hesaplanır; Play Console'un ürettiği son etiketi kontrol edin.

## 2. Veri güvenliği formu (Data safety)

- **"Uygulamanız kullanıcı verisi topluyor mu veya paylaşıyor mu?"** → **Hayır, veri
  toplanmıyor** ("No data collected").
- Bu seçildiğinde Play Console, veri kategorisi bazlı (konum, kişisel bilgiler, mesajlar
  vb.) alt soruları göstermez.
- **"Verileriniz aktarım sırasında şifreleniyor mu?"** → Görünmez / uygulanamaz, çünkü
  hiçbir veri cihaz dışına gönderilmiyor (uygulamanın internet bağlantısı yok).
- **"Kullanıcılar veri silinmesini talep edebiliyor mu?"** → Görünmez / uygulanamaz;
  hesap geçmişi zaten kalıcı olarak saklanmıyor (yalnızca bellekte, uygulama kapanınca
  otomatik silinir), silinecek bir sunucu verisi yok.
- **Güvenlik uygulamaları bölümü**: "Bağımsız bir güvenlik incelemesinden geçti mi?"
  gibi isteğe bağlı sorular boş bırakılabilir (uygulanamaz); zorunlu değildir.

## 3. Hedef kitle ve içerik (Target audience and content)

- **Hedef yaş aralıkları**: Tüm yaş gruplarını işaretleyin (13 yaş altı dahil tüm
  aralıklar seçilebilir) — hesap makinesi içeriği yaş açısından nötrdür, herhangi bir
  yaş grubu için sakıncalı unsur içermez.
- **"Uygulamanız birincil olarak çocuklara mı yöneliktir?"** → **Hayır.**
  **Gerekçe**: Uygulama genel amaçlı bir araçtır (hesap makinesi); çocuklara özgü
  karakterler, oyunlaştırma, ödül sistemi veya çocuk odaklı pazarlama içermez. Bu
  nedenle "Designed for Families" programına dahil edilmemeli, "çocuklara yönelik
  değil, ancak tüm yaşlar için uygun" olarak işaretlenmelidir.
- Bu seçim, uygulamanın Ailelere Yönelik Politika (Families Policy) kapsamındaki ek
  yükümlülüklerden (ör. reklam kısıtlamaları, COPPA'ya özgü bildirimler) muaf
  tutulmasını sağlar; zaten reklam ve veri toplama olmadığı için bu yükümlülükler
  fiilen ilgisizdir.

## 4. Reklamlar (Ads)

- **"Uygulamanız reklam içeriyor mu?"** → **Hayır.**

## 5. Devlet uygulaması (Government app)

- **"Bu bir devlet uygulaması mı?"** → **Hayır.**

## 6. Finansal özellikler beyanı (Financial features)

- **"Uygulamanız finansal hizmetler sunuyor mu?"** → **Hayır**
  (borç verme, ödeme işleme, kripto para, sigorta vb. yok; basit bir hesap makinesi
  finansal işlem yapmaz, yalnızca matematiksel işlem yapar).

## 7. Sağlık uygulaması (Health)

- **"Uygulamanız sağlıkla ilgili içerik/işlev içeriyor mu?"** → **Hayır.**

## 8. Haber uygulaması (News apps)

- **"Uygulamanız bir haber uygulaması mı?"** → **Hayır.**

## 9. COVID-19 izleme uygulaması

- **"Uygulamanız COVID-19 ile ilgili mi?"** → **Hayır.**

## 10. Erişilebilirlik beyanı (isteğe bağlı, öneri)

Play Console'da zorunlu bir form olmasa da mağaza sayfası incelemesinde faydalıdır:
uygulama her tuş için `contentDescription` sağlar, minimum 48dp dokunma hedefi
kullanır ve ifade/sonuç alanları için semantics tanımlıdır — bu nedenle
"erişilebilirlik özellikleri içerir" notu eklenebilir.

## Gizlilik politikası URL / Privacy policy URL
https://ridvankayahan6900.github.io/SadeHesap/play/privacy.html

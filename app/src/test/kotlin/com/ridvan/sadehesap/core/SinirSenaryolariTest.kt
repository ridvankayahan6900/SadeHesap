package com.ridvan.sadehesap.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aritmetik doğruluk ve durum makinesi için ek sınır (edge case) senaryoları.
 * Görev kapsamında istenen liste: 0.1+0.2, 1÷3×3, 10%×2, −5±, 5÷0, 1e15 girdileri,
 * 20 basamak, ardışık "==", "5+=" gibi eksik operand, ⌫ ile operatör silme,
 * ondalik iki kez basma, "0" başında sıfır yığılması, sonuç sonrası operatör.
 */
class SinirSenaryolariTest {

    private fun basarili(tokenlar: List<String>): BigDecimal {
        val sonuc = Degerlendirici.degerlendir(tokenlar)
        assertTrue("Beklenen: Basarili, gelen: $sonuc", sonuc is DegerlendirmeSonucu.Basarili)
        return (sonuc as DegerlendirmeSonucu.Basarili).deger
    }

    private fun rakamlar(metin: String, durum: HesapDurumu = HesapDurumu()): HesapDurumu =
        metin.fold(durum) { d, c -> d.uygula(HesapTusu.Rakam(c)) }

    // --- 0.1 + 0.2 : BigDecimal ondalık string'ten kurulduğu için ikili (binary) float
    // sapması olmamalı; IEEE-754 double ile hesaplansaydı 0.30000000000000004 çıkardı. ---
    @Test
    fun `0-1 artı 0-2 tam olarak 0-3 verir`() {
        val deger = basarili(listOf("0.1", "+", "0.2"))
        assertEquals("0.3", Degerlendirici.sadeMetne(deger))
    }

    // --- 1 ÷ 3 × 3 : ara sonuç 20 basamak hassasiyetle 0.333...3, ×3 sonrası
    // 0.999...9 (20 dokuz) çıkar; sadeMetne 12 anlamlı basamağa yuvarlarken bu
    // yukarı yuvarlanıp "1" olmalı (aksi halde kullanıcı "0.999999999999" görür). ---
    @Test
    fun `1 bolu 3 carpi 3 goruntude 1 e yuvarlanir`() {
        val deger = basarili(listOf("1", "÷", "3", "×", "3"))
        assertEquals("1", Degerlendirici.sadeMetne(deger))
    }

    // --- 10 % × 2 : HesapDurumu üzerinden "10", "%", "×", "2", "=" tuş dizisi. % anında
    // uygulanır (10 -> 0.1), sonra 0.1 × 2 = 0.2. ---
    @Test
    fun `10 yuzde carpi 2 tus dizisi 0-2 verir`() {
        var durum = rakamlar("10")
        durum = durum.uygula(HesapTusu.Yuzde)
        assertEquals(listOf("0.1"), durum.tokenlar)
        durum = durum.uygula(HesapTusu.Carp)
        durum = rakamlar("2", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals("0.2", durum.sonSonuc)
    }

    // --- −5 ± : 5 girilip işaret değiştirilir, sonra tekrar ± ile geri alınır
    // (mevcut DegerlendiriciTest'te ayrı bir varyantı var; burada tazeden boş
    // durumda ± ve operatör hemen ardından ± senaryosu ele alınıyor). ---
    @Test
    fun `bos durumda arti eksi hicbir sey yapmaz`() {
        val durum = HesapDurumu().uygula(HesapTusu.ArtiEksi)
        assertEquals(listOf("0"), durum.tokenlar)
    }

    @Test
    fun `operatorden hemen sonra arti eksi yok sayilir`() {
        // 5 + basildiktan sonra ikinci sayi icin negatif isareti baslatma beklenebilir,
        // ama mevcut davranis operatoru degistirmeden hicbir sey yapmamak.
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.Topla)
        val once = durum.tokenlar
        durum = durum.uygula(HesapTusu.ArtiEksi)
        assertEquals(once, durum.tokenlar)
    }

    // --- 5 ÷ 0 : hem Degerlendirici hem HesapDurumu seviyesinde (mevcut testlerin
    // tekrarı değil, 0 ÷ 0 varyantı) ---
    @Test
    fun `0 bolu 0 da hata dondurur`() {
        val sonuc = Degerlendirici.degerlendir(listOf("0", "÷", "0"))
        assertEquals(DegerlendirmeSonucu.Hata, sonuc)
    }

    // --- 1e15 girdileri : 16 haneli "1000000000000000" tam olarak yazilabilmeli,
    // kirpilmemeli. ---
    @Test
    fun `1e15 buyuklugunde sayi tam yazilir`() {
        val binIkiuzeronbes = "1000000000000000" // 1e15, 16 hane
        val durum = rakamlar(binIkiuzeronbes)
        assertEquals(listOf(binIkiuzeronbes), durum.tokenlar)
    }

    @Test
    fun `1e15 artı 1 dogru toplanir ve 12 basamaga yuvarlanarak gosterilir`() {
        // Gercek toplam 1000000000000001 (16 hane) ama sadeMetne en fazla 12 anlamli
        // basamak tutar (tasarim geregi GOSTERIM_BASAMAK=12); bu yuzden goruntude
        // 1'in kaybolup 1000000000000000 gorunmesi TASARIM GEREGI beklenen davranis.
        val deger = basarili(listOf("1000000000000000", "+", "1"))
        assertEquals(BigDecimal("1000000000000001"), deger)
        assertEquals("1000000000000000", Degerlendirici.sadeMetne(deger))
    }

    // --- 20 basamak : tam 20 haneli bir sayi kesintisiz girilebilmeli, degerlendirilebilmeli
    // ve sadeMetne cikisi en fazla 12 anlamli basamak icermeli. ---
    @Test
    fun `20 haneli sayi tam girilir`() {
        val yirmiHane = "12345678901234567890"
        val durum = rakamlar(yirmiHane)
        assertEquals(listOf(yirmiHane), durum.tokenlar)
        assertEquals(20, durum.tokenlar[0].length)
    }

    @Test
    fun `20 haneli sayi tek basina esittir ile hata vermez ve 12 basamaga yuvarlanir`() {
        // NOT: sadeMetne tam sayilarda 12 anlamli basamaktan sonrasini "0" ile doldurup
        // (bilimsel gosterim kullanmadan) duz metin uretir; bu yuzden cikan dizenin
        // UZUNLUGU 20 kalir ama gercek/anlamli basamak sayisi 12'dir (kalan 8 hane
        // sadece basamak degeri korumak icin konan sifirlardir, ozgun rakamlar
        // DEGILDIR). Asagidaki bulgu bunu ayrica raporluyor: kullaniciya bu ayrimi
        // gosteren hicbir isaret (yuvarlama simgesi, bilimsel gosterim vb.) yok.
        val yirmiHane = "12345678901234567890"
        var durum = rakamlar(yirmiHane)
        durum = durum.uygula(HesapTusu.Esittir)
        assertFalse("Hata vermemeli", durum.hataVarMi)
        assertEquals("12345678901200000000", durum.sonSonuc)
    }

    @Test
    fun `20 haneli iki sayinin carpimi hata firlatmadan hesaplanir`() {
        // 20 basamaklik MathContext ile carpim tasmadan (exception firlatmadan) sonuclanmali.
        val sonuc = Degerlendirici.degerlendir(
            listOf("12345678901234567890", "×", "98765432109876543210")
        )
        assertTrue("Beklenen: Basarili, gelen: $sonuc", sonuc is DegerlendirmeSonucu.Basarili)
    }

    // --- ardışık "==" : ikinci esittir basimi, birinci sonucu degistirmemeli ve tokenlar/
    // gecmis dahil TUM durumu degistirmeden (no-op) birakmali (bkz. HesapDurumu.esitle
    // duzeltmesi: esittirSonrasi iken "=" artik hicbir sey yapmiyor). ---
    @Test
    fun `ardisik esittir sonucu degistirmez`() {
        var durum = rakamlar("2")
        durum = durum.uygula(HesapTusu.Topla)
        durum = rakamlar("3", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals("5", durum.sonSonuc)

        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals("5", durum.sonSonuc)
        assertEquals(listOf("5"), durum.tokenlar)
    }

    @Test
    fun `ardisik esittir gecmise yinelenen kayit eklemez`() {
        // DUZELTME: "2+3=" bir kere gecmise "2+3 -> 5" yazar; YENI bir hesap yapmayan
        // ikinci (ucuncu, ...) "=" basimi artik gecmise anlamsiz "5 -> 5" kaydi EKLEMEZ
        // (eskiden ekliyordu — bkz. inceleme bulgusu, HesapDurumu.esitle duzeltildi).
        var durum = rakamlar("2")
        durum = durum.uygula(HesapTusu.Topla)
        durum = rakamlar("3", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals(1, durum.gecmis.size)
        assertEquals(GecmisKaydi("2+3", "5"), durum.gecmis.first())

        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals(1, durum.gecmis.size)
        assertEquals(GecmisKaydi("2+3", "5"), durum.gecmis.first())

        // Uc, dort kez ust uste basmak da ayni sekilde no-op olmali.
        durum = durum.uygula(HesapTusu.Esittir)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals(1, durum.gecmis.size)
    }

    // --- "5+=" gibi eksik operand : mevcut testte tek varyant var (Topla/+), burada
    // Bol (÷) ile ayrica dogrulaniyor; ayrica esittir sonrasi tekrar rakam girisiyle
    // toparlanabildigi kontrol ediliyor. ---
    @Test
    fun `5 bolu esittir eksik operandda yok sayilir`() {
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.Bol)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals(listOf("5", "÷"), durum.tokenlar)
        assertFalse(durum.hataVarMi)
        assertFalse(durum.esittirSonrasi)

        // Toparlanma: ikinci sayi girilip esittir basilinca normal sonuc gelmeli.
        durum = rakamlar("2", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals("2.5", durum.sonSonuc)
    }

    // --- ⌫ ile operatör silme : coklu operatorlu bir ifadede zincirleme geri silme,
    // her adimda token listesinin tutarli kalmasi. ---
    @Test
    fun `coklu operatorlu ifadede zincirleme geri silme dogru calisir`() {
        var durum = rakamlar("12")
        durum = durum.uygula(HesapTusu.Topla)
        durum = rakamlar("34", durum)
        durum = durum.uygula(HesapTusu.Carp)
        assertEquals(listOf("12", "+", "34", "×"), durum.tokenlar)

        durum = durum.uygula(HesapTusu.GeriSil) // "×" operatoru silinir
        assertEquals(listOf("12", "+", "34"), durum.tokenlar)

        durum = durum.uygula(HesapTusu.GeriSil) // "34" -> "3"
        assertEquals(listOf("12", "+", "3"), durum.tokenlar)

        durum = durum.uygula(HesapTusu.GeriSil) // "3" tek hane -> operatore doner
        assertEquals(listOf("12", "+"), durum.tokenlar)

        durum = durum.uygula(HesapTusu.GeriSil) // "+" operatoru silinir
        assertEquals(listOf("12"), durum.tokenlar)

        durum = durum.uygula(HesapTusu.GeriSil) // "12" -> "1"
        assertEquals(listOf("1"), durum.tokenlar)

        durum = durum.uygula(HesapTusu.GeriSil) // "1" tek hane -> "0"
        assertEquals(listOf("0"), durum.tokenlar)

        durum = durum.uygula(HesapTusu.GeriSil) // "0" uzerinde ekstra geri silme guvenli olmali
        assertEquals(listOf("0"), durum.tokenlar)
    }

    // --- ondalik iki kez basma : operatör sonrası "0." üretilir, ikinci basim yok sayılır;
    // negatif sayı üzerinde de aynı korumanın çalıştığı ayrıca doğrulanıyor. ---
    @Test
    fun `operator sonrasi ondalik iki kez basinca tek nokta kalir`() {
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.Topla)
        durum = durum.uygula(HesapTusu.Ondalik)
        assertEquals(listOf("5", "+", "0."), durum.tokenlar)
        durum = durum.uygula(HesapTusu.Ondalik) // yok sayilmali
        assertEquals(listOf("5", "+", "0."), durum.tokenlar)
    }

    @Test
    fun `negatif sayida ondalik iki kez basinca tek nokta kalir`() {
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.ArtiEksi) // "-5"
        durum = durum.uygula(HesapTusu.Ondalik)
        assertEquals(listOf("-5."), durum.tokenlar)
        durum = durum.uygula(HesapTusu.Ondalik) // yok sayilmali
        assertEquals(listOf("-5."), durum.tokenlar)
    }

    // --- "0" başında sıfır yığılması : hem ifadenin en basinda hem operatorden sonra
    // ust uste "0" basimi tek "0" olarak kalmali, ardindan gelen rakam onun yerine gecmeli. ---
    @Test
    fun `bastaki sifirlar yigilmadan tek sifir kalir`() {
        var durum = HesapDurumu()
        durum = durum.uygula(HesapTusu.Rakam('0'))
        durum = durum.uygula(HesapTusu.Rakam('0'))
        durum = durum.uygula(HesapTusu.Rakam('0'))
        assertEquals(listOf("0"), durum.tokenlar)
        durum = durum.uygula(HesapTusu.Rakam('5'))
        assertEquals(listOf("5"), durum.tokenlar)
    }

    @Test
    fun `operator sonrasi sifirlar yigilmadan rakamla degisir`() {
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.Topla)
        durum = durum.uygula(HesapTusu.Rakam('0'))
        durum = durum.uygula(HesapTusu.Rakam('0'))
        durum = durum.uygula(HesapTusu.Rakam('0'))
        assertEquals(listOf("5", "+", "0"), durum.tokenlar)
        durum = durum.uygula(HesapTusu.Rakam('7'))
        assertEquals(listOf("5", "+", "7"), durum.tokenlar)
    }

    // --- sonuç sonrası operatör : "=" sonrasi hemen operator basilinca sonuc uzerinden
    // devam edilir; ardindan operator degistirilirse (ardisik operator) son basilan
    // gecerli olmali. ---
    @Test
    fun `sonuc sonrasi ardisik operator son basilani kullanir`() {
        var durum = rakamlar("2")
        durum = durum.uygula(HesapTusu.Topla)
        durum = rakamlar("3", durum)
        durum = durum.uygula(HesapTusu.Esittir) // sonuc: 5

        durum = durum.uygula(HesapTusu.Topla)
        assertEquals(listOf("5", "+"), durum.tokenlar)
        durum = durum.uygula(HesapTusu.Carp) // + yerine × gecerli olmali
        assertEquals(listOf("5", "×"), durum.tokenlar)

        durum = rakamlar("4", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals("20", durum.sonSonuc)
    }

    // --- sonucYuvarlandiMi: HesapDurumu duzeyinde, Degerlendirici.yuvarlandiMi ile ayni
    // kural esitle() sonrasinda alana dogru yansimali; sonraki bir islemle (yeni rakam ya
    // da operator) sonuc ekrandan kalkinca da sifirlanmali. ---
    @Test
    fun `2+3 esittir sonrasi sonucYuvarlandiMi false olur`() {
        var durum = rakamlar("2")
        durum = durum.uygula(HesapTusu.Topla)
        durum = rakamlar("3", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals("5", durum.sonSonuc)
        assertFalse(durum.sonucYuvarlandiMi)
    }

    @Test
    fun `1 bolu 3 esittir sonrasi sonucYuvarlandiMi true olur`() {
        var durum = rakamlar("1")
        durum = durum.uygula(HesapTusu.Bol)
        durum = rakamlar("3", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertTrue(durum.sonucYuvarlandiMi)
    }

    @Test
    fun `sonucYuvarlandiMi yeni islem baslayinca sifirlanir`() {
        var durum = rakamlar("1")
        durum = durum.uygula(HesapTusu.Bol)
        durum = rakamlar("3", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertTrue(durum.sonucYuvarlandiMi)

        durum = durum.uygula(HesapTusu.Topla) // sonuc uzerinden devam: yuvarlama gostergesi artik anlamsiz
        assertFalse(durum.sonucYuvarlandiMi)
    }
}

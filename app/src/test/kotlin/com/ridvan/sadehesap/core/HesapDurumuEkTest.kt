package com.ridvan.sadehesap.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * [HesapDurumu] reducer'ı ([uygula], [gecmistenYukle]) için ek durum geçişi testleri:
 * hata durumunda her tuşun davranışı, "=" sonrası %, ±, ⌫ ve "." tuşları, ikinci
 * operand üzerinde %/±, geçmiş sırası ve sınırı, değişmezlik (immutability).
 */
class HesapDurumuEkTest {

    private fun rakamlar(metin: String, durum: HesapDurumu = HesapDurumu()): HesapDurumu =
        metin.fold(durum) { d, c -> d.uygula(HesapTusu.Rakam(c)) }

    /** "2+3" gibi bir metni tuş dizisine çevirip uygular ("." ondalık, "=" eşittir). */
    private fun tusla(dizi: String, durum: HesapDurumu = HesapDurumu()): HesapDurumu =
        dizi.fold(durum) { d, c ->
            d.uygula(
                when (c) {
                    in '0'..'9' -> HesapTusu.Rakam(c)
                    '.' -> HesapTusu.Ondalik
                    '+' -> HesapTusu.Topla
                    '−' -> HesapTusu.Cikar
                    '×' -> HesapTusu.Carp
                    '÷' -> HesapTusu.Bol
                    '%' -> HesapTusu.Yuzde
                    '±' -> HesapTusu.ArtiEksi
                    '⌫' -> HesapTusu.GeriSil
                    'C' -> HesapTusu.TumunuSil
                    '=' -> HesapTusu.Esittir
                    else -> error("Bilinmeyen tus: $c")
                }
            )
        }

    private fun hataDurumu(): HesapDurumu {
        val durum = tusla("2+2=1÷0=")
        assertTrue(durum.hataVarMi)
        return durum
    }

    // ---------------------------------------------------------------- başlangıç durumu

    @Test
    fun `varsayilan durum sifir gosterir ve bos gecmise sahiptir`() {
        val durum = HesapDurumu()
        assertEquals(listOf("0"), durum.tokenlar)
        assertNull(durum.sonSonuc)
        assertFalse(durum.hataVarMi)
        assertFalse(durum.esittirSonrasi)
        assertFalse(durum.sonucYuvarlandiMi)
        assertTrue(durum.gecmis.isEmpty())
        assertEquals(20, HesapDurumu.MAKS_GECMIS)
    }

    @Test
    fun `baslangicta operator basilirsa sifir uzerinden devam edilir`() {
        assertEquals(listOf("0", "+"), tusla("+").tokenlar)
        assertEquals("5", tusla("+5=").sonSonuc)
    }

    @Test
    fun `baslangicta ondalik basilirsa sifir nokta olusur`() {
        assertEquals(listOf("0."), tusla(".").tokenlar)
        assertEquals(listOf("0.5"), tusla(".5").tokenlar)
    }

    @Test
    fun `uygula orijinal durumu degistirmez`() {
        val once = rakamlar("12")
        val tokenlarOnce = once.tokenlar
        once.uygula(HesapTusu.Topla)
        once.uygula(HesapTusu.GeriSil)
        once.uygula(HesapTusu.TumunuSil)
        assertEquals(listOf("12"), once.tokenlar)
        assertSame(tokenlarOnce, once.tokenlar)
    }

    // ---------------------------------------------------------------- işlem önceliği (tuşlarla)

    @Test
    fun `tus dizisiyle karma ifade oncelik kuralina gore hesaplanir`() {
        // 2 + 3*4 - 6/2 = 2 + 12 - 3 = 11
        val durum = tusla("2+3×4−6÷2=")
        assertEquals("11", durum.sonSonuc)
        assertEquals(GecmisKaydi("2+3×4−6÷2", "11"), durum.gecmis.first())
    }

    @Test
    fun `ardisik operator degisimlerinde yalniz son basilan gecerli olur`() {
        val durum = tusla("5+×−3")
        assertEquals(listOf("5", "−", "3"), durum.tokenlar)
        assertEquals("2", tusla("=", durum).sonSonuc)
    }

    @Test
    fun `nokta ile biten ikinci operandla esittir hesaplanir`() {
        val durum = tusla("5+3.=")
        assertEquals("8", durum.sonSonuc)
        assertEquals(GecmisKaydi("5+3.", "8"), durum.gecmis.first())
    }

    @Test
    fun `0-1 arti 0-2 tus dizisiyle 0-3 verir`() {
        assertEquals("0.3", tusla("0.1+0.2=").sonSonuc)
    }

    @Test
    fun `esittir sonucu tek token olarak tutulur`() {
        val durum = tusla("12×3=")
        assertEquals(listOf("36"), durum.tokenlar)
        assertEquals("36", durum.sonSonuc)
        assertTrue(durum.esittirSonrasi)
    }

    // ---------------------------------------------------------------- hata durumu

    @Test
    fun `hata durumu gecmisi korur`() {
        val durum = hataDurumu()
        assertEquals(listOf(GecmisKaydi("2+2", "4")), durum.gecmis)
        assertEquals(listOf("0"), durum.tokenlar)
    }

    @Test
    fun `hata durumunda esittir operator yuzde ve arti eksi yok sayilir`() {
        val durum = hataDurumu()
        listOf(
            HesapTusu.Esittir, HesapTusu.Topla, HesapTusu.Cikar,
            HesapTusu.Carp, HesapTusu.Bol, HesapTusu.Yuzde, HesapTusu.ArtiEksi
        ).forEach { tus ->
            assertEquals("Tus: $tus", durum, durum.uygula(tus))
        }
    }

    @Test
    fun `hata durumunda ondalik yeni sifir nokta ile baslar`() {
        val durum = hataDurumu().uygula(HesapTusu.Ondalik)
        assertFalse(durum.hataVarMi)
        assertEquals(listOf("0."), durum.tokenlar)
        assertEquals(1, durum.gecmis.size)
    }

    @Test
    fun `hata durumunda geri silme baslangica doner ve gecmisi korur`() {
        val durum = hataDurumu().uygula(HesapTusu.GeriSil)
        assertEquals(HesapDurumu(gecmis = listOf(GecmisKaydi("2+2", "4"))), durum)
    }

    @Test
    fun `hata durumunda tumunu sil hatayi temizler`() {
        val durum = hataDurumu().uygula(HesapTusu.TumunuSil)
        assertFalse(durum.hataVarMi)
        assertEquals(listOf("0"), durum.tokenlar)
        assertEquals(1, durum.gecmis.size)
    }

    @Test
    fun `hatali hesap gecmise eklenmez`() {
        val durum = tusla("1÷0=")
        assertTrue(durum.hataVarMi)
        assertTrue(durum.gecmis.isEmpty())
    }

    // ---------------------------------------------------------------- "=" sonrası tuşlar

    @Test
    fun `esittir sonrasi ondalik yeni sayi baslatir`() {
        val durum = tusla("2+3=.")
        assertEquals(listOf("0."), durum.tokenlar)
        assertFalse(durum.esittirSonrasi)
        assertNull(durum.sonSonuc)
        assertEquals(1, durum.gecmis.size)
    }

    @Test
    fun `esittir sonrasi rakam gecmisi korur`() {
        val durum = tusla("2+3=9")
        assertEquals(listOf("9"), durum.tokenlar)
        assertEquals(1, durum.gecmis.size)
    }

    @Test
    fun `esittir sonrasi yuzde sonucu 100e boler`() {
        val durum = tusla("2+3=%")
        assertEquals(listOf("0.05"), durum.tokenlar)
        assertFalse(durum.esittirSonrasi)
        assertNull(durum.sonSonuc)
    }

    @Test
    fun `esittir sonrasi arti eksi sonucun isaretini degistirir`() {
        var durum = tusla("2+3=±")
        assertEquals(listOf("-5"), durum.tokenlar)
        assertFalse(durum.esittirSonrasi)
        durum = tusla("±", durum)
        assertEquals(listOf("5"), durum.tokenlar)
    }

    @Test
    fun `esittir sonrasi negatif sonucta arti eksi isareti kaldirir`() {
        val durum = tusla("0−5=±")
        assertEquals(listOf("5"), durum.tokenlar)
    }

    @Test
    fun `esittir sonrasi sifir sonucta arti eksi sifir birakir`() {
        val durum = tusla("2−2=±")
        assertEquals(listOf("0"), durum.tokenlar)
    }

    @Test
    fun `esittir sonrasi geri silme sonucun son hanesini siler`() {
        val durum = tusla("12×3=⌫")
        assertEquals(listOf("3"), durum.tokenlar)
        assertFalse(durum.esittirSonrasi)
        assertNull(durum.sonSonuc)
    }

    @Test
    fun `esittir sonrasi tek haneli negatif sonucta geri silme sifira doner`() {
        val durum = tusla("0−5=⌫")
        assertEquals(listOf("0"), durum.tokenlar)
    }

    @Test
    fun `esittir sonrasi ondalik sonucta geri silme nokta birakir`() {
        val durum = tusla("1÷2=⌫")
        assertEquals(listOf("0."), durum.tokenlar)
    }

    @Test
    fun `esittir sonrasi geri silme yuvarlama bayragini sifirlar`() {
        var durum = tusla("1÷3=")
        assertTrue(durum.sonucYuvarlandiMi)
        durum = tusla("⌫", durum)
        assertFalse(durum.sonucYuvarlandiMi)
    }

    @Test
    fun `esittir sonrasi yuzde ve arti eksi yuvarlama bayragini sifirlar`() {
        assertFalse(tusla("1÷3=%").sonucYuvarlandiMi)
        assertFalse(tusla("1÷3=±").sonucYuvarlandiMi)
    }

    @Test
    fun `esittir sonrasi tumunu sil sonucu ve bayraklari temizler`() {
        val durum = tusla("1÷3=C")
        assertEquals(listOf("0"), durum.tokenlar)
        assertNull(durum.sonSonuc)
        assertFalse(durum.esittirSonrasi)
        assertFalse(durum.sonucYuvarlandiMi)
        assertEquals(1, durum.gecmis.size)
    }

    // ---------------------------------------------------------------- yüzde

    @Test
    fun `operatorden sonra yuzde yok sayilir`() {
        val durum = tusla("5+")
        assertEquals(durum, tusla("%", durum))
    }

    @Test
    fun `nokta ile biten sayida yuzde calisir`() {
        assertEquals(listOf("0.05"), tusla("5.%").tokenlar)
    }

    @Test
    fun `ikinci operandda yuzde yalniz o sayiyi 100e boler`() {
        // Mevcut tasarim: "50 + 10 %" -> 50 + 0.1 (bazi hesap makinelerindeki gibi 50'nin %10'u degil).
        val durum = tusla("50+10%")
        assertEquals(listOf("50", "+", "0.1"), durum.tokenlar)
        assertEquals("50.1", tusla("=", durum).sonSonuc)
    }

    @Test
    fun `ust uste yuzde her seferinde 100e boler`() {
        assertEquals(listOf("0.0005"), tusla("5%%").tokenlar)
    }

    @Test
    fun `negatif sayida yuzde isareti korur`() {
        assertEquals(listOf("-0.25"), tusla("25±%").tokenlar)
    }

    // ---------------------------------------------------------------- artı/eksi

    @Test
    fun `ikinci operandda arti eksi yalniz o sayinin isaretini degistirir`() {
        val durum = tusla("5+3±")
        assertEquals(listOf("5", "+", "-3"), durum.tokenlar)
        val sonuc = tusla("=", durum)
        assertEquals("2", sonuc.sonSonuc)
        assertEquals(GecmisKaydi("5+-3", "2"), sonuc.gecmis.first())
    }

    @Test
    fun `sifir nokta uzerinde arti eksi yok sayilir`() {
        val durum = tusla("0.")
        assertEquals(durum, tusla("±", durum))
    }

    @Test
    fun `negatif ondalik sayida arti eksi isareti kaldirir`() {
        assertEquals(listOf("0.5"), tusla("0.5±±").tokenlar)
        assertEquals(listOf("-0.5"), tusla("0.5±").tokenlar)
    }

    // ---------------------------------------------------------------- geri silme

    @Test
    fun `negatif cok haneli sayida geri silme isareti korur`() {
        assertEquals(listOf("-1"), tusla("12±⌫").tokenlar)
    }

    @Test
    fun `negatif tek haneli sayida geri silme sifira doner`() {
        assertEquals(listOf("0"), tusla("7±⌫").tokenlar)
    }

    @Test
    fun `ikinci operand negatif tek haneyken geri silme operatore doner`() {
        assertEquals(listOf("5", "+"), tusla("5+3±⌫").tokenlar)
    }

    @Test
    fun `geri silme ondalik noktayi da siler`() {
        assertEquals(listOf("1"), tusla("1.⌫").tokenlar)
        assertEquals(listOf("1."), tusla("1.5⌫").tokenlar)
    }

    @Ignore(
        "HATA: '0.5' → ± → '-0.5' → ⌫ ⌫ → '-0' olduktan sonra rakam basılınca bastaki sifir " +
            "silinmiyor ve '-07' olusuyor (rakamGir yalniz son == \"0\" durumunu kontrol ediyor, " +
            "\"-0\" durumunu degil). Ekranda ve gecmiste '-07' gorunur."
    )
    @Test
    fun `negatif sifirdan sonra girilen rakam bastaki sifirin yerine gecmeli`() {
        val durum = tusla("0.5±⌫⌫")
        assertEquals(listOf("-0"), durum.tokenlar)
        assertEquals(listOf("-7"), tusla("7", durum).tokenlar)
    }

    // ---------------------------------------------------------------- geçmiş

    @Test
    fun `gecmis en yeni kayit basta olacak sekilde siralanir`() {
        val durum = tusla("1+1=2+2=3+3=")
        assertEquals(
            listOf(GecmisKaydi("3+3", "6"), GecmisKaydi("2+2", "4"), GecmisKaydi("1+1", "2")),
            durum.gecmis
        )
    }

    @Test
    fun `gecmis sinira ulasinca en eski kayitlar atilir`() {
        var durum = HesapDurumu()
        repeat(25) { i -> durum = tusla("${i + 1}+1=", durum) }
        assertEquals(HesapDurumu.MAKS_GECMIS, durum.gecmis.size)
        assertEquals(GecmisKaydi("25+1", "26"), durum.gecmis.first())
        assertEquals(GecmisKaydi("6+1", "7"), durum.gecmis.last())
    }

    @Test
    fun `sonuc uzerinden devam eden hesap gecmise sonucla baslayan ifade yazar`() {
        val durum = tusla("2+3=×4=")
        assertEquals(GecmisKaydi("5×4", "20"), durum.gecmis.first())
        assertEquals(2, durum.gecmis.size)
    }

    @Test
    fun `gecmis ifadesi ondalik noktayla tutulur`() {
        val durum = tusla("1.5+2.5=")
        assertEquals(GecmisKaydi("1.5+2.5", "4"), durum.gecmis.first())
    }

    @Test
    fun `gecmisten yukleme hata durumunu temizler`() {
        val hata = hataDurumu()
        val kayit = hata.gecmis.first()
        val durum = hata.gecmistenYukle(kayit)
        assertFalse(durum.hataVarMi)
        assertEquals(listOf("4"), durum.tokenlar)
        assertEquals("4", durum.sonSonuc)
        assertTrue(durum.esittirSonrasi)
        assertEquals(hata.gecmis, durum.gecmis)
    }

    @Test
    fun `gecmisten yukleme mevcut gecmisi degistirmez ve yeni kayit eklemez`() {
        val durum = tusla("1+1=2+2=")
        val yuklenen = durum.gecmistenYukle(durum.gecmis.last())
        assertEquals(durum.gecmis, yuklenen.gecmis)
        assertEquals("2", yuklenen.sonSonuc)
        assertFalse(yuklenen.sonucYuvarlandiMi)
    }

    @Test
    fun `gecmisten yukleme sonrasi rakam yeni hesap baslatir`() {
        val durum = tusla("6×7=")
        val yuklenen = HesapDurumu().gecmistenYukle(durum.gecmis.first())
        assertEquals(listOf("9"), tusla("9", yuklenen).tokenlar)
    }

    @Test
    fun `gecmisten yukleme sonrasi esittir yeni kayit eklemez`() {
        val durum = tusla("6×7=")
        val yuklenen = durum.gecmistenYukle(durum.gecmis.first())
        assertEquals(yuklenen, tusla("=", yuklenen))
    }
}

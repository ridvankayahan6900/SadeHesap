package com.ridvan.sadehesap.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DegerlendiriciTest {

    private fun basarili(tokenlar: List<String>): BigDecimal {
        val sonuc = Degerlendirici.degerlendir(tokenlar)
        assertTrue("Beklenen: Basarili, gelen: $sonuc", sonuc is DegerlendirmeSonucu.Basarili)
        return (sonuc as DegerlendirmeSonucu.Basarili).deger
    }

    @Test
    fun `carpma toplamadan once yapilir`() {
        // 2 + 3 * 4 = 14 (8 degil)
        val deger = basarili(listOf("2", "+", "3", "×", "4"))
        assertEquals("14", Degerlendirici.sadeMetne(deger))
    }

    @Test
    fun `bolme cikarmadan once yapilir`() {
        // 20 - 8 / 2 = 16 (6 degil)
        val deger = basarili(listOf("20", "−", "8", "÷", "2"))
        assertEquals("16", Degerlendirici.sadeMetne(deger))
    }

    @Test
    fun `ondalikli sayilarla toplama dogru sonuc verir`() {
        val deger = basarili(listOf("1.5", "+", "2.5"))
        assertEquals("4", Degerlendirici.sadeMetne(deger))
    }

    @Test
    fun `sifira bolme hata dondurur`() {
        val sonuc = Degerlendirici.degerlendir(listOf("5", "÷", "0"))
        assertEquals(DegerlendirmeSonucu.Hata, sonuc)
    }

    @Test
    fun `carpma sonrasi sifira bolme de hata dondurur`() {
        val sonuc = Degerlendirici.degerlendir(listOf("5", "×", "2", "÷", "0"))
        assertEquals(DegerlendirmeSonucu.Hata, sonuc)
    }

    @Test
    fun `operatorle biten eksik ifade hata dondurur`() {
        val sonuc = Degerlendirici.degerlendir(listOf("5", "+"))
        assertEquals(DegerlendirmeSonucu.Hata, sonuc)
    }

    @Test
    fun `bos ifade hata dondurur`() {
        val sonuc = Degerlendirici.degerlendir(emptyList())
        assertEquals(DegerlendirmeSonucu.Hata, sonuc)
    }

    @Test
    fun `negatif sayiyla islem dogru sonuc verir`() {
        // (-5) + 10 = 5
        val deger = basarili(listOf("-5", "+", "10"))
        assertEquals("5", Degerlendirici.sadeMetne(deger))
    }

    @Test
    fun `buyuk sayilarin carpimi hassasiyetle hesaplanir`() {
        val deger = basarili(listOf("123456789", "×", "987654321"))
        // 18 basamaklı tam sonuç, 20 basamaklık MathContext sınırının içinde — yuvarlama olmaz.
        assertEquals(BigDecimal("121932631112635269"), deger)
    }

    @Test
    fun `sadeMetne gereksiz sifirlari kirpar`() {
        assertEquals("3.5", Degerlendirici.sadeMetne(BigDecimal("3.5000")))
        assertEquals("4", Degerlendirici.sadeMetne(BigDecimal("4.000")))
    }

    @Test
    fun `sadeMetne en fazla 12 anlamli basamak gosterir`() {
        // 1'i 3'e bolmenin 20 basamaklik sonucu 12 anlamli basamaga yuvarlanmali.
        val deger = basarili(listOf("1", "÷", "3"))
        val metin = Degerlendirici.sadeMetne(deger)
        val anlamliBasamak = metin.replace("-", "").replace(".", "").trimStart('0').length
        assertTrue("Beklenen <=12 basamak, gelen: $metin ($anlamliBasamak)", anlamliBasamak <= 12)
    }

    @Test
    fun `sadeMetne sifiri her zaman 0 olarak gosterir`() {
        assertEquals("0", Degerlendirici.sadeMetne(BigDecimal.ZERO))
        assertEquals("0", Degerlendirici.sadeMetne(BigDecimal("0.00")))
    }

    @Test
    fun `ardisik toplama ve cikarma soldan saga uygulanir`() {
        // 10 - 3 + 2 = 9 (10 - (3+2)=5 degil)
        val deger = basarili(listOf("10", "−", "3", "+", "2"))
        assertEquals("9", Degerlendirici.sadeMetne(deger))
    }

    // --- yuvarlandiMi: sadeMetne'nin GOSTERIM_BASAMAK=12 sinirindan dolayi bir degeri
    // yuvarlayip yuvarlamadigini UI'ya bildiren ayri bayrak (inceleme bulgusu: sessiz
    // yuvarlama kullaniciya hicbir isaretle gosterilmiyordu). ---
    @Test
    fun `12 basamaktan az sonuclarda yuvarlandiMi false doner`() {
        val deger = basarili(listOf("2", "+", "3"))
        assertEquals("5", Degerlendirici.sadeMetne(deger))
        assertFalse(Degerlendirici.yuvarlandiMi(deger))
    }

    @Test
    fun `12 basamaktan fazla sonuclarda yuvarlandiMi true doner`() {
        // 1 / 3, 20 basamak hassasiyetle 0.333...3 (12'den fazla anlamli basamak) doner.
        val deger = basarili(listOf("1", "÷", "3"))
        assertTrue(Degerlendirici.yuvarlandiMi(deger))
    }

    @Test
    fun `sifir yuvarlandiMi icin her zaman false doner`() {
        assertFalse(Degerlendirici.yuvarlandiMi(BigDecimal.ZERO))
        assertFalse(Degerlendirici.yuvarlandiMi(BigDecimal("0.00")))
    }
}

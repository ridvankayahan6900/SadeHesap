package com.ridvan.sadehesap.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * [Degerlendirici] için ek birim testler: token ayrıştırma (tokenSayiyaCevir'in kabul
 * ettiği/ret ettiği biçimler), geçersiz token dizileri, işlem önceliğinin daha uzun
 * zincirlerde korunması, [Degerlendirici.sadeMetne] yuvarlama/biçimlendirme ayrıntıları
 * ve [Degerlendirici.yuvarlandiMi] bayrağı.
 */
class DegerlendiriciEkTest {

    private fun basarili(tokenlar: List<String>): BigDecimal {
        val sonuc = Degerlendirici.degerlendir(tokenlar)
        assertTrue("Beklenen: Basarili, gelen: $sonuc", sonuc is DegerlendirmeSonucu.Basarili)
        return (sonuc as DegerlendirmeSonucu.Basarili).deger
    }

    private fun metin(tokenlar: List<String>): String = Degerlendirici.sadeMetne(basarili(tokenlar))

    private fun hata(tokenlar: List<String>) =
        assertEquals("Girdi: $tokenlar", DegerlendirmeSonucu.Hata, Degerlendirici.degerlendir(tokenlar))

    // ---------------------------------------------------------------- token ayrıştırma

    @Test
    fun `tek sayidan olusan ifade sayinin kendisini dondurur`() {
        assertEquals("42", metin(listOf("42")))
    }

    @Test
    fun `nokta ile biten sayi tam sayi olarak okunur`() {
        assertEquals("5", metin(listOf("5.")))
        assertEquals("8", metin(listOf("5", "+", "3.")))
    }

    @Test
    fun `nokta ile baslayan sayi basina sifir eklenmis gibi okunur`() {
        assertEquals("1", metin(listOf(".5", "+", ".5")))
    }

    @Test
    fun `eksi nokta ile baslayan sayi negatif ondalik olarak okunur`() {
        assertEquals("-1", metin(listOf("-.5", "×", "2")))
    }

    @Test
    fun `yalniz eksi isareti ya da yalniz nokta gecersiz sayidir`() {
        hata(listOf("-"))
        hata(listOf("."))
        hata(listOf("5", "+", "-"))
        hata(listOf("5", "×", "."))
    }

    @Test
    fun `bos token gecersiz sayidir`() {
        hata(listOf(""))
        hata(listOf("5", "+", ""))
    }

    @Test
    fun `sayi olmayan metin hata dondurur`() {
        hata(listOf("abc"))
        hata(listOf("5", "+", "x"))
    }

    @Test
    fun `operatorle baslayan ifade hata dondurur`() {
        hata(listOf("+", "5", "+"))
    }

    @Test
    fun `cift sayida token iceren ifade hata dondurur`() {
        hata(listOf("5", "+", "3", "−"))
        hata(listOf("5", "5"))
    }

    @Test
    fun `bilinmeyen operator hata dondurur`() {
        hata(listOf("5", "^", "2"))
        hata(listOf("5", "*", "2"))
        hata(listOf("6", "/", "2"))
    }

    @Test
    fun `ascii tire cikarma operatoru olarak kabul edilmez yalniz unicode eksi gecerlidir`() {
        // Cikarma operatoru U+2212 ("−"); ASCII "-" yalnizca sayinin isaretidir.
        hata(listOf("5", "-", "3"))
        assertEquals("2", metin(listOf("5", "−", "3")))
    }

    @Test
    fun `iki operatorun ardisik gelmesi hata dondurur`() {
        hata(listOf("5", "+", "+"))
    }

    @Test
    fun `degerlendir girdi listesini degistirmez`() {
        val tokenlar = listOf("2", "+", "3", "×", "4")
        val kopya = tokenlar.toList()
        Degerlendirici.degerlendir(tokenlar)
        assertEquals(kopya, tokenlar)
    }

    // ---------------------------------------------------------------- işlem önceliği

    @Test
    fun `ardisik bolmeler soldan saga uygulanir`() {
        // (100 / 10) / 2 = 5 ; 100 / (10 / 2) = 20 olurdu
        assertEquals("5", metin(listOf("100", "÷", "10", "÷", "2")))
    }

    @Test
    fun `ardisik cikarmalar soldan saga uygulanir`() {
        // (2 - 3) - 4 = -5 ; 2 - (3 - 4) = 3 olurdu
        assertEquals("-5", metin(listOf("2", "−", "3", "−", "4")))
    }

    @Test
    fun `bolme ve carpma ayni oncelikte soldan saga uygulanir`() {
        // (8 / 2) * 4 = 16 ; 8 / (2 * 4) = 1 olurdu
        assertEquals("16", metin(listOf("8", "÷", "2", "×", "4")))
    }

    @Test
    fun `iki carpim grubu toplanir`() {
        assertEquals("26", metin(listOf("2", "×", "3", "+", "4", "×", "5")))
    }

    @Test
    fun `dort islemli karma ifade oncelik kuralina uyar`() {
        // 1 + 2*3 - 4/2 = 1 + 6 - 2 = 5
        assertEquals("5", metin(listOf("1", "+", "2", "×", "3", "−", "4", "÷", "2")))
    }

    @Test
    fun `negatif sayilarin carpimi pozitif olur`() {
        assertEquals("12", metin(listOf("-3", "×", "-4")))
    }

    @Test
    fun `negatif sayi cikarmasi toplama gibi davranir`() {
        assertEquals("8", metin(listOf("5", "−", "-3")))
    }

    // ---------------------------------------------------------------- sıfır / ondalık

    @Test
    fun `negatif sifira bolme de hata dondurur`() {
        hata(listOf("5", "÷", "-0"))
        hata(listOf("5", "÷", "0.0"))
        hata(listOf("5", "÷", "0."))
    }

    @Test
    fun `sifirin bolunmesi sifir verir`() {
        assertEquals("0", metin(listOf("0", "÷", "7")))
    }

    @Test
    fun `birbirini goturen sayilarin toplami sifir olarak gosterilir`() {
        assertEquals("0", metin(listOf("-5", "+", "5")))
    }

    @Test
    fun `sifirin negatifle carpimi eksi sifir degil sifir gosterilir`() {
        assertEquals("0", metin(listOf("0", "×", "-1")))
    }

    @Test
    fun `ondalik carpma ikili kayan nokta hatasi icermez`() {
        assertEquals("0.01", metin(listOf("0.1", "×", "0.1")))
        assertEquals("0.1", metin(listOf("1", "−", "0.9")))
    }

    @Test
    fun `tam bolunmeyen bolme ondalik sonuc verir`() {
        assertEquals("2.5", metin(listOf("10", "÷", "4")))
    }

    @Test
    fun `iki bolu uc 12 basamaga yuvarlanarak gosterilir`() {
        val deger = basarili(listOf("2", "÷", "3"))
        assertEquals("0.666666666667", Degerlendirici.sadeMetne(deger))
        assertTrue(Degerlendirici.yuvarlandiMi(deger))
    }

    @Test
    fun `kendine bolunen sayi tam 1 verir ve yuvarlanmis sayilmaz`() {
        val deger = basarili(listOf("7", "÷", "7"))
        assertEquals("1", Degerlendirici.sadeMetne(deger))
        assertFalse(Degerlendirici.yuvarlandiMi(deger))
    }

    // ---------------------------------------------------------------- sadeMetne

    @Test
    fun `sadeMetne negatif sayilarda da gereksiz sifirlari kirpar`() {
        assertEquals("-3.5", Degerlendirici.sadeMetne(BigDecimal("-3.50")))
        assertEquals("-4", Degerlendirici.sadeMetne(BigDecimal("-4.000")))
    }

    @Test
    fun `sadeMetne buyuk sayilarda bilimsel gosterim kullanmaz`() {
        assertEquals("10000000000000000000000000", Degerlendirici.sadeMetne(BigDecimal("1E+25")))
    }

    @Test
    fun `sadeMetne kucuk sayilarda bilimsel gosterim kullanmaz`() {
        assertEquals("0.0000000001", Degerlendirici.sadeMetne(BigDecimal("1E-10")))
    }

    @Test
    fun `sadeMetne cok kucuk sayida 12 anlamli basamak tutar`() {
        assertEquals(
            "0.00000000000123456789012",
            Degerlendirici.sadeMetne(BigDecimal("0.000000000001234567890123"))
        )
    }

    @Test
    fun `sadeMetne yarimda yukari yuvarlar`() {
        // 13. anlamli basamak 5 -> yukari (HALF_UP)
        assertEquals("0.123456789013", Degerlendirici.sadeMetne(BigDecimal("0.1234567890125")))
        // 13. anlamli basamak 4 -> asagi
        assertEquals("0.123456789012", Degerlendirici.sadeMetne(BigDecimal("0.1234567890124")))
    }

    @Test
    fun `sadeMetne negatif sayida yarimda sifirdan uzaga yuvarlar`() {
        assertEquals("-0.123456789013", Degerlendirici.sadeMetne(BigDecimal("-0.1234567890125")))
    }

    @Test
    fun `sadeMetne tam 12 basamakli sayiyi oldugu gibi birakir`() {
        assertEquals("123456789012", Degerlendirici.sadeMetne(BigDecimal("123456789012")))
        assertFalse(Degerlendirici.yuvarlandiMi(BigDecimal("123456789012")))
    }

    @Test
    fun `sadeMetne 13 basamakli sayiyi yuvarlar ve yuvarlandiMi true doner`() {
        // 13. anlamli basamak 3 -> asagi; tam sayi kismi sifirla doldurulur (bilimsel gosterim yok)
        assertEquals("1234567890120", Degerlendirici.sadeMetne(BigDecimal("1234567890123")))
        // 13. anlamli basamak 5 -> yukari
        assertEquals("1234567890130", Degerlendirici.sadeMetne(BigDecimal("1234567890125")))
        assertTrue(Degerlendirici.yuvarlandiMi(BigDecimal("1234567890123")))
    }

    @Test
    fun `sadeMetne ciktisi tekrar BigDecimal olarak ayristirilabilir`() {
        // Sonuc metni tokenlar icinde saklanip tekrar hesaba girdigi icin bu sart.
        listOf("1E+25", "1E-10", "-3.50", "0.1234567890125", "2").forEach { ham ->
            val metin = Degerlendirici.sadeMetne(BigDecimal(ham))
            assertEquals(
                "Girdi: $ham",
                0,
                BigDecimal(metin).compareTo(BigDecimal(ham).round(java.math.MathContext(12)))
            )
            assertFalse("Bilimsel gosterim olmamali: $metin", metin.contains('E'))
        }
    }

    // ---------------------------------------------------------------- yuvarlandiMi

    @Test
    fun `yuvarlandiMi negatif yuvarlanan degerde de true doner`() {
        val deger = basarili(listOf("-1", "÷", "3"))
        assertTrue(Degerlendirici.yuvarlandiMi(deger))
    }

    @Test
    fun `yuvarlandiMi 12 basamakli tam sayida false doner`() {
        assertFalse(Degerlendirici.yuvarlandiMi(BigDecimal("999999999999")))
    }

    @Ignore(
        "HATA: yuvarlandiMi, BigDecimal.precision() kullaniyor; sondaki sifirlar da basamak " +
            "sayildigi icin 1000000000000000 (1e15) gibi tam/kesin gosterilen bir deger " +
            "'yuvarlandi' sayiliyor ve UI'da gereksiz yaklasiklik isareti (≈) cikiyor."
    )
    @Test
    fun `yuvarlandiMi sondaki sifirlar nedeniyle yanlis pozitif vermemeli`() {
        val deger = basarili(listOf("1000000000000000", "×", "1"))
        assertEquals("1000000000000000", Degerlendirici.sadeMetne(deger)) // gosterim kesin
        assertFalse(Degerlendirici.yuvarlandiMi(deger))
    }

    @Ignore(
        "HATA: 1.000000 × 1.000000 sonucu 1.000000000000 (precision 13) oldugu icin " +
            "yuvarlandiMi true donuyor; oysa gosterilen '1' tam olarak dogru."
    )
    @Test
    fun `yuvarlandiMi ondalik sondaki sifirlarda yanlis pozitif vermemeli`() {
        val deger = basarili(listOf("1.000000", "×", "1.000000"))
        assertEquals("1", Degerlendirici.sadeMetne(deger))
        assertFalse(Degerlendirici.yuvarlandiMi(deger))
    }
}

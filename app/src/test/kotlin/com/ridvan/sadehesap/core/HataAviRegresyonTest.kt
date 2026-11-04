package com.ridvan.sadehesap.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Hata avı (2026-11-04) sırasında bulunan hataların regresyon testleri. */
class HataAviRegresyonTest {

    private fun rakamlar(metin: String, durum: HesapDurumu = HesapDurumu()): HesapDurumu =
        metin.fold(durum) { d, c -> d.uygula(HesapTusu.Rakam(c)) }

    private fun hesapla(a: String, op: HesapTusu, b: String, durum: HesapDurumu = HesapDurumu()): HesapDurumu {
        var d = rakamlar(a, durum)
        d = d.uygula(op)
        d = rakamlar(b, d)
        return d.uygula(HesapTusu.Esittir)
    }

    // --- yuvarlandiMi sondaki sıfırları anlamlı basamak sayıyordu ---
    @Test
    fun `tam 1e15 sonucu yuvarlanmis sayilmaz`() {
        val durum = hesapla("1000000", HesapTusu.Carp, "1000000000")
        assertEquals("1000000000000000", durum.sonSonuc)
        assertFalse(durum.sonucYuvarlandiMi)
    }

    @Test
    fun `sondaki sifirlar yuvarlandiMi yi tetiklemez`() {
        assertFalse(Degerlendirici.yuvarlandiMi(BigDecimal("1.0000000000000")))
        assertFalse(Degerlendirici.yuvarlandiMi(BigDecimal("1000000000000000")))
        // Gerçekten kırpılan değer hâlâ yuvarlanmış sayılır.
        assertTrue(Degerlendirici.yuvarlandiMi(BigDecimal("1000000000000001")))
    }

    // --- "-0" sonrası rakam "-07" üretiyordu ---
    @Test
    fun `eksi sifirdan sonra girilen rakam bastaki sifirin yerine gecer`() {
        var durum = HesapDurumu().uygula(HesapTusu.Ondalik)
        durum = rakamlar("5", durum) // 0.5
        durum = durum.uygula(HesapTusu.ArtiEksi) // -0.5
        durum = durum.uygula(HesapTusu.GeriSil).uygula(HesapTusu.GeriSil) // -0
        assertEquals(listOf("-0"), durum.tokenlar)
        durum = rakamlar("7", durum)
        assertEquals(listOf("-7"), durum.tokenlar)
    }

    // --- üst satır, sonuca göre geçmişte arama yaptığı için yanlış ifadeyi gösteriyordu ---
    @Test
    fun `gecmisten yuklenen kaydin kendi ifadesi tutulur`() {
        val ilk = hesapla("2", HesapTusu.Topla, "2") // 2+2 = 4
        val ikinci = hesapla("1", HesapTusu.Topla, "3", HesapDurumu(gecmis = ilk.gecmis)) // 1+3 = 4
        assertEquals("1+3", ikinci.sonIfade)

        val eskiKayit = ikinci.gecmis[1]
        assertEquals("2+2", eskiKayit.ifade)
        val yuklenen = ikinci.gecmistenYukle(eskiKayit)
        assertEquals("2+2", yuklenen.sonIfade)
    }

    // --- rememberSaveable kodlaması sonucYuvarlandiMi'yi kaydetmiyordu (döndürmede "≈" kayboluyordu) ---
    @Test
    fun `durum kodlamasi tum alanlari korur`() {
        val durum = hesapla("1", HesapTusu.Bol, "3")
        assertTrue(durum.sonucYuvarlandiMi)
        assertEquals(durum, DurumKodlayici.coz(DurumKodlayici.kodla(durum)))

        val bos = HesapDurumu()
        assertEquals(bos, DurumKodlayici.coz(DurumKodlayici.kodla(bos)))

        val hata = hesapla("5", HesapTusu.Bol, "0")
        assertTrue(hata.hataVarMi)
        assertEquals(hata, DurumKodlayici.coz(DurumKodlayici.kodla(hata)))
    }

    @Test
    fun `eski bes alanli kodlama da cozulur`() {
        val eski = listOf("12", "\u0004", "0", "0", "").joinToString("\u0005")
        val durum = DurumKodlayici.coz(eski)
        assertEquals(listOf("12"), durum.tokenlar)
        assertFalse(durum.sonucYuvarlandiMi)
        assertEquals(null, durum.sonIfade)
    }
}

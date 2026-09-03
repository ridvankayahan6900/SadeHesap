package com.ridvan.sadehesap.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HesapDurumuTest {

    private fun rakamlar(metin: String, durum: HesapDurumu = HesapDurumu()): HesapDurumu =
        metin.fold(durum) { d, c -> d.uygula(HesapTusu.Rakam(c)) }

    @Test
    fun `rakam girisi ifadeyi olusturur`() {
        val durum = rakamlar("12")
        assertEquals(listOf("12"), durum.tokenlar)
    }

    @Test
    fun `bastaki sifirin yerine yeni rakam yazilir`() {
        val durum = HesapDurumu().uygula(HesapTusu.Rakam('7'))
        assertEquals(listOf("7"), durum.tokenlar)
    }

    @Test
    fun `ondalik ayirici iki kez eklenemez`() {
        var durum = rakamlar("1")
        durum = durum.uygula(HesapTusu.Ondalik)
        durum = durum.uygula(HesapTusu.Rakam('5'))
        durum = durum.uygula(HesapTusu.Ondalik) // yok sayilmali
        durum = durum.uygula(HesapTusu.Rakam('2'))
        assertEquals(listOf("1.52"), durum.tokenlar)
    }

    @Test
    fun `ardisik operatorde son operator degistirilir`() {
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.Topla)
        durum = durum.uygula(HesapTusu.Carp) // + yerine × gecerli olmali
        assertEquals(listOf("5", "×"), durum.tokenlar)
    }

    @Test
    fun `esittir sonrasi yeni rakam girisi ifadeyi sifirlar`() {
        var durum = rakamlar("2")
        durum = durum.uygula(HesapTusu.Topla)
        durum = rakamlar("3", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals("5", durum.sonSonuc)

        durum = durum.uygula(HesapTusu.Rakam('9'))
        assertEquals(listOf("9"), durum.tokenlar)
        assertFalse(durum.esittirSonrasi)
    }

    @Test
    fun `esittir sonrasi operator girisi sonuc uzerinden devam eder`() {
        var durum = rakamlar("2")
        durum = durum.uygula(HesapTusu.Topla)
        durum = rakamlar("3", durum)
        durum = durum.uygula(HesapTusu.Esittir) // sonuc: 5

        durum = durum.uygula(HesapTusu.Carp)
        durum = rakamlar("4", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals("20", durum.sonSonuc)
    }

    @Test
    fun `geri silme son haneyi siler`() {
        var durum = rakamlar("123")
        durum = durum.uygula(HesapTusu.GeriSil)
        assertEquals(listOf("12"), durum.tokenlar)
    }

    @Test
    fun `geri silme tek haneli sayiyi sifira dondurur`() {
        var durum = rakamlar("7")
        durum = durum.uygula(HesapTusu.GeriSil)
        assertEquals(listOf("0"), durum.tokenlar)
    }

    @Test
    fun `geri silme operatoru siler`() {
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.Topla)
        durum = durum.uygula(HesapTusu.GeriSil)
        assertEquals(listOf("5"), durum.tokenlar)
    }

    @Test
    fun `geri silme ikinci sayinin tamamini silince operatore doner`() {
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.Topla)
        durum = durum.uygula(HesapTusu.Rakam('3'))
        durum = durum.uygula(HesapTusu.GeriSil) // "3" tek hane, silinince operatore donmeli
        assertEquals(listOf("5", "+"), durum.tokenlar)
    }

    @Test
    fun `yuzde girilen sayiyi 100e boler`() {
        var durum = rakamlar("50")
        durum = durum.uygula(HesapTusu.Yuzde)
        assertEquals(listOf("0.5"), durum.tokenlar)
    }

    @Test
    fun `arti eksi isareti degistirir ve tekrar basinca geri alir`() {
        var durum = rakamlar("9")
        durum = durum.uygula(HesapTusu.ArtiEksi)
        assertEquals(listOf("-9"), durum.tokenlar)
        durum = durum.uygula(HesapTusu.ArtiEksi)
        assertEquals(listOf("9"), durum.tokenlar)
    }

    @Test
    fun `sifira bolme hata durumuna gecirir`() {
        var durum = rakamlar("8")
        durum = durum.uygula(HesapTusu.Bol)
        durum = durum.uygula(HesapTusu.Rakam('0'))
        durum = durum.uygula(HesapTusu.Esittir)
        assertTrue(durum.hataVarMi)
    }

    @Test
    fun `hata durumunda rakam girilince sifirlanir`() {
        var durum = rakamlar("8")
        durum = durum.uygula(HesapTusu.Bol)
        durum = durum.uygula(HesapTusu.Rakam('0'))
        durum = durum.uygula(HesapTusu.Esittir)
        assertTrue(durum.hataVarMi)

        durum = durum.uygula(HesapTusu.Rakam('4'))
        assertFalse(durum.hataVarMi)
        assertEquals(listOf("4"), durum.tokenlar)
    }

    @Test
    fun `tumunu sil gecmisi korur ama ifadeyi sifirlar`() {
        var durum = rakamlar("2")
        durum = durum.uygula(HesapTusu.Topla)
        durum = rakamlar("2", durum)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals(1, durum.gecmis.size)

        durum = durum.uygula(HesapTusu.TumunuSil)
        assertEquals(listOf("0"), durum.tokenlar)
        assertEquals(1, durum.gecmis.size)
    }

    @Test
    fun `gecmis en fazla 20 kayit tutar`() {
        var durum = HesapDurumu()
        repeat(25) { i ->
            durum = rakamlar((i + 1).toString(), durum)
            durum = durum.uygula(HesapTusu.Topla)
            durum = rakamlar("1", durum)
            durum = durum.uygula(HesapTusu.Esittir)
        }
        assertEquals(HesapDurumu.MAKS_GECMIS, durum.gecmis.size)
    }

    @Test
    fun `gecmisten kayda dokununca ifadeye yuklenir`() {
        var durum = rakamlar("6")
        durum = durum.uygula(HesapTusu.Carp)
        durum = rakamlar("7", durum)
        durum = durum.uygula(HesapTusu.Esittir) // 42, gecmiste "6×7 = 42"

        val kayit = durum.gecmis.first()
        assertEquals("42", kayit.sonuc)

        var yeniDurum = HesapDurumu(gecmis = durum.gecmis).gecmistenYukle(kayit)
        assertEquals(listOf("42"), yeniDurum.tokenlar)
        assertTrue(yeniDurum.esittirSonrasi)

        // Uzerinden devam edilebilir: 42 + 8 = 50
        yeniDurum = yeniDurum.uygula(HesapTusu.Topla)
        yeniDurum = rakamlar("8", yeniDurum)
        yeniDurum = yeniDurum.uygula(HesapTusu.Esittir)
        assertEquals("50", yeniDurum.sonSonuc)
    }

    @Test
    fun `operatorle biten ifadede esittir yok sayilir`() {
        var durum = rakamlar("5")
        durum = durum.uygula(HesapTusu.Topla)
        durum = durum.uygula(HesapTusu.Esittir)
        assertEquals(listOf("5", "+"), durum.tokenlar)
        assertFalse(durum.esittirSonrasi)
    }
}

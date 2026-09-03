package com.ridvan.sadehesap.core

import java.math.BigDecimal
import java.math.MathContext

/** Kullanıcının basabileceği tuşlar — UI'daki yerelleştirilmiş etiketlerden bağımsızdır. */
sealed interface HesapTusu {
    data class Rakam(val hane: Char) : HesapTusu
    data object Ondalik : HesapTusu
    data object Topla : HesapTusu
    data object Cikar : HesapTusu
    data object Carp : HesapTusu
    data object Bol : HesapTusu
    data object Yuzde : HesapTusu
    data object ArtiEksi : HesapTusu
    data object TumunuSil : HesapTusu
    data object GeriSil : HesapTusu
    data object Esittir : HesapTusu
}

/** Geçmişteki tek bir hesap kaydı — ifade ve sonuç her zaman nokta ondalık ayırıcıyla tutulur. */
data class GecmisKaydi(val ifade: String, val sonuc: String)

/**
 * Hesap makinesinin değişmez (immutable) durumu. [tokenlar] her zaman bir sayı ile başlar;
 * sayı-operatör-sayı-... biçiminde alternatif elemanlardan oluşur, son eleman sayı ya da
 * operatör olabilir (yazım sürerken). Tüm sayılar nokta ondalık ayırıcıyla tutulur;
 * yerelleştirilmiş görünüm UI katmanında üretilir.
 */
data class HesapDurumu(
    val tokenlar: List<String> = listOf("0"),
    val sonSonuc: String? = null,
    val hataVarMi: Boolean = false,
    val esittirSonrasi: Boolean = false,
    val gecmis: List<GecmisKaydi> = emptyList()
) {
    companion object {
        const val MAKS_GECMIS = 20
    }
}

private fun isOperator(s: String) = s == "+" || s == "−" || s == "×" || s == "÷"

/** [durum] üzerine [tus] uygulanır, yeni (değişmez) bir [HesapDurumu] döner — saf reducer. */
fun HesapDurumu.uygula(tus: HesapTusu): HesapDurumu = when (tus) {
    is HesapTusu.TumunuSil -> HesapDurumu(gecmis = gecmis)
    is HesapTusu.Rakam -> rakamGir(tus.hane)
    HesapTusu.Ondalik -> ondalikGir()
    HesapTusu.Topla -> operatorGir("+")
    HesapTusu.Cikar -> operatorGir("−")
    HesapTusu.Carp -> operatorGir("×")
    HesapTusu.Bol -> operatorGir("÷")
    HesapTusu.Yuzde -> yuzdeUygula()
    HesapTusu.ArtiEksi -> artiEksiUygula()
    HesapTusu.GeriSil -> geriSil()
    HesapTusu.Esittir -> esitle()
}

/** Geçmişten bir kayda dokunulduğunda: sanki "=" ile o sonuca ulaşılmış gibi devam edilir. */
fun HesapDurumu.gecmistenYukle(kayit: GecmisKaydi): HesapDurumu = HesapDurumu(
    tokenlar = listOf(kayit.sonuc),
    sonSonuc = kayit.sonuc,
    esittirSonrasi = true,
    gecmis = gecmis
)

private fun HesapDurumu.rakamGir(hane: Char): HesapDurumu {
    if (hataVarMi || esittirSonrasi) {
        return HesapDurumu(tokenlar = listOf(hane.toString()), gecmis = gecmis)
    }
    val son = tokenlar.last()
    return if (isOperator(son)) {
        copy(tokenlar = tokenlar + hane.toString())
    } else if (son == "0") {
        copy(tokenlar = tokenlar.dropLast(1) + hane.toString())
    } else {
        copy(tokenlar = tokenlar.dropLast(1) + (son + hane))
    }
}

private fun HesapDurumu.ondalikGir(): HesapDurumu {
    if (hataVarMi || esittirSonrasi) {
        return HesapDurumu(tokenlar = listOf("0."), gecmis = gecmis)
    }
    val son = tokenlar.last()
    return when {
        isOperator(son) -> copy(tokenlar = tokenlar + "0.")
        son.contains(".") -> this
        else -> copy(tokenlar = tokenlar.dropLast(1) + (son + "."))
    }
}

private fun HesapDurumu.operatorGir(op: String): HesapDurumu {
    if (hataVarMi) return this
    if (esittirSonrasi) {
        val baslangic = sonSonuc ?: "0"
        return copy(tokenlar = listOf(baslangic, op), sonSonuc = null, esittirSonrasi = false)
    }
    val son = tokenlar.last()
    return if (isOperator(son)) {
        // Ardışık operatörde son operatör değiştirilir.
        copy(tokenlar = tokenlar.dropLast(1) + op)
    } else {
        copy(tokenlar = tokenlar + op)
    }
}

private fun HesapDurumu.yuzdeUygula(): HesapDurumu {
    if (hataVarMi) return this
    if (esittirSonrasi) {
        val deger = sonSonuc?.let { ham -> runCatching { BigDecimal(ham) }.getOrNull() } ?: return this
        val yeni = Degerlendirici.sadeMetne(deger.divide(BigDecimal(100), MathContext(20)))
        return copy(tokenlar = listOf(yeni), sonSonuc = null, esittirSonrasi = false)
    }
    val son = tokenlar.last()
    if (isOperator(son)) return this
    val temizSon = if (son.endsWith(".")) son.dropLast(1) else son
    val bd = runCatching { BigDecimal(temizSon) }.getOrNull() ?: return this
    val yeni = Degerlendirici.sadeMetne(bd.divide(BigDecimal(100), MathContext(20)))
    return copy(tokenlar = tokenlar.dropLast(1) + yeni)
}

private fun HesapDurumu.artiEksiUygula(): HesapDurumu {
    if (hataVarMi) return this
    if (esittirSonrasi) {
        val deger = sonSonuc ?: return this
        val yeni = when {
            deger.startsWith("-") -> deger.substring(1)
            deger == "0" -> "0"
            else -> "-$deger"
        }
        return copy(tokenlar = listOf(yeni), sonSonuc = null, esittirSonrasi = false)
    }
    val son = tokenlar.last()
    if (isOperator(son) || son == "0" || son == "0.") return this
    val yeni = if (son.startsWith("-")) son.substring(1) else "-$son"
    return copy(tokenlar = tokenlar.dropLast(1) + yeni)
}

private fun HesapDurumu.geriSil(): HesapDurumu {
    if (hataVarMi) return HesapDurumu(gecmis = gecmis)
    if (esittirSonrasi) {
        val deger = sonSonuc ?: "0"
        val kisaltilmis = deger.dropLast(1)
        val yeni = if (kisaltilmis.isEmpty() || kisaltilmis == "-") "0" else kisaltilmis
        return copy(tokenlar = listOf(yeni), sonSonuc = null, esittirSonrasi = false)
    }
    val son = tokenlar.last()
    if (isOperator(son)) {
        return if (tokenlar.size == 1) this else copy(tokenlar = tokenlar.dropLast(1))
    }
    val kisaltilmis = son.dropLast(1)
    return if (kisaltilmis.isEmpty() || kisaltilmis == "-") {
        if (tokenlar.size == 1) {
            copy(tokenlar = listOf("0"))
        } else {
            copy(tokenlar = tokenlar.dropLast(1))
        }
    } else {
        copy(tokenlar = tokenlar.dropLast(1) + kisaltilmis)
    }
}

private fun HesapDurumu.esitle(): HesapDurumu {
    if (hataVarMi) return this
    val son = tokenlar.last()
    if (isOperator(son)) return this // eksik ifade, yok say
    return when (val sonucu = Degerlendirici.degerlendir(tokenlar)) {
        is DegerlendirmeSonucu.Hata -> HesapDurumu(hataVarMi = true, gecmis = gecmis)
        is DegerlendirmeSonucu.Basarili -> {
            val ifadeGosterim = tokenlar.joinToString("")
            val sonucMetni = Degerlendirici.sadeMetne(sonucu.deger)
            val yeniGecmis = (listOf(GecmisKaydi(ifadeGosterim, sonucMetni)) + gecmis)
                .take(HesapDurumu.MAKS_GECMIS)
            copy(
                tokenlar = listOf(sonucMetni),
                sonSonuc = sonucMetni,
                esittirSonrasi = true,
                gecmis = yeniGecmis
            )
        }
    }
}

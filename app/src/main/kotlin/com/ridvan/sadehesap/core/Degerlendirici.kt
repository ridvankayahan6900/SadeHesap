package com.ridvan.sadehesap.core

import java.math.BigDecimal
import java.math.MathContext

/**
 * Saf ifade değerlendirici. Android/UI bağımlılığı yoktur, doğrudan birim testlerle
 * doğrulanır. Sayılar her zaman nokta (".") ondalık ayırıcıyla, işlemler "+", "−",
 * "×", "÷" sembolleriyle temsil edilir — yerelleştirilmiş görünüm UI katmanında yapılır.
 *
 * İşlem önceliği: önce × ve ÷ (soldan sağa), sonra + ve − (soldan sağa).
 */
sealed interface DegerlendirmeSonucu {
    data class Basarili(val deger: BigDecimal) : DegerlendirmeSonucu
    data object Hata : DegerlendirmeSonucu
}

object Degerlendirici {

    /** Tüm ara işlemlerde kullanılan hassasiyet. */
    private val ISLEM_HASSASIYETI = MathContext(20)

    /** Nihai/gösterilen sayılarda en fazla bu kadar anlamlı basamak tutulur. */
    private const val GOSTERIM_BASAMAK = 12

    private fun isOperator(s: String) = s == "+" || s == "−" || s == "×" || s == "÷"

    /**
     * [tokenlar]: sayı, operatör, sayı, operatör, ... sayı biçiminde, sayıyla başlayıp
     * sayıyla bitmesi beklenir. Aksi halde (boş liste, operatörle biten, geçersiz sayı,
     * sıfıra bölme) [DegerlendirmeSonucu.Hata] döner.
     */
    fun degerlendir(tokenlar: List<String>): DegerlendirmeSonucu {
        if (tokenlar.isEmpty() || tokenlar.size % 2 == 0) return DegerlendirmeSonucu.Hata

        return try {
            // 1. geçiş: × ve ÷ soldan sağa indirgenir, sonuçta yalnız + / − ile bağlı terimler kalır.
            val terimler = mutableListOf<BigDecimal>()
            val baglaclar = mutableListOf<String>()
            var mevcut = tokenSayiyaCevir(tokenlar[0]) ?: return DegerlendirmeSonucu.Hata

            var i = 1
            while (i < tokenlar.size) {
                val op = tokenlar[i]
                val sayi = tokenSayiyaCevir(tokenlar[i + 1]) ?: return DegerlendirmeSonucu.Hata
                when (op) {
                    "×" -> mevcut = mevcut.multiply(sayi, ISLEM_HASSASIYETI)
                    "÷" -> {
                        if (sayi.compareTo(BigDecimal.ZERO) == 0) return DegerlendirmeSonucu.Hata
                        mevcut = mevcut.divide(sayi, ISLEM_HASSASIYETI)
                    }
                    "+", "−" -> {
                        terimler += mevcut
                        baglaclar += op
                        mevcut = sayi
                    }
                    else -> return DegerlendirmeSonucu.Hata
                }
                i += 2
            }
            terimler += mevcut

            // 2. geçiş: + ve − soldan sağa toplanır.
            var sonuc = terimler[0]
            for (j in baglaclar.indices) {
                sonuc = if (baglaclar[j] == "+") {
                    sonuc.add(terimler[j + 1], ISLEM_HASSASIYETI)
                } else {
                    sonuc.subtract(terimler[j + 1], ISLEM_HASSASIYETI)
                }
            }
            DegerlendirmeSonucu.Basarili(sonuc)
        } catch (e: ArithmeticException) {
            DegerlendirmeSonucu.Hata
        } catch (e: NumberFormatException) {
            DegerlendirmeSonucu.Hata
        }
    }

    private fun tokenSayiyaCevir(ham: String): BigDecimal? {
        if (ham.isEmpty() || ham == "-" || ham == ".") return null
        var s = ham
        if (s.endsWith(".")) s = s.dropLast(1)
        if (s.startsWith("-.")) s = "-0" + s.substring(1)
        if (s.startsWith(".")) s = "0$s"
        if (s.isEmpty() || s == "-") return null
        return try {
            BigDecimal(s)
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Bir [BigDecimal] değeri en fazla 12 anlamlı basamağa yuvarlar, gereksiz sıfırları
     * kırpar ve bilimsel gösterim kullanmadan (düz metin) döndürür.
     */
    fun sadeMetne(deger: BigDecimal): String {
        if (deger.compareTo(BigDecimal.ZERO) == 0) return "0"
        val yuvarlanan = deger.round(MathContext(GOSTERIM_BASAMAK))
        val sadelesmis = yuvarlanan.stripTrailingZeros()
        val duzMetin = sadelesmis.toPlainString()
        return if (duzMetin == "-0") "0" else duzMetin
    }

    /**
     * [deger], [sadeMetne] ile metne çevrilirken GOSTERIM_BASAMAK basamağından fazla anlamlı
     * basamağının kırpılıp kırpıldığını (yani gösterilen metnin gerçek değerin YAKLAŞIK bir
     * temsili olup olmadığını) bildirir — UI'nın kullanıcıya bir yuvarlama işareti (örn. "≈")
     * gösterebilmesi içindir. [sadeMetne]'nin kendi dönüş tipini değiştirmez: o metin hâlâ
     * ham/ayrıştırılabilir sayı olarak tokenlar içinde saklanıp tekrar hesaplara girer.
     */
    fun yuvarlandiMi(deger: BigDecimal): Boolean =
        deger.compareTo(BigDecimal.ZERO) != 0 && deger.precision() > GOSTERIM_BASAMAK
}

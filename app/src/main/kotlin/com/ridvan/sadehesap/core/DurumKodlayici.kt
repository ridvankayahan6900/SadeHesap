package com.ridvan.sadehesap.core

/**
 * [HesapDurumu]'nu rememberSaveable için tek bir String'e kodlar / geri çözer (kalıcı
 * depolama yok). UI'dan ayrı, saf Kotlin olduğu için birim testlerle doğrulanır.
 *
 * Ayırıcılar kontrol karakterleridir: kullanıcı girdisinde (rakam/operatör/geçmiş metni)
 * hiçbir zaman oluşmaz, bu yüzden güvenlidir.
 */
object DurumKodlayici {
    private const val TOKEN_AYIRICI = "\u0001" // tokenlar listesi içinde
    private const val KAYIT_AYIRICI = "\u0002" // geçmiş kayıtları arasında
    private const val KAYIT_ALAN_AYIRICI = "\u0003" // bir kaydın ifade/sonuç alanları arasında
    private const val BOS_ISARETI = "\u0004" // null alan işareti
    private const val ALAN_AYIRICI = "\u0005" // üst düzey alanlar arasında

    fun kodla(durum: HesapDurumu): String = listOf(
        durum.tokenlar.joinToString(TOKEN_AYIRICI),
        durum.sonSonuc ?: BOS_ISARETI,
        if (durum.hataVarMi) "1" else "0",
        if (durum.esittirSonrasi) "1" else "0",
        durum.gecmis.joinToString(KAYIT_AYIRICI) { "${it.ifade}$KAYIT_ALAN_AYIRICI${it.sonuc}" },
        if (durum.sonucYuvarlandiMi) "1" else "0",
        durum.sonIfade ?: BOS_ISARETI
    ).joinToString(ALAN_AYIRICI)

    fun coz(metin: String): HesapDurumu {
        val alanlar = metin.split(ALAN_AYIRICI)
        fun alan(i: Int) = alanlar.getOrNull(i) ?: ""
        val tokenlar = alan(0).split(TOKEN_AYIRICI).filter { it.isNotEmpty() }.ifEmpty { listOf("0") }
        val gecmis = alan(4).takeIf { it.isNotEmpty() }
            ?.split(KAYIT_AYIRICI)
            ?.mapNotNull { kayit ->
                val parcalar = kayit.split(KAYIT_ALAN_AYIRICI)
                if (parcalar.size == 2) GecmisKaydi(parcalar[0], parcalar[1]) else null
            }
            ?: emptyList()
        return HesapDurumu(
            tokenlar = tokenlar,
            sonSonuc = alan(1).takeIf { it != BOS_ISARETI && it.isNotEmpty() },
            hataVarMi = alan(2) == "1",
            esittirSonrasi = alan(3) == "1",
            gecmis = gecmis,
            sonucYuvarlandiMi = alan(5) == "1",
            sonIfade = alan(6).takeIf { it != BOS_ISARETI && it.isNotEmpty() }
        )
    }
}

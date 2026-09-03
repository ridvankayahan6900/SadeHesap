package com.ridvan.sadehesap.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridvan.sadehesap.R
import com.ridvan.sadehesap.core.GecmisKaydi
import com.ridvan.sadehesap.core.HesapDurumu
import com.ridvan.sadehesap.core.HesapTusu
import com.ridvan.sadehesap.core.gecmistenYukle
import com.ridvan.sadehesap.core.uygula
import kotlinx.coroutines.launch

// Kontrol karakterleri: kullanici girdisinde (rakam/operator/gecmis metni) hicbir zaman
// olusmaz, bu yuzden ayirici olarak guvenlidir. Tek bir String'e kodlanir (Bundle'da
// String her zaman guvenle saklanabilir).
private const val TOKEN_AYIRICI = "" // tokenlar listesi icinde
private const val KAYIT_AYIRICI = "" // gecmis kayitlari arasinda
private const val KAYIT_ALAN_AYIRICI = "" // bir kaydin ifade/sonuc alanlari arasinda
private const val BOS_ISARETI = "" // sonSonuc == null isareti
private const val ALAN_AYIRICI = "" // 5 ust duzey alan arasinda

/** [HesapDurumu]'nu rememberSaveable ile tek bir String'e kodlar (kalici depolama yok). */
private val HesapDurumuSaver: Saver<HesapDurumu, String> = Saver(
    save = { durum ->
        listOf(
            durum.tokenlar.joinToString(TOKEN_AYIRICI),
            durum.sonSonuc ?: BOS_ISARETI,
            if (durum.hataVarMi) "1" else "0",
            if (durum.esittirSonrasi) "1" else "0",
            durum.gecmis.joinToString(KAYIT_AYIRICI) { "${it.ifade}$KAYIT_ALAN_AYIRICI${it.sonuc}" }
        ).joinToString(ALAN_AYIRICI)
    },
    restore = { metin ->
        val alanlar = metin.split(ALAN_AYIRICI)
        val tokenlar = alanlar[0].split(TOKEN_AYIRICI).filter { it.isNotEmpty() }.ifEmpty { listOf("0") }
        val sonSonuc = alanlar[1].takeIf { it != BOS_ISARETI }
        val hataVarMi = alanlar[2] == "1"
        val esittirSonrasi = alanlar[3] == "1"
        val gecmis = alanlar[4].takeIf { it.isNotEmpty() }
            ?.split(KAYIT_AYIRICI)
            ?.mapNotNull { kayit ->
                val parcalar = kayit.split(KAYIT_ALAN_AYIRICI)
                if (parcalar.size == 2) GecmisKaydi(parcalar[0], parcalar[1]) else null
            }
            ?: emptyList()
        HesapDurumu(tokenlar, sonSonuc, hataVarMi, esittirSonrasi, gecmis)
    }
)

private fun tokenOperatorMu(tok: String) = tok == "+" || tok == "−" || tok == "×" || tok == "÷"

@Composable
fun HesapEkrani(modifier: Modifier = Modifier) {
    var durum by rememberSaveable(stateSaver = HesapDurumuSaver) { mutableStateOf(HesapDurumu()) }

    val ondalikAyiraci = stringResource(R.string.btn_decimal)
    val hataMetni = stringResource(R.string.error_result)
    val kopyalandiMesaji = stringResource(R.string.msg_copied)

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val kapsam = rememberCoroutineScope()
    val yapilandirma = LocalConfiguration.current
    val yatayMi = yapilandirma.screenWidthDp > yapilandirma.screenHeightDp

    fun gosterMetni(ham: String): String = ham.replace(".", ondalikAyiraci)

    val tamIfade = remember(durum.tokenlar) {
        durum.tokenlar.joinToString("") { tok ->
            if (tokenOperatorMu(tok)) " $tok " else gosterMetni(tok)
        }.trim()
    }

    val anaGosterim = when {
        durum.hataVarMi -> hataMetni
        durum.esittirSonrasi -> gosterMetni(durum.sonSonuc ?: "0")
        else -> tamIfade
    }

    // Yalnizca "=" sonrasi gosterilen sonuc icin anlamli: Degerlendirici.sadeMetne'nin
    // GOSTERIM_BASAMAK sinirindan dolayi bu deger yuvarlanmis/yaklasik mi?
    val sonucYuvarlandiMi = durum.esittirSonrasi && durum.sonucYuvarlandiMi

    val ustSatir = if (durum.esittirSonrasi) {
        durum.gecmis.firstOrNull { it.sonuc == durum.sonSonuc }
            ?.let { kayit -> gosterMetni(kayit.ifade) + " =" }
            ?: ""
    } else {
        ""
    }

    val kopyalanacakDeger: String? = when {
        durum.hataVarMi -> null
        durum.esittirSonrasi -> durum.sonSonuc
        else -> durum.tokenlar.last().takeUnless { tokenOperatorMu(it) }
    }

    fun sonucuKopyala() {
        val deger = kopyalanacakDeger ?: return
        clipboardManager.setText(AnnotatedString(gosterMetni(deger)))
        kapsam.launch { snackbarHostState.showSnackbar(kopyalandiMesaji) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { icBoslukDegerleri ->
        if (yatayMi) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(icBoslukDegerleri)
            ) {
                GecmisPaneli(
                    gecmis = durum.gecmis,
                    gosterMetni = ::gosterMetni,
                    onKayitTikla = { durum = durum.gecmistenYukle(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                VerticalDivider()
                HesapGovde(
                    ustSatir = ustSatir,
                    anaGosterim = anaGosterim,
                    sonucYuvarlandiMi = sonucYuvarlandiMi,
                    onUzunBasKopyala = ::sonucuKopyala,
                    onTus = { durum = durum.uygula(it) },
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(icBoslukDegerleri)
            ) {
                GecmisPaneli(
                    gecmis = durum.gecmis,
                    gosterMetni = ::gosterMetni,
                    onKayitTikla = { durum = durum.gecmistenYukle(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.8f)
                )
                HorizontalDivider()
                HesapGovde(
                    ustSatir = ustSatir,
                    anaGosterim = anaGosterim,
                    sonucYuvarlandiMi = sonucYuvarlandiMi,
                    onUzunBasKopyala = ::sonucuKopyala,
                    onTus = { durum = durum.uygula(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                )
            }
        }
    }
}

@Composable
private fun HesapGovde(
    ustSatir: String,
    anaGosterim: String,
    sonucYuvarlandiMi: Boolean,
    onUzunBasKopyala: () -> Unit,
    onTus: (HesapTusu) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        EkranAlani(
            ustSatir = ustSatir,
            anaGosterim = anaGosterim,
            sonucYuvarlandiMi = sonucYuvarlandiMi,
            onUzunBasKopyala = onUzunBasKopyala,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        TusTakimi(
            onTus = onTus,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.7f)
        )
    }
}

@Composable
private fun EkranAlani(
    ustSatir: String,
    anaGosterim: String,
    sonucYuvarlandiMi: Boolean,
    onUzunBasKopyala: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ifadeAciklama = stringResource(R.string.cd_expression)
    val sonucAciklama = stringResource(R.string.cd_result)
    val yuvarlandiOneki = stringResource(R.string.label_rounded_prefix)
    val yuvarlandiAciklamaEki = stringResource(R.string.cd_rounded_suffix)

    // "≈" öneki yalnizca gorsel etiket icin: kopyalanan/hesaba devam eden gercek deger
    // (kopyalanacakDeger, durum.sonSonuc) bu metinden degil dogrudan HesapDurumu'ndan
    // gelir, bu yuzden onek onlari etkilemez.
    val anaGosterimMetni = if (sonucYuvarlandiMi) "$yuvarlandiOneki$anaGosterim" else anaGosterim
    val sonucAciklamaMetni = if (sonucYuvarlandiMi) {
        "$sonucAciklama: $anaGosterim$yuvarlandiAciklamaEki"
    } else {
        "$sonucAciklama: $anaGosterim"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        // Tek satir + yatay kaydirma (reverseScrolling=true): ifade kisa ise sag kenara
        // yasli gorunur (bir kaydirma cubugu gerekmez); uzunsa varsayilan olarak SONU
        // (en son basilan "=" ve ona en yakin basamaklar) gorunur, kullanici sola kaydirip
        // basini gorebilir. maxLines/Ellipsis ile hicbir basamak sessizce kirpilmaz.
        Text(
            text = ustSatir,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState(), reverseScrolling = true)
                .semantics { contentDescription = "$ifadeAciklama: $ustSatir" }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onUzunBasKopyala
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = sonucAciklamaMetni
                    liveRegion = LiveRegionMode.Polite
                }
                .padding(vertical = 12.dp)
        ) {
            // Ana sonuc: ayni tek-satir + yatay kaydirma korumasi (bkz. yukaridaki not) —
            // önceki sabit 40sp + Ellipsis kombinasyonu cok uzun sonuclarda anlamli son
            // basamaklari gorunmez kiliyordu (inceleme bulgusu), artik hicbiri kaybolmuyor.
            Text(
                text = anaGosterimMetni,
                style = MaterialTheme.typography.displaySmall,
                fontSize = 40.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState(), reverseScrolling = true)
            )
        }
    }
}

@Composable
private fun GecmisPaneli(
    gecmis: List<GecmisKaydi>,
    gosterMetni: (String) -> String,
    onKayitTikla: (GecmisKaydi) -> Unit,
    modifier: Modifier = Modifier
) {
    val panelAciklama = stringResource(R.string.cd_history_panel)
    val bosMetin = stringResource(R.string.label_history_empty)
    val baslik = stringResource(R.string.label_history_title)

    Column(
        modifier = modifier
            .padding(12.dp)
            .semantics { contentDescription = panelAciklama }
    ) {
        Text(
            text = baslik,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (gecmis.isEmpty()) {
            Text(
                text = bosMetin,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(gecmis) { kayit ->
                    GecmisSatiri(kayit = kayit, gosterMetni = gosterMetni, onTikla = { onKayitTikla(kayit) })
                }
            }
        }
    }
}

@Composable
private fun GecmisSatiri(
    kayit: GecmisKaydi,
    gosterMetni: (String) -> String,
    onTikla: () -> Unit
) {
    val ifadeGosterim = gosterMetni(kayit.ifade)
    val sonucGosterim = gosterMetni(kayit.sonuc)
    val satirAciklama = stringResource(R.string.cd_history_item, ifadeGosterim, sonucGosterim)

    Surface(
        onClick = onTikla,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            // clearAndSetSemantics DEGIL: o, Surface'in kendi onClick'inin ekledigi
            // ACTION_CLICK/tiklanabilirlik semantigini SILERDI (inceleme bulgusu — TalkBack
            // ile bu satir etkinlestirilemez hale geliyordu). semantics(mergeDescendants)
            // yerine mevcut semantigin UZERINE birlesir, tiklama eylemi korunur.
            .semantics(mergeDescendants = true) {
                contentDescription = satirAciklama
                role = Role.Button
            },
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
            Text(
                text = ifadeGosterim,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sonucGosterim,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private enum class TusRenk { RAKAM, ISLEM, TEMIZLE, ESIT, IKINCIL }

@Composable
private fun TusTakimi(onTus: (HesapTusu) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TusSatiri(Modifier.weight(1f)) {
            HesapTusButonu(
                stringResource(R.string.btn_clear),
                stringResource(R.string.cd_clear),
                TusRenk.TEMIZLE,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.TumunuSil) }
            HesapTusButonu(
                stringResource(R.string.btn_backspace),
                stringResource(R.string.cd_backspace),
                TusRenk.TEMIZLE,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.GeriSil) }
            HesapTusButonu(
                stringResource(R.string.btn_percent),
                stringResource(R.string.cd_percent),
                TusRenk.IKINCIL,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Yuzde) }
            HesapTusButonu(
                stringResource(R.string.btn_divide),
                stringResource(R.string.cd_divide),
                TusRenk.ISLEM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Bol) }
        }
        TusSatiri(Modifier.weight(1f)) {
            HesapTusButonu(
                stringResource(R.string.btn_digit_7),
                stringResource(R.string.cd_digit, "7"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('7')) }
            HesapTusButonu(
                stringResource(R.string.btn_digit_8),
                stringResource(R.string.cd_digit, "8"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('8')) }
            HesapTusButonu(
                stringResource(R.string.btn_digit_9),
                stringResource(R.string.cd_digit, "9"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('9')) }
            HesapTusButonu(
                stringResource(R.string.btn_multiply),
                stringResource(R.string.cd_multiply),
                TusRenk.ISLEM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Carp) }
        }
        TusSatiri(Modifier.weight(1f)) {
            HesapTusButonu(
                stringResource(R.string.btn_digit_4),
                stringResource(R.string.cd_digit, "4"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('4')) }
            HesapTusButonu(
                stringResource(R.string.btn_digit_5),
                stringResource(R.string.cd_digit, "5"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('5')) }
            HesapTusButonu(
                stringResource(R.string.btn_digit_6),
                stringResource(R.string.cd_digit, "6"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('6')) }
            HesapTusButonu(
                stringResource(R.string.btn_subtract),
                stringResource(R.string.cd_subtract),
                TusRenk.ISLEM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Cikar) }
        }
        TusSatiri(Modifier.weight(1f)) {
            HesapTusButonu(
                stringResource(R.string.btn_digit_1),
                stringResource(R.string.cd_digit, "1"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('1')) }
            HesapTusButonu(
                stringResource(R.string.btn_digit_2),
                stringResource(R.string.cd_digit, "2"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('2')) }
            HesapTusButonu(
                stringResource(R.string.btn_digit_3),
                stringResource(R.string.cd_digit, "3"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('3')) }
            HesapTusButonu(
                stringResource(R.string.btn_add),
                stringResource(R.string.cd_add),
                TusRenk.ISLEM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Topla) }
        }
        TusSatiri(Modifier.weight(1f)) {
            HesapTusButonu(
                stringResource(R.string.btn_plus_minus),
                stringResource(R.string.cd_plus_minus),
                TusRenk.IKINCIL,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.ArtiEksi) }
            HesapTusButonu(
                stringResource(R.string.btn_digit_0),
                stringResource(R.string.cd_digit, "0"),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Rakam('0')) }
            HesapTusButonu(
                stringResource(R.string.btn_decimal),
                stringResource(R.string.cd_decimal),
                TusRenk.RAKAM,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Ondalik) }
            HesapTusButonu(
                stringResource(R.string.btn_equals),
                stringResource(R.string.cd_equals),
                TusRenk.ESIT,
                Modifier.weight(1f)
            ) { onTus(HesapTusu.Esittir) }
        }
    }
}

@Composable
private fun TusSatiri(modifier: Modifier = Modifier, icerik: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = icerik
    )
}

@Composable
private fun HesapTusButonu(
    etiket: String,
    aciklama: String,
    renk: TusRenk,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (kapsayiciRenk, icerikRenk) = when (renk) {
        TusRenk.RAKAM -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TusRenk.ISLEM -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TusRenk.TEMIZLE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TusRenk.ESIT -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        TusRenk.IKINCIL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .fillMaxHeight()
            // clearAndSetSemantics DEGIL (bkz. GecmisSatiri'ndeki ayni not) — Surface'in
            // tiklanabilirlik eylemini korumak icin semantics(mergeDescendants) kullanilir.
            .semantics(mergeDescendants = true) {
                contentDescription = aciklama
                role = Role.Button
            },
        shape = MaterialTheme.shapes.large,
        color = kapsayiciRenk,
        contentColor = icerikRenk
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = etiket,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

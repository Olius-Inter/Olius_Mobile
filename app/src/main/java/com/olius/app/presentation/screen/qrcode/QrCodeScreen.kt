package com.olius.app.presentation.screen.qrcode

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.olius.app.R
import com.olius.app.presentation.theme.DecorativeBackground
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusLaranjaVivo
import com.olius.app.presentation.theme.OliusTextoCard
import com.olius.app.presentation.theme.OliusTituloMarrom
import com.olius.app.presentation.theme.OliusTituloMarromClaro
import com.olius.app.presentation.theme.OliusSuperficie

private val StepGray = Color(0xFF636363)

/**
 * "Mobile / identificação / Cidadão" (Figma): QR com o token do usuário para o
 * PEV escanear. O QR guarda só o qrToken (POST /qr/resolutions do lado de quem lê).
 */
@Composable
fun QrCodeScreen(onBack: () -> Unit, viewModel: QrCodeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {
        DecorativeBackground(isRow = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            Text(
                "← Voltar",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OliusTextoCard,
                modifier = Modifier
                    .align(Alignment.Start)
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(24.dp))
            OliusLogo()
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(OliusSuperficie)
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Identifique-se", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = OliusTituloMarrom)
                Spacer(Modifier.height(24.dp))
                // Fundo branco fixo: o QR precisa de contraste para ser lido, mesmo no tema escuro.
                Box(
                    Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val content = state.content
                    when {
                        state.isLoading -> CircularProgressIndicator(color = OliusAmarelo)
                        content == null -> Text(
                            "QR Code indisponível. Verifique sua conexão.",
                            fontSize = 13.sp,
                            color = StepGray,
                            textAlign = TextAlign.Center
                        )
                        else -> {
                            val bitmap = remember(content) { qrBitmap(content, OliusTituloMarromClaro.toArgb()) }
                            Image(bitmap = bitmap, contentDescription = "QR Code de identificação", modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Step(number = 1, text = "Aponte para o coletor")
                StepArrow()
                Step(number = 2, text = "Informe a Quantidade de Oleo")
                StepArrow()
                Step(number = 3, text = "Pronto!")
            }
        }
    }
}

@Composable
private fun OliusLogo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo_sem_fundo), contentDescription = null, modifier = Modifier.size(width = 49.dp, height = 69.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Olius",
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.verticalGradient(listOf(Color(0xFFFCDB03), OliusLaranjaVivo))
            )
        )
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(1.dp, StepGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), fontSize = 21.sp, color = StepGray)
        }
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 14.sp, color = StepGray)
    }
}

@Composable
private fun StepArrow() {
    Text("↓", fontSize = 14.sp, color = StepGray, modifier = Modifier.padding(vertical = 2.dp))
}

/** Gera o bitmap do QR com a ZXing (só encoder, sem câmera). */
private fun qrBitmap(content: String, color: Int, size: Int = 720): ImageBitmap {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.CHARACTER_SET to "UTF-8")
    )
    val pixels = IntArray(size * size) { index ->
        if (matrix[index % size, index / size]) color else android.graphics.Color.TRANSPARENT
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}

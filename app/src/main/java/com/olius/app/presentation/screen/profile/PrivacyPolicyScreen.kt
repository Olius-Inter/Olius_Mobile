package com.olius.app.presentation.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olius.app.data.remote.ApiConfig
import com.olius.app.presentation.components.BOTTOM_NAV_CONTENT_PADDING
import com.olius.app.presentation.theme.OliusFundoTela
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoPrimarioClaro
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusTituloMarrom

/** Uma seção numerada da política (título + parágrafos). */
data class PolicySection(val title: String, val paragraphs: List<String>)

/** Data mostrada no rodapé. Atualize junto com o texto. */
const val PRIVACY_POLICY_UPDATED_AT = "31/10/2026"

/**
 * Texto da Política de Privacidade. O Figma ("Perfil-V2", nó 3941:43990) só tem
 * os três títulos e "lorem ipsum"; este texto descreve o que o app realmente
 * coleta hoje. RASCUNHO: revisar com a equipe antes de publicar na loja.
 */
val PRIVACY_POLICY_SECTIONS: List<PolicySection> = listOf(
    PolicySection(
        "1. Dados que coletamos",
        listOf(
            "Cadastro: nome, e-mail, telefone e senha. Cidadãos informam o CPF; estabelecimentos informam o " +
                "CNPJ, o tipo de estabelecimento e o endereço. A senha é guardada de forma cifrada e ninguém da " +
                "equipe Olius consegue lê-la. Se você entrar com a conta Google, recebemos o nome e o e-mail dela.",
            "Uso do app: as entregas e coletas de óleo (volume, data e ponto), os pontos e a posição no ranking, " +
                "os pedidos de coleta e, se você virar Coletor, o endereço do seu ponto.",
            "Localização: com a sua permissão, o app usa a localização do aparelho para mostrar os pontos de " +
                "coleta mais próximos e calcular a distância até eles. Ela não é guardada no seu cadastro.",
            "Conversas com a OlivIA: as mensagens que você envia à assistente e as respostas dela."
        )
    ),
    PolicySection(
        "2. Como usamos seus dados",
        listOf(
            "Para criar e proteger a sua conta, mostrar o seu histórico, calcular os seus pontos, o impacto " +
                "ambiental do óleo descartado e a sua posição no ranking.",
            "Para organizar as coletas entre estabelecimentos, coletores e a equipe Olius.",
            "Para a OlivIA responder às suas perguntas com base nos seus próprios dados (entregas, pontos e " +
                "pontos de coleta próximos).",
            "Para falar com você sobre a sua conta, como no envio do código de recuperação de senha."
        )
    ),
    PolicySection(
        "3. Compartilhamento dos seus dados",
        listOf(
            "Não vendemos os seus dados.",
            "Ficam visíveis para outros usuários: o seu nome e a sua cidade no ranking e, se você for " +
                "estabelecimento ou Coletor aprovado, o endereço do ponto de coleta no mapa.",
            "Usamos serviços de terceiros para o app funcionar: hospedagem em nuvem, provedores de inteligência " +
                "artificial que processam as mensagens enviadas à OlivIA, o Google (login com a conta Google), o " +
                "ViaCEP (recebe só o CEP digitado) e o OpenStreetMap (desenho do mapa).",
            "Também podemos compartilhar dados quando a lei ou uma ordem judicial exigir."
        )
    ),
    PolicySection(
        "4. Seus direitos",
        listOf(
            "Pela LGPD (Lei nº 13.709/2018), você pode pedir acesso, correção ou exclusão dos seus dados, e " +
                "saber com quem eles foram compartilhados. Nome, e-mail e telefone podem ser corrigidos em " +
                "\"Editar informações de perfil\".",
            "Para qualquer pedido ou dúvida, escreva para ${ApiConfig.SUPPORT_EMAIL}."
        )
    )
)

/**
 * Política de Privacidade (Figma "Perfil-V2" → "Política de privacidade"): voltar,
 * título e as seções numeradas, com rolagem. Fica dentro do app (funciona sem
 * internet), em vez de abrir uma página no navegador.
 */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(OliusFundoTela)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = BOTTOM_NAV_CONTENT_PADDING)
        ) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Voltar", tint = OliusTextoPrimarioClaro)
                }
                Text(
                    "Política de privacidade",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OliusTituloMarrom,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 28.dp)
                )
            }
            Spacer(Modifier.height(28.dp))

            PRIVACY_POLICY_SECTIONS.forEach { section ->
                Text(section.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OliusTextoPrimario)
                Spacer(Modifier.height(10.dp))
                section.paragraphs.forEach { paragraph ->
                    Text(paragraph, fontSize = 14.sp, lineHeight = 21.sp, color = OliusTextoSecundario)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(14.dp))
            }
            Text("Última atualização: $PRIVACY_POLICY_UPDATED_AT", fontSize = 12.sp, color = OliusTextoSecundario)
            Spacer(Modifier.height(16.dp))
        }
    }
}

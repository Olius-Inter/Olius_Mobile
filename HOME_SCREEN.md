# Tela Home — o que foi feito

Implementação completa da tela Home (pós-login) do app Olius, a partir das imagens de
referência em `app/src/main/res/drawable/` (`referencia_home.png`, `ref_home_cell_1.png`,
`ref_home_cell_2.png`, `ref_register_oil.png`, `ref_solicitar_coleta.png`, `ref_perfil.png`,
`ref_notification.png`). Testado ponta a ponta num emulador (Pixel 6 / API do projeto).

## Arquivos

- `presentation/screen/home/HomeUiState.kt` — estado da tela (`HomeUiState`) + modelos de
  notificação (`NotificationItem`, `NotificationCategory`, `NotificationFilter`).
- `presentation/screen/home/HomeViewModel.kt` — `StateFlow<HomeUiState>` com dados de exemplo
  (iguais aos das referências) + todos os eventos da tela (abrir/fechar popups, editar campos).
- `presentation/screen/home/HomeScreen.kt` — toda a UI: header, saudação, botões de ação, grid
  de indicadores, card de nível de óleo (tanque), card de nível de sustentabilidade (barra de
  progresso), os 2 popups (`Dialog`) e os 2 bottom sheets animados (`SlideUpDialog`).
- `presentation/screen/main/MainScaffoldScreen.kt` — casca do app pós-login: menu inferior fixo
  (`OliusBottomNavBar`) + `NavHost` próprio com as 5 abas.
- `presentation/navigation/Routes.kt` / `NavGraph.kt` — rota `Home` trocada por `Main`, que agora
  aponta pro `MainScaffoldScreen` em vez de chamar `HomeScreen()` direto.
- `presentation/theme/Color.kt` — cores novas usadas só na Home (ver seção "Cores" abaixo).

## O que funciona de verdade vs. o que é placeholder

**UI pronta e funcional** (abre popup, atualiza estado, anima):
- Botão "Registrar óleo" abre o popup central.
- Botão "Solicitar coleta" abre o popup central **somente se habilitado** (ver regra abaixo);
  volume inicial do formulário já vem preenchido com o óleo acumulado atual.
- Sino de notificações e avatar de perfil abrem os bottom sheets, com a mesma animação de baixo
  pra cima dos cards de login/cadastro (slide + fade).
- Filtro de notificações (Todas/Coletas/Frotas) filtra a lista de verdade.
- Menu inferior troca de aba de verdade (NavHost próprio) e anima a cor do ícone.
- Tanque de óleo e barra de nível animam a % de preenchimento (`animateFloatAsState`).

**Funções vazias de propósito** (o pedido foi deixar prontas pra você implementar a lógica
depois, sem travar a UI):
- `HomeViewModel.onConfirmRegisterOil()` — não persiste nada ainda (só fecha o popup).
- `HomeViewModel.onConfirmRequestPickup()` — idem.
- `HomeViewModel.estimateWaterPreserved(volume)` / `estimateCo2Avoided(volume)` — retornam
  sempre `0.0`; é onde entra a fórmula de conversão óleo→água/CO² quando ela existir.
- `HomeViewModel.onLogoutClick()` — fecha o sheet e dispara o callback de navegação
  (`onLogout`), mas não chama o `LogoutUseCases` que já existe no projeto — fica a seu critério
  ligar isso.
- `HomeViewModel.onAddAccountClick()` / `onNotificationActionClick()` — vazias (`Unit`).
- As 4 abas do menu que não são Home (Histórico, Serviços, Chat, Cofrinho) são só um placeholder
  "em breve" (`PlaceholderTabScreen` em `MainScaffoldScreen.kt`) — a navegação já funciona,
  falta só substituir pelo conteúdo real de cada uma quando existir.
- `ProfileAvatar` usa um círculo amarelo com ícone de pessoa — não existe lib de imagem (Coil)
  no projeto ainda, então a foto real do usuário (`HomeUiState.profileImageUrl`) não é carregada.

## Regra do botão "Solicitar coleta"

Only uma regra foi implementada, exatamente como pedido:

```kotlin
// HomeViewModel.kt
private fun hasRegisteredOilThisMonth(state: HomeUiState) = state.oilRegisteredThisMonthLiters > 0.0

// TODO: valor mínimo ainda não definido pelo negócio.
private fun meetsMinimumOilRequirement(state: HomeUiState): Boolean = true

fun canRequestPickup(state: HomeUiState) =
    hasRegisteredOilThisMonth(state) && meetsMinimumOilRequirement(state)
```

Enquanto `oilRegisteredThisMonthLiters` for `0.0`, o botão fica cinza/inativo (mesma cor da
referência). Quando o valor mínimo de óleo for decidido, é só implementar a comparação dentro de
`meetsMinimumOilRequirement` — o resto da tela (cor do botão, `enabled`, etc.) já reage a esse
retorno automaticamente.

O selo verde "Pronto para coleta" **não** está amarrado a essa regra — nas duas imagens de
referência ele aparece sempre, mesmo com o tanque vazio (`ref_home_cell_1.png` mostra "0L" com o
selo do mesmo jeito). Por isso ele é controlado por um campo separado do estado
(`HomeUiState.showPickupReadyBadge`, `true` por padrão) em vez de ser calculado a partir do
volume — assim não inventamos uma regra de negócio que ninguém pediu ainda.

## Tanque de óleo (gota cinza → branca)

Pedido: a gota (`oil_drop.png`) fica **cinza** com o tanque vazio, **branca** com o tanque cheio,
e **metade cinza / metade branca** quando estiver na metade — porque vai ter uma animação de
"enchendo".

Implementação (`OilTankGauge` em `HomeScreen.kt`): desenha a gota cinza normalmente, e por cima
desenha uma **segunda cópia branca da mesma gota**, recortada (`clipToBounds`) na mesma altura do
preenchimento amarelo do tanque. Como as duas gotas ficam exatamente sobrepostas, o efeito visual
é uma única gota que vai "pintando de branco de baixo pra cima" conforme o tanque enche — sem
precisar de nenhum asset novo, e a animação (`animateFloatAsState`, 900ms) já funciona.

A fração de preenchimento vem de `HomeUiState.oilTankFraction`
(`oilAccumuladoLiters / minimumForPickupLiters`, limitado entre 0 e 1) — troque a fonte desse
cálculo quando os dados reais existirem, a UI não precisa mudar.

## Nível de sustentabilidade

`level.png` é usado como selo (ícone de medalha) dentro de um quadrado escuro, do lado do "lv. N".
A barra de progresso abaixo (`LevelProgressBar`) enche de amarelo de acordo com
`HomeUiState.levelProgressFraction` (`currentLevelProgressLiters / nextLevelTargetLiters`),
também animada.

## Menu inferior fixo

`MainScaffoldScreen` (novo arquivo) virou a "casca" do app depois do login: um `Scaffold` com
`bottomBar` fixo + um `NavHost` próprio pras 5 abas (Home, Histórico, Serviços, Chat, Cofrinho).
O `NavGraph.kt` principal agora navega pra rota `Main` (em vez de `Home` direto) depois do
login/cadastro.

A "animação trocando a cor do ícone" foi feita fazendo **crossfade entre os dois PNGs** de cada
aba (`nome.png` e `nome_selected.png`) — em vez de aplicar tint programático — porque os ícones
já vêm coloridos nos arquivos (o normal já é uma cor e o `_selected` é amarelo), então trocar a
`alpha` de um pro outro reproduz a troca de cor com uma animação suave (250ms) sem risco de tintar
errado um ícone que não é uma máscara monocromática.

**Atenção — possível inconsistência nos assets**: ao inspecionar os PNGs, `home.png` e
`home_selected.png` pareciam idênticos (os dois já amarelos), e `pig.png` também já vinha amarelo
enquanto `pig_selected.png` aparentava estar vazio/transparente — o oposto do padrão dos outros 3
pares (`chat`, `history`, `vector`), onde o arquivo sem `_selected` é claro/neutro e o
`_selected` é amarelo. Não alterei nada nos ícones (só usei os arquivos como estão, no padrão
`nome.png` = normal / `nome_selected.png` = selecionado); no emulador o efeito ficou aceitável,
mas vale conferir esses dois arquivos se notar o ícone de Home ou Cofrinho não mudando de cor como
esperado.

O menu continua sempre visível **mesmo com os popups/bottom sheets abertos por cima** — os
popups e sheets cobrem a tela inteira (via `Dialog`, que renderiza numa window separada por cima
de tudo, inclusive do menu), então visualmente eles tampam o menu como nas referências
(`ref_perfil.png`/`ref_notification.png` não mostram o menu por trás do sheet), mas a
composable do menu em si nunca é destruída — ela é "fixa em todas as telas" no sentido pedido
(sobrevive à troca de aba).

## Cores novas (`presentation/theme/Color.kt`)

Extraídas por amostragem de pixel direto das imagens de referência (não são chute):

| Cor | Valor | Uso |
|---|---|---|
| `OliusLaranja` | `#F57A04` | "Solicitar coleta" habilitado |
| `OliusBotaoInativoFundo` / `OliusBotaoInativoTexto` | `#E3E3E3` / `#737A7B` | "Solicitar coleta" desabilitado |
| `OliusNavBarFundo` | `#676767` | fundo do menu inferior + fundo do selo de nível |
| `OliusCardAguaFundo` / `OliusCardOleoFundo` / `OliusCardVerdeFundo` / `OliusCardBiodieselFundo` | tons pastel | fundo dos chips de ícone nos 4 cards de indicador |
| `OliusVerdeSeloFundo` / `OliusVerdeSeloTexto` | verde claro/escuro | selo "Pronto para coleta" |
| `OliusSheetFundo` / `OliusSheetTextoSecundario` | `#2B2B2B` / cinza claro | fundo escuro dos bottom sheets |

`Registrar óleo` continua usando o `OliusAmarelo` (`#FFC800`) já existente no app — a amostra da
referência deu `#FCDB03`, bem próximo, então mantive a cor de marca já usada no resto do app em
vez de criar uma segunda variante de amarelo.

## Formatação numérica

Os números usam **ponto** como separador decimal (`625.9K`, `50.3`) igual às referências, mesmo
com o resto do app em pt-BR — foi uma escolha deliberada pra bater visualmente com o design; se
preferir vírgula (padrão pt-BR), é só trocar `Locale.US` por `Locale.forLanguageTag("pt-BR")` nas
funções `formatNumber`/`formatCompact`/`formatThousands` no fim de `HomeScreen.kt`.

## Correção não relacionada (necessária pra compilar)

`app/build.gradle.kts` tinha `implementation("com.facebook.android:facebook-android-sdk:8.x")` —
`8.x` não é uma versão válida (não resolve no Maven, quebra **qualquer** build do projeto). Isso
já estava no arquivo antes desta tarefa (não fui eu quem adicionou), então só comentei a linha
com um `TODO` explicando o problema, em vez de apagar — like isso, o projeto volta a compilar.
Troque por uma versão real do SDK do Facebook quando for habilitar login por lá.

## Testado no emulador

Rodei o app completo num emulador Pixel 6, entrei até a Home (fluxo de login não valida nada de
verdade ainda, então basta preencher qualquer perfil) e verifiquei visualmente: os 2 popups, os 2
bottom sheets, a troca de abas do menu inferior e o preenchimento do tanque/barra de progresso.
Encontrei e corrigi, durante esse teste, um bug de texto sobreposto no popup "Registrar Óleo"
(colunas sem peso igual faziam "Litros de água preservada" e "Kg de CO² evitados" colarem uma na
outra).

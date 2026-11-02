# Olius Mobile (Android + Kotlin + Jetpack Compose)

App Android do projeto interdisciplinar **Olius**: registra o descarte de óleo de
cozinha usado, liga estabelecimentos (B2B) e cidadãos (B2C) aos pontos de coleta
(PEV) e mostra o impacto ambiental, o ranking e o nível de sustentabilidade de cada conta.

O app não guarda regra de negócio nem chave de API: fala com duas APIs do projeto.

```
                        ┌──> Core API (Spring + PostgreSQL, :8080, /api/v1)
App Android ──HTTPS/JWT─┤      login, cadastro, sessão, perfil, PEVs, coletas, chat (repassa para a IA)
 (Compose + MVVM)       └──> API Redis (Spring + Redis, :8081)
                               ranking anual por pontos (valida o Bearer na Core)
             ──HTTPS──> ViaCEP (público)       preenche o endereço pelo CEP
             ──HTTPS──> OpenStreetMap (tiles)  mapa, sem chave
```

## Como rodar

**Pré-requisitos:** Android Studio recente, JDK 21 (o Gradle baixa o toolchain sozinho)
e um emulador com imagem **Google Play** (o login com Google precisa dela).

1. Peça ao time o `app/google-services.json` e coloque em `app/` (não é versionado).
   O plugin do Google Services só gera o `default_web_client_id` do login com Google;
   o app não usa nenhuma biblioteca do Firebase.
2. (Opcional) Aponte para outro servidor no `local.properties` da raiz (não versionado):

   ```properties
   OLIUS_CORE_BASE_URL=http://10.0.2.2:8080/
   OLIUS_RANKING_BASE_URL=http://10.0.2.2:8081/
   ```

   Sem as chaves, o app usa esses valores: `10.0.2.2` é o `localhost` do PC visto
   pelo emulador. Trocou a URL? Faça rebuild. HTTP sem TLS só é liberado para
   `10.0.2.2` e `localhost` (`res/xml/network_security_config.xml`).
3. Suba as APIs (cada uma tem o próprio README). Para o ranking com dados de exemplo:

   ```bash
   # no repositório da API Redis
   OLIUS_SEED=true OLIUS_API_DOCS_ENABLED=true docker compose up --build
   # http://localhost:8081/health
   ```

4. Rode a configuração `app` no Android Studio, ou:

   ```bash
   ./gradlew installDebug          # Windows: gradlew.bat installDebug
   ```

Testes: `./gradlew testDebugUnitTest` (também rodam no CI, com Sonar).

## Configuração (`data/remote/ApiConfig.kt`)

`ApiConfig.kt` é o **único lugar com URLs, rotas e flags**. Cada rota tem um
comentário dizendo o contrato e se ela já existe na Core.

| Flag | Valor | Efeito |
|---|---|---|
| `USE_MOCK_MAIN_BACKEND` | `true` | Dados das telas (Home, Histórico, Mapa, Coletor, Dicas) vêm de mocks. Passa para `false` quando a Core tiver todas as rotas |
| `USE_MOCK_RANKING` | `false` | O ranking usa a API Redis de verdade |
| `USE_MOCK_OLIVIA` | `false` | O chat chama `POST /api/v1/chat` na Core |
| `SOCIAL_LOGIN_ENABLED` | `false` | Esconde os botões Google/Facebook/Microsoft. O fluxo do Google está pronto, esperando `auth/mobile/google` na Core |
| `DEBUG_MOCK_COLLECTOR` | `false` | `true` faz o perfil mock nascer PEV, para testar as telas do coletor |

Login, cadastro, sessão, `/users/me`, ranking e chat **não dependem** de
`USE_MOCK_MAIN_BACKEND`: vão sempre para os servidores.

## Telas

| Tela | Pacote | Status |
|---|---|---|
| Splash | `presentation/splash` | Pula o login se já houver sessão |
| Login, cadastro (cidadão e estabelecimento) | `screen/PerfilType` | Core. Telefone com DDI por país, CPF/CNPJ validados, CEP pelo ViaCEP |
| Esqueci a senha (código de 6 dígitos) | `screen/PerfilType` | App pronto; a Core ainda não tem `auth/password-resets` |
| Home (B2B, B2C e coletor) | `screen/home` | Mock, exceto "Solicitar coleta" (`collection-requests`) |
| Histórico | `screen/history` | B2B: `collections` + `collection-requests` + `score`. B2C: `users/me/points` |
| Mapa e busca de PEVs | `screen/map` | osmdroid (OpenStreetMap) + GPS. Pontos de `GET /pevs` |
| Vire Coletor | `screen/becomecollector` | `POST /pevs` + ViaCEP |
| Ranking e Conquistas | `screen/ranking` | API Redis. Conquistas B2B: `certificate-progress` na Core |
| Chat com a OlivIA | `screen/chat` | `POST /api/v1/chat` na Core (avisa se a rota não existir) |
| Perfil, Editar Perfil, Política de Privacidade | `screen/profile` | `/users/me` + `/pevs/me`. Tema claro/escuro. Editar espera `PATCH /users/me` |
| QR Code de identificação | `screen/qrcode` | Gerado no aparelho (ZXing) a partir do `qrToken` do perfil |
| Sem conexão | `screen/error` | "Voltar ao início": Home se houver sessão, senão o login |

O app roda só em retrato.

### Tipos de usuário

O tipo vem do perfil da Core (`userType` em `GET /api/v1/users/me`), não de uma escolha no app.

| Tipo | Como é definido |
|---|---|
| B2B | `userType = ESTABLISHMENT` |
| B2C | `userType = CITIZENS` |
| Coletor | Dono de um PEV **aprovado** (`GET /api/v1/pevs/me` → `APPROVED`). Pode ser B2B ou B2C; não é um terceiro tipo de conta |

As regras ficam em `HomeViewModel`, `HistoryViewModel`, `MapViewModel` e `AppContainer.currentProfileType()`.

## Sessão e segurança

- Login em `POST /api/v1/auth/mobile/login` → `accessToken` + `refreshToken` (JWT da Core).
- O **access token fica só na memória**. O **refresh token** fica cifrado com uma
  chave do Android Keystore (AES/GCM) e fora do backup (`backup_rules.xml` e
  `data_extraction_rules.xml`).
- Toda chamada autenticada leva `Authorization: Bearer`. Um 401 faz **um** refresh
  (um por vez, mesmo com várias chamadas) e repete a chamada; se o refresh for
  recusado, o app volta para o login.
- O perfil (`/users/me`) fica em cache no Room: abrir o app não precisa chamar a
  API para saber se a conta é estabelecimento ou cidadão.
- Tokens e `qrToken` não vão para logs (os objetos de token escondem o valor no `toString`).

## Organização (MVVM + Clean Architecture)

Base: `app/src/main/java/com/olius/app/`.

| Pacote | Responsabilidade |
|---|---|
| `presentation/screen/*` | Uma pasta por tela: `Screen` (Compose), `ViewModel` (`StateFlow` de `UiState`) |
| `presentation/navigation` | `Routes` e `NavGraph`; `screen/main` tem o scaffold com o menu inferior |
| `presentation/components`, `presentation/theme` | Componentes compartilhados, cores e tema claro/escuro |
| `presentation/auth` | `GoogleIdentityClient` (Credential Manager) |
| `domain/entity` | Modelos e regras puras (documentos, senha, telefone, erros) |
| `domain/repository` | Contratos dos repositórios |
| `domain/usecases` | Casos de uso (`LoginUseCase`, `RegisterUseCase`, `SyncUserProfileUseCase`, `BuildQrCodeContentUseCase`...) |
| `data/remote` | `ApiConfig` (URLs, rotas e flags), `ApiClient` (HTTP próprio com `HttpURLConnection` + `org.json`), `CoreAuthApi`, `AppContainer` (escolhe mock ou HTTP) |
| `data/repository` | `Http*Repository` e `Mock*Repository` de cada tela, `CoreSessionRepository` (tokens e refresh) |
| `data/local` | `SessionStore` (Room), `RefreshTokenStore` (Keystore), preferências do aparelho |

Os ViewModels têm construtor padrão que pega os repositórios do `AppContainer`;
os testes passam fakes no construtor.

## Bibliotecas

| Uso | Biblioteca |
|---|---|
| UI | Jetpack Compose (BOM), Material 3, Navigation Compose, Material Icons Extended |
| Login com Google | Credential Manager + `googleid` |
| Cache do perfil | Room (com KSP) |
| Mapa | osmdroid (OpenStreetMap, sem chave nem cartão) |
| Telefone | `libphonenumber-android` (DDI, máscara e validação offline) |
| QR Code | ZXing `core` (só o gerador) |
| Testes | JUnit 4, MockK, Turbine, `kotlinx-coroutines-test`, `org.json` |

Versões em `gradle/libs.versions.toml`. Dependência nova precisa entrar também no
`gradle/verification-metadata.xml` (o build verifica os checksums).

| Item | Versão |
|---|---|
| `minSdk` / `targetSdk` | 29 / 37 |
| Kotlin / AGP / Gradle | 2.2.10 / 9.2.1 / 9.4.1 |
| JDK (toolchain) | 21 |

## Testes e qualidade

| Comando | O que faz |
|---|---|
| `./gradlew testDebugUnitTest` | Testes unitários (ViewModels, use cases, repositórios HTTP com servidor fake, regras de domínio) |
| `./gradlew createDebugUnitTestCoverageReport` | Relatório JaCoCo usado pelo Sonar |
| `python -m unittest discover tests` | Valida o `.coderabbit.yaml` |

- **CI** (`.github/workflows/ci.yml`): em PR e push para a `main`, roda `build`,
  os testes, a cobertura e o **SonarCloud** (`Olius-Inter_Olius_Mobile`). O
  `google-services.json` é recriado a partir do secret `GOOGLE_SERVICES_JSON`.
- **CodeRabbit** (`.coderabbit.yaml`): revisão automática dos PRs em pt-BR.
- Arquivos `*Screen.kt` (só Compose) ficam fora da cobertura do Sonar.

## Fluxo de trabalho

- Uma branch por card do ClickUp, criada a partir da `main`:
  `feat/<id-do-card>-descricao`, `fix/...`, `chore/...`.
- Commits no padrão Conventional Commits, em português:
  `feat(ranking): Integra a tela de ranking com a API Redis`.
- PR para a `main` com o CI verde.
- Nunca versionar `google-services.json`, `local.properties`, keystores nem `.env`
  (já estão no `.gitignore`).

## Licença

[MIT](LICENSE)

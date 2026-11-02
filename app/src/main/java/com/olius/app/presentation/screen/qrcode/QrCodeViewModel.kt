package com.olius.app.presentation.screen.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.repository.UserProfileRepository
import com.olius.app.domain.usecases.BuildQrCodeContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QrCodeUiState(
    val isLoading: Boolean = true,
    /** Texto codificado no QR (o qrToken exato). null = token indisponível. */
    val content: String? = null
)

/**
 * Tela "Identifique-se". O token já vem salvo no perfil (GET /users/me →
 * qrToken); só chama a API se ele ainda não estiver no aparelho.
 */
class QrCodeViewModel(
    private val profiles: UserProfileRepository = AppContainer.userProfileRepository,
    private val buildContent: BuildQrCodeContentUseCase = BuildQrCodeContentUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeUiState())
    val uiState: StateFlow<QrCodeUiState> = _uiState.asStateFlow()

    init {
        val cached = profiles.cachedProfile()?.qrToken
        if (cached != null) {
            _uiState.value = QrCodeUiState(isLoading = false, content = buildContent(cached))
        } else {
            viewModelScope.launch {
                val token = profiles.fetchMyProfile().getOrNull()?.qrToken
                _uiState.update { QrCodeUiState(isLoading = false, content = buildContent(token)) }
            }
        }
    }
}

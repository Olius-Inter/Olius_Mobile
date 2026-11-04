package com.olius.app.presentation.screen.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.ApiConfig
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
    /** Texto codificado no QR (link do Forms com o qrToken). */
    val content: String? = null
)

/** Tela "Identifique-se". */
class QrCodeViewModel(
    private val profiles: UserProfileRepository = AppContainer.userProfileRepository,
    private val buildContent: BuildQrCodeContentUseCase = BuildQrCodeContentUseCase(ApiConfig.FORMS_BASE_URL)
) : ViewModel() {
    private val _uiState = MutableStateFlow(QrCodeUiState())
    val uiState: StateFlow<QrCodeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Mostra o QR do perfil salvo e confere com a Core; sem perfil salvo, espera a Core. */
    fun load() {
        val cached = buildContent(profiles.cachedProfile()?.qrToken)
        _uiState.value = QrCodeUiState(isLoading = cached == null, content = cached)
        viewModelScope.launch {
            val fresh = profiles.fetchMyProfile().getOrNull()
            _uiState.update {
                QrCodeUiState(isLoading = false, content = if (fresh != null) buildContent(fresh.qrToken) else cached)
            }
        }
    }
}

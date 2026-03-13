package con.open.tvos.ui.screens.detail

import androidx.lifecycle.viewModelScope
import con.open.tvos.bean.VodInfo
import con.open.tvos.ui.base.BaseViewModel
import con.open.tvos.ui.base.UiEffect
import con.open.tvos.ui.base.UiEvent
import con.open.tvos.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailState(
    val vodInfo: VodInfo? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) : UiState

sealed class DetailEvent : UiEvent {
    data class LoadDetail(val vodId: String) : DetailEvent()
    data class SelectSeriesFlag(val flagIndex: Int) : DetailEvent()
    data class SelectEpisode(val episodeIndex: Int) : DetailEvent()
}

sealed class DetailEffect : UiEffect {
    data class PlayEpisode(val vodId: String, val episodeIndex: Int) : DetailEffect()
    object NavigateBack : DetailEffect()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    // TODO: Inject repository
) : BaseViewModel<DetailState, DetailEvent, DetailEffect>(
    initialState = DetailState()
) {

    fun loadDetail(vodId: String) {
        viewModelScope.launch {
            setLoading(true)
            try {
                // TODO: Load from repository
                // Dummy data for now
                val vodInfo = createDummyVodInfo(vodId)
                updateState {
                    copy(
                        vodInfo = vodInfo,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                setError(e.message)
            }
        }
    }

    private fun createDummyVodInfo(vodId: String): VodInfo {
        return VodInfo().apply {
            id = vodId
            name = "示例影片 $vodId"
            pic = null
            des = "这是一部精彩的影片，讲述了..."
            actor = "演员A, 演员B, 演员C"
            director = "导演X"
            area = "中国"
            year = 2024
            note = "全24集"
        }
    }
}

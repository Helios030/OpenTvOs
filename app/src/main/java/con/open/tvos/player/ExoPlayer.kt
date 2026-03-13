package con.open.tvos.player

import android.content.Context
import android.util.Pair
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import con.open.tvos.util.AudioTrackMemory
import con.open.tvos.util.LOG
import xyz.doikki.videoplayer.exo.ExoMediaPlayer

class ExoPlayer(context: Context) : ExoMediaPlayer(context) {

    companion object {
        private val LANG_MAP = mapOf(
            "zh" to "中文",
            "zh-cn" to "中文",
            "en" to "英语",
            "en-us" to "英语"
        )
        
        private var memory: AudioTrackMemory? = null
    }

    init {
        memory = AudioTrackMemory.getInstance(context)
    }

    fun getTrackInfo(): TrackInfo {
        val data = TrackInfo()
        val mappedInfo = trackSelector.currentMappedTrackInfo ?: return data
        val params = trackSelector.parameters
        
        for (rendererIndex in 0 until mappedInfo.rendererCount) {
            val type = mappedInfo.getRendererType(rendererIndex)
            if (type != C.TRACK_TYPE_AUDIO && type != C.TRACK_TYPE_TEXT) continue
            
            val groups = mappedInfo.getTrackGroups(rendererIndex)
            val override = params.getSelectionOverride(rendererIndex, groups)
            var hasSelected = false
            
            for (groupIndex in 0 until groups.length) {
                val group = groups[groupIndex]
                for (trackIndex in 0 until group.length) {
                    val fmt = group.getFormat(trackIndex)
                    val bean = TrackInfoBean().apply {
                        language = getLanguage(fmt)
                        name = getName(fmt)
                        this.groupIndex = groupIndex
                        index = trackIndex
                        selected = if (override != null) {
                            if (override.groupIndex == groupIndex) {
                                override.tracks.contains(trackIndex).also { 
                                    if (it) hasSelected = true 
                                }
                            } else false
                        } else if (type == C.TRACK_TYPE_AUDIO && !hasSelected) {
                            hasSelected = true
                            true
                        } else false
                    }
                    
                    if (type == C.TRACK_TYPE_AUDIO) {
                        data.addAudio(bean)
                    } else {
                        data.addSubtitle(bean)
                    }
                }
            }
        }
        return data
    }

    fun setTrack(groupIndex: Int, trackIndex: Int, playKey: String) {
        try {
            val mappedInfo = trackSelector.currentMappedTrackInfo ?: run {
                LOG.i("echo-setTrack: MappedTrackInfo is null")
                return
            }
            
            val audioRendererIndex = findAudioRendererIndex(mappedInfo)
            if (audioRendererIndex == C.INDEX_UNSET) {
                LOG.i("echo-setTrack: No audio renderer found")
                return
            }
            
            val audioGroups = mappedInfo.getTrackGroups(audioRendererIndex)
            if (!isTrackIndexValid(audioGroups, groupIndex, trackIndex)) {
                LOG.i("echo-setTrack: Invalid track index - group:$groupIndex, track:$trackIndex")
                return
            }
            
            val newOverride = DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
            val builder = trackSelector.buildUponParameters()
            builder.clearSelectionOverrides(audioRendererIndex)
            builder.setSelectionOverride(audioRendererIndex, audioGroups, newOverride)
            trackSelector.setParameters(builder.build())
            
            if (playKey.isNotEmpty()) {
                memory?.save(playKey, groupIndex, trackIndex)
            }
        } catch (e: Exception) {
            LOG.i("echo-setTrack error: ${e.message}")
        }
    }

    fun loadDefaultTrack(playKey: String) {
        val pair = memory?.exoLoad(playKey) ?: return
        val mappedInfo = trackSelector.currentMappedTrackInfo ?: return
        
        val audioRendererIndex = findAudioRendererIndex(mappedInfo)
        if (audioRendererIndex == C.INDEX_UNSET) return
        
        val audioGroups = mappedInfo.getTrackGroups(audioRendererIndex)
        val groupIndex = pair.first
        val trackIndex = pair.second
        
        if (!isTrackIndexValid(audioGroups, groupIndex, trackIndex)) return
        
        val override = DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
        val builder = trackSelector.buildUponParameters()
        builder.clearSelectionOverrides(audioRendererIndex)
        builder.setSelectionOverride(audioRendererIndex, audioGroups, override)
        trackSelector.setParameters(builder.build())
    }

    private fun findAudioRendererIndex(mappedInfo: MappingTrackSelector.MappedTrackInfo): Int {
        for (i in 0 until mappedInfo.rendererCount) {
            if (mappedInfo.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                return i
            }
        }
        return C.INDEX_UNSET
    }

    private fun isTrackIndexValid(groups: TrackGroupArray, groupIndex: Int, trackIndex: Int): Boolean {
        if (groupIndex < 0 || groupIndex >= groups.length) return false
        val group = groups[groupIndex]
        return trackIndex >= 0 && trackIndex < group.length
    }

    private fun getLanguage(fmt: Format): String {
        val lang = fmt.language ?: return "未知"
        if (lang.isEmpty() || "und".equals(lang, ignoreCase = true)) {
            return "未知"
        }
        return LANG_MAP[lang.lowercase()] ?: lang
    }

    private fun getName(fmt: Format): String {
        val channelLabel = when {
            fmt.channelCount <= 0 -> ""
            fmt.channelCount == 1 -> "单声道"
            fmt.channelCount == 2 -> "立体声"
            else -> "${fmt.channelCount} 声道"
        }
        
        val codec = fmt.sampleMimeType?.let { mime ->
            mime.substring(mime.indexOf('/') + 1).uppercase()
        } ?: ""
        
        return listOf(channelLabel, codec).filter { it.isNotEmpty() }.joinToString(", ")
    }
}

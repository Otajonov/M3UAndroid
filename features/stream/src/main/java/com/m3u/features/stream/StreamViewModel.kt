package com.m3u.features.stream

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Tracks
import com.m3u.core.architecture.logger.Logger
import com.m3u.core.architecture.viewmodel.BaseViewModel
import com.m3u.data.manager.PlayerManager
import com.m3u.data.repository.PlaylistRepository
import com.m3u.data.repository.StreamRepository
import com.m3u.dlna.DLNACastManager
import com.m3u.dlna.OnDeviceRegistryListener
import com.m3u.dlna.control.DeviceControl
import com.m3u.dlna.control.OnDeviceControlListener
import com.m3u.dlna.control.ServiceActionCallback
import com.m3u.features.stream.model.Format
import com.m3u.features.stream.model.asFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jupnp.model.meta.Device
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val streamRepository: StreamRepository,
    playlistRepository: PlaylistRepository,
    private val playerManager: PlayerManager,
    private val logger: Logger
) : BaseViewModel<StreamState, StreamEvent>(
    emptyState = StreamState()
), OnDeviceRegistryListener, OnDeviceControlListener {
    private val _devices = MutableStateFlow<ImmutableList<Device<*, *, *>>>(persistentListOf())

    // searched screencast devices
    internal val devices = _devices.asStateFlow()

    private val _volume: MutableStateFlow<Float> = MutableStateFlow(1f)
    internal val volume = _volume.asStateFlow()

    // playlist and stream info
    internal val metadata: StateFlow<StreamState.Metadata> = combine(
        playerManager.url,
        streamRepository.observeAll(),
        playlistRepository.observeAll()
    ) { url, streams, playlists ->
        val stream = streams.find { it.url == url }
        val playlist = playlists.find { it.url == stream?.playlistUrl }
        StreamState.Metadata(playlist, stream)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = StreamState.Metadata(),
            started = SharingStarted.WhileSubscribed(5_000)
        )

    private val groups: StateFlow<List<Tracks.Group>> = playerManager
        .groups
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(5_000)
        )

    internal val formats: StateFlow<ImmutableList<Format>> = groups
        .mapNotNull { all -> all.find { it.type == C.TRACK_TYPE_VIDEO } }
        .map { group ->
            List(group.length) {
                group.getTrackFormat(it)
            }
        }
        .map { all ->
            all.mapNotNull { f ->
                f.takeIf { it.width > 0 && it.height > 0 }?.asFormat()
            }.toPersistentList()
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = persistentListOf(),
            started = SharingStarted.WhileSubscribed(5_000)
        )

    internal val format: StateFlow<Format?> = playerManager
        .selected
        .map { it[C.TRACK_TYPE_VIDEO] }
        .map { it?.asFormat() }
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(5_000)
        )

    internal fun chooseFormat(format: Format) {
        val currentGroups = groups.value
        val currentGroup = currentGroups.find { it.type == C.TRACK_TYPE_VIDEO } ?: return
        for (index in 0 until currentGroup.length) {
            if (currentGroup.getTrackFormat(index).id == format.id) {
                playerManager.chooseTrack(
                    group = currentGroup.mediaTrackGroup,
                    trackIndex = index
                )
                break
            }
        }
    }

    // stream playing state
    internal val playerState: StateFlow<StreamState.PlayerState> = combine(
        playerManager.player,
        playerManager.playbackState,
        playerManager.videoSize,
        playerManager.playerError
    ) { player, playState, videoSize, playerError ->
        StreamState.PlayerState(
            playState = playState,
            videoSize = videoSize,
            playerError = playerError,
            player = player
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StreamState.PlayerState()
        )

    init {
        playerManager
            .url
            .onEach { url ->
                url ?: return@onEach
                val stream = streamRepository.getByUrl(url) ?: return@onEach
                streamRepository.reportPlayed(stream.id)
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: StreamEvent) {
        when (event) {
            is StreamEvent.OpenDlnaDevices -> openDlnaDevices(event.activityContext)
            is StreamEvent.CloseDlnaDevices -> closeDlnaDevices(event.activityContext)
            is StreamEvent.ConnectDlnaDevice -> connectDlnaDevice(event.device)
            is StreamEvent.DisconnectDlnaDevice -> disconnectDlnaDevice(event.device)
            is StreamEvent.OnFavourite -> onFavourite(event.url)
            StreamEvent.Release -> release()
            is StreamEvent.OnVolume -> onVolume(event.volume)
        }
    }

    private val _isDevicesVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // show searching devices dialog or not
    internal val isDevicesVisible = _isDevicesVisible.asStateFlow()

    private val _searching: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // searching or not
    internal val searching = _searching.asStateFlow()

    private fun openDlnaDevices(activityContext: Context) {
        try {
            DLNACastManager.bindCastService(activityContext)
            DLNACastManager.registerDeviceListener(this)
        } catch (ignore: Exception) {

        }
        viewModelScope.launch {
            delay(800.milliseconds)
            _searching.value = true
        }
        _isDevicesVisible.value = true
    }

    private fun closeDlnaDevices(activityContext: Context) {
        try {
            _searching.value = false
            _isDevicesVisible.value = false
            _devices.value = persistentListOf()
            DLNACastManager.unbindCastService(activityContext)
            DLNACastManager.unregisterListener(this)
        } catch (ignore: Exception) {

        }
    }

    private var controlPoint: DeviceControl? = null

    private fun connectDlnaDevice(device: Device<*, *, *>) {
        controlPoint = DLNACastManager.connectDevice(device, this)
    }

    private fun disconnectDlnaDevice(device: Device<*, *, *>) {
        controlPoint?.stop()
        controlPoint = null
        DLNACastManager.disconnectDevice(device)
    }

    private val _recording = MutableStateFlow(false)
    internal val recording = _recording.asStateFlow()

    internal fun record() {
        _recording.update { !it }
    }

    private fun onFavourite(url: String) {
        viewModelScope.launch {
            val stream = streamRepository.getByUrl(url) ?: return@launch
            val id = stream.id
            val target = !stream.favourite
            streamRepository.setFavourite(id, target)
        }
    }

    private fun onVolume(target: Float) {
        _volume.update { target }

        playerState.value.player?.volume = target
        controlPoint?.setVolume((target * 100).roundToInt(), null)
    }

    override fun onDeviceAdded(device: Device<*, *, *>) {
        _devices.update { (it + device).toPersistentList() }
    }

    override fun onDeviceRemoved(device: Device<*, *, *>) {
        _devices.update { (it - device).toPersistentList() }
    }

    override fun onConnected(device: Device<*, *, *>) {
        writable.update { it.copy(connected = device) }
        val url = metadata.value.stream?.url ?: return
        val title = metadata.value.stream?.title.orEmpty()

        controlPoint?.setAVTransportURI(
            uri = url,
            title = title,
            callback = object : ServiceActionCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    controlPoint?.play()
                }

                override fun onFailure(msg: String) {
                    logger.log(msg)
                }
            }
        )
    }

    override fun onDisconnected(device: Device<*, *, *>) {
        writable.update { it.copy(connected = null) }
        controlPoint?.stop()
        controlPoint = null
    }

    private fun release() {
        controlPoint?.stop()
        controlPoint = null
        playerManager.stop()
    }
}
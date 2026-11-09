package com.music.bitchord.ui

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer {
    private var player: AVPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    init {
        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryPlayback, null)
            audioSession.setActive(true, null)
        } catch (_: Throwable) {
        }
    }

    fun playUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        player?.pause()
        player = AVPlayer.playerWithURL(nsUrl)
        player?.play()
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (_isPlaying.value) {
            p.pause()
            _isPlaying.value = false
        } else {
            p.play()
            _isPlaying.value = true
        }
    }

    fun seekTo(seconds: Float) {
        val p = player ?: return
        val time = CMTimeMakeWithSeconds(seconds.toDouble(), 1000)
        p.seekToTime(time)
    }

    fun updateProgress() {
        val p = player ?: return
        val current = CMTimeGetSeconds(p.currentTime()).toFloat()
        if (!current.isNaN() && current >= 0f) {
            _currentPosition.value = current
        }
        val item = p.currentItem
        if (item != null) {
            val d: CValue<CMTime> = item.duration()
            val total = CMTimeGetSeconds(d).toFloat()
            if (!total.isNaN() && total > 0f) {
                _duration.value = total
            }
        }
    }
}

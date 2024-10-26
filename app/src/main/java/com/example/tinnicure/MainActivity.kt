package com.example.tinnicure

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var playPauseButton: Button
    private lateinit var frequencyLabel: TextView
    private lateinit var frequencySlider: SeekBar
    private lateinit var bothEars: RadioButton
    private lateinit var leftEar: RadioButton
    private lateinit var rightEar: RadioButton

    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var currentFrequency = 200
    private var playJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playPauseButton = findViewById(R.id.playPauseButton)
        frequencyLabel = findViewById(R.id.frequencyLabel)
        frequencySlider = findViewById(R.id.frequencySlider)
        bothEars = findViewById(R.id.bothEars)
        leftEar = findViewById(R.id.leftEar)
        rightEar = findViewById(R.id.rightEar)

        // Slider Listener zur Frequenzänderung
        frequencySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentFrequency = progress
                frequencyLabel.text = "Frequency: $progress Hz"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Play/Pause Button
        playPauseButton.setOnClickListener {
            if (isPlaying) {
                isPlaying = !isPlaying
                stopTone()
                playPauseButton.text = "Play"
            } else {
                isPlaying = !isPlaying
                startTone(currentFrequency)
                playPauseButton.text = "Pause"
            }
        }
    }

    private fun startTone(frequency: Int) {
        val sampleRate = 44100
        val numSamples = sampleRate
        val generatedSnd = DoubleArray(numSamples)
        val sound = ByteArray(2 * numSamples)

        // Generate sine wave sound data
        for (i in generatedSnd.indices) {
            generatedSnd[i] = Math.sin(2.0 * Math.PI * i / (sampleRate / frequency))
        }

        var idx = 0
        for (dVal in generatedSnd) {
            val value = (dVal * 32767).toInt()
            sound[idx++] = (value and 0x00ff).toByte()
            sound[idx++] = ((value and 0xff00) shr 8).toByte()
        }

        val channelConfig = when {
            leftEar.isChecked -> AudioFormat.CHANNEL_OUT_MONO
            rightEar.isChecked -> AudioFormat.CHANNEL_OUT_MONO
            else -> AudioFormat.CHANNEL_OUT_STEREO
        }

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC, sampleRate,
            channelConfig, AudioFormat.ENCODING_PCM_16BIT,
            sound.size, AudioTrack.MODE_STREAM
        )

        audioTrack?.play()

        // Start a coroutine to continuously write the sound data to the AudioTrack
        playJob = CoroutineScope(Dispatchers.IO).launch {
            while (isPlaying) {
                audioTrack?.write(sound, 0, sound.size)
            }
        }
    }

    private fun stopTone() {
        playJob?.cancel()
        playJob = null

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}

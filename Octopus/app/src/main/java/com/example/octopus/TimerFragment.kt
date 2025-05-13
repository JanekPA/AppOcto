package com.example.octopus

import android.animation.ValueAnimator
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView

class TimerFragment : Fragment(R.layout.fragment_timer) {

    private lateinit var timerText: TextView
    private lateinit var lottieView: LottieAnimationView
    private lateinit var roundTimeInput: EditText
    private lateinit var breakTimeInput: EditText
    private lateinit var roundsInput: EditText

    private var timer: CountDownTimer? = null
    private var animator: ValueAnimator? = null

    private var isRunning = false
    private var isPaused = false
    private var isBreak = false
    private var currentRound = 1
    private var totalRounds = 1
    private var roundTime = 60_000L
    private var breakTime = 30_000L
    private var timeLeft = 0L
    private var totalTime = 0L
    private var pausedTimeLeft = 0L
    private var pausedAnimationProgress = 0f

    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        timerText = view.findViewById(R.id.text_timer)
        lottieView = view.findViewById(R.id.lottie_timer)
        roundTimeInput = view.findViewById(R.id.input_round_time)
        breakTimeInput = view.findViewById(R.id.input_break_time)
        roundsInput = view.findViewById(R.id.input_rounds_count)

        view.findViewById<Button>(R.id.button_start).setOnClickListener { startFullTimer() }
        view.findViewById<Button>(R.id.button_pause).setOnClickListener { pauseTimer() }
        view.findViewById<Button>(R.id.button_reset).setOnClickListener { resetTimer() }

        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        soundId = soundPool.load(requireContext(), R.raw.beep, 1)
    }

    private fun startFullTimer() {
        if (isRunning) return

        if (isPaused) {
            // Wznawianie od momentu pauzy
            isRunning = true
            isPaused = false
            timeLeft = pausedTimeLeft
            startTimer()
            resumeAnimation()
        } else {
            // Nowy cykl
            roundTime = roundTimeInput.text.toString().toLongOrNull()?.times(1000) ?: 60_000L
            breakTime = breakTimeInput.text.toString().toLongOrNull()?.times(1000) ?: 30_000L
            totalRounds = roundsInput.text.toString().toIntOrNull() ?: 1
            currentRound = 1
            startRound()
        }
    }

    private fun startRound() {
        isRunning = true
        isBreak = false
        isPaused = false
        timeLeft = roundTime
        totalTime = roundTime
        startTimer()
        startAnimation()
    }

    private fun startBreak() {
        isRunning = true
        isBreak = true
        isPaused = false
        timeLeft = breakTime
        totalTime = breakTime
        startTimer()
        startAnimation()
    }

    private fun startTimer() {
        timer?.cancel()

        timer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                updateTimerUI()
            }

            override fun onFinish() {
                playBeep()
                stopAnimation()
                if (isBreak) {
                    currentRound++
                    if (currentRound > totalRounds) {
                        timerText.text = "Koniec!"
                        isRunning = false
                        stopAnimation()
                    } else {
                        startRound()
                    }
                } else {
                    startBreak()
                }
            }
        }.start()
    }

    private fun updateTimerUI() {
        val seconds = (timeLeft / 1000) % 60 + 1
        val minutes = (timeLeft / 1000) / 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun startAnimation() {
        animator?.cancel()
        lottieView.progress = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = totalTime
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                lottieView.progress = progress
            }
            start()
        }
    }

    private fun resumeAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(pausedAnimationProgress, 1f).apply {
            duration = pausedTimeLeft
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                lottieView.progress = progress
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        lottieView.progress = 0f
    }

    private fun pauseTimer() {
        if (!isRunning) return

        isRunning = false
        isPaused = true

        timer?.cancel()
        animator?.cancel()

        pausedTimeLeft = timeLeft
        pausedAnimationProgress = lottieView.progress
    }

    private fun resetTimer() {
        timer?.cancel()
        animator?.cancel()

        isRunning = false
        isPaused = false
        currentRound = 1

        timerText.text = "00:00"
        lottieView.progress = 0f
    }

    private fun playBeep() {
        soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        animator?.cancel()
        soundPool.release()
    }
}

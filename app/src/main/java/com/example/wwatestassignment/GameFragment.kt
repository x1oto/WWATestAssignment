package com.example.wwatestassignment

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.wwatestassignment.databinding.FragmentGameBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.wwatestassignment.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by viewModels()
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private var submarineMoveJob: Job? = null
    private var obstaclesSpawnJob: Job? = null
    private var rewardSpawnJob: Job? = null
    private var checkCollisionJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        startGame()
    }

    override fun onPause() {
        super.onPause()
        stopGame()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        setupScoreCounter()
        setupLivesCounter()
        setupMovementControls()
    }

    private fun startGame() {
        viewModel.startScoreCounter()
        spawnObstacles()
        spawnRewards()
        startCollisionDetection()
    }

    private fun stopGame() {
        cancelAllJobs()
        viewModel.destroyScoreCounter()
    }

    private fun cancelAllJobs() {
        submarineMoveJob?.cancel()
        obstaclesSpawnJob?.cancel()
        rewardSpawnJob?.cancel()
        checkCollisionJob?.cancel()
    }

    private fun setupScoreCounter() {
        lifecycleScope.launch {
            viewModel.score.collectLatest { score ->
                binding.tvCount.text = score.toString()
                if (score >= 100) endGame(Result.WIN)
            }
        }
    }

    private fun setupLivesCounter() {
        lifecycleScope.launch {
            viewModel.lives.collectLatest { lives ->
                binding.tvLives.text = lives.toString()
                if (lives == 0) endGame(Result.LOSE)
            }
        }
    }

    private fun endGame(result: Result) {
        viewModel.setDefaultValues()
        findNavController().navigate(GameFragmentDirections.actionGameFragmentToResultFragment(result))
    }

    private fun startCollisionDetection() {
        checkCollisionJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(150)
                val submarineRect = Rect()
                binding.imageView.getHitRect(submarineRect)

                binding.root.children.forEach { child ->
                    if (child !is ImageView || child == binding.imageView || child == binding.clBottom) return@forEach

                    val childRect = Rect().apply { child.getHitRect(this) }
                    if (Rect.intersects(submarineRect, childRect)) {
                        handleCollision(child)
                    }
                }
            }
        }
    }

    private suspend fun handleCollision(child: View) {
        if (child.tag == 0) {
            viewModel.decrementLife()
        } else {
            viewModel.incrementLife()
        }
        delay(500)
    }

    private fun spawnObstacles() {
        obstaclesSpawnJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                spawnObject(R.drawable.bomb_24px, 0)
            }
        }
    }

    private fun spawnRewards() {
        rewardSpawnJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(20000)
                spawnObject(R.drawable.heart_24px, 1)
            }
        }
    }

    private suspend fun spawnObject(drawableId: Int, tag: Int) {
        val dynamicImageView = ImageView(requireContext()).apply {
            this.tag = tag
            setImageResource(drawableId)
            layoutParams = FrameLayout.LayoutParams(100, 100)
            x = binding.root.width.toFloat()
            y = (Math.random() * binding.root.height).toFloat() - (getBottomBarHeight() * 2)
        }

        withContext(Dispatchers.Main) {
            _binding?.root?.addView(dynamicImageView)
            dynamicImageView.animate()
                .translationX(-3000f)
                .setDuration(10000)
                .withEndAction { _binding?.root?.removeView(dynamicImageView) }
                .start()
        }
    }

    private fun setupMovementControls() {
        setupSubmarineMovement(binding.btMoveToTop) { viewModel.surfaceSubmarine(binding.imageView.y.toInt()) }
        setupSubmarineMovement(binding.btMoveToEnd) {
            viewModel.sailSubmarine(
                binding.imageView.y.toInt(),
                getDisplayHeight() - getSubmarineHeight() - getBottomBarHeight()
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSubmarineMovement(button: View, movementFlow: () -> Flow<Float>) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startSubmarineMovement(movementFlow)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopSubmarineMovement()
            }
            false
        }
    }

    private fun startSubmarineMovement(movementFlow: () -> Flow<Float>) {
        submarineMoveJob = lifecycleScope.launch {
            movementFlow().collectLatest { binding.imageView.y = it }
        }
    }

    private fun stopSubmarineMovement() {
        submarineMoveJob?.cancel()
    }

    private fun getDisplayHeight(): Int {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun getBottomBarHeight() = binding.clBottom.height

    private fun getSubmarineHeight() = binding.imageView.height
}

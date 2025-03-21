package com.example.wwatestassignment

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
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
import kotlinx.coroutines.cancel

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by viewModels()

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val submarineScope = CoroutineScope(Dispatchers.Default)
    private var submarineMoveJob: Job? = null
    private val obstacleScope = CoroutineScope(Dispatchers.Default)
    private var obstaclesSpawnJob: Job? = null
    private var checkCollisionJob: Job? = null

    private var isActive: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moveSubmarineUp()
        moveSubmarineDown()
        setupScoreCounter()
        setupLivesCounter()
    }

    override fun onResume() {
        super.onResume()
        isActive = true
        viewModel.startScoreCounter()
        spawnObstacle()
        checkCollision()
    }

    private fun setupScoreCounter() {
        lifecycleScope.launch {
            viewModel.score.collectLatest {
                binding.tvCount.text = it.toString()

                if(it == 100) {
                    findNavController().navigate(GameFragmentDirections.actionGameFragmentToResultFragment(Result.WIN))
                }
            }
        }
    }

    private fun setupLivesCounter() {
        lifecycleScope.launch {
            viewModel.lives.collectLatest {
                binding.tvLives.text = it.toString()

                if(it == 0) {
                    findNavController().navigate(GameFragmentDirections.actionGameFragmentToResultFragment(Result.LOSE))
                }
            }
        }
    }

    private fun checkCollision() {
        checkCollisionJob = lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                delay(150)
                val submarineRect = Rect()
                binding.imageView.getHitRect(submarineRect)

                for (i in 0 until binding.root.childCount) {
                    val child = binding.root.getChildAt(i)
                    if (child != binding.imageView && child != binding.clBottom) {
                        val childRect = Rect()
                        child.getHitRect(childRect)
                        if (Rect.intersects(submarineRect, childRect)) {
                            viewModel.decrementLife()
                            delay(500)
                        }
                    }
                }
            }
        }
    }


    private fun spawnObstacle() {
        obstaclesSpawnJob = obstacleScope.launch {
            while (true) {
                delay(1000)
                val dynamicImageView = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_launcher_background)
                    layoutParams = FrameLayout.LayoutParams(100, 100)
                    val rootWidth = _binding?.root?.width ?: 0
                    x = rootWidth.toFloat()
                    val rootHeight = _binding?.root?.height ?: 0
                    y = (Math.random() * rootHeight).toFloat() - (getBottomBarHeight() * 2)
                }

                withContext(Dispatchers.Main) {
                    _binding?.let { binding ->
                        binding.root.addView(dynamicImageView)

                        dynamicImageView.animate()
                            .translationX(-3000f)
                            .setDuration(10000)
                            .withEndAction {
                                _binding?.root?.removeView(dynamicImageView)
                            }
                            .start()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        obstaclesSpawnJob?.cancel()
        checkCollisionJob?.cancel()
        submarineMoveJob?.cancel()
        viewModel.destroyScoreCounter()
    }

    override fun onStop() {
        super.onStop()
        obstaclesSpawnJob?.cancel()
        checkCollisionJob?.cancel()
        submarineMoveJob?.cancel()
        viewModel.destroyScoreCounter()
    }

    private fun spawnReward() {

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun moveSubmarineUp() {
        binding.btMoveToTop.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    submarineMoveJob = submarineScope.launch {
                        viewModel.decrement(binding.imageView.y.toInt()).collectLatest {
                            binding.imageView.y = it
                        }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (submarineMoveJob?.isActive == true) {
                        submarineMoveJob?.cancel()
                    }
                }
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun moveSubmarineDown() {
        binding.btMoveToEnd.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    submarineMoveJob = submarineScope.launch {
                        viewModel.increment(
                            binding.imageView.y.toInt(),
                            getDisplayHeight() - getSubmarineHeight() - getBottomBarHeight()
                        )
                            .collectLatest {
                                binding.imageView.y = it
                            }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (submarineMoveJob?.isActive == true) {
                        submarineMoveJob?.cancel()
                    }
                }
            }
            false
        }
    }

    private fun getDisplayHeight(): Int {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun getBottomBarHeight() = binding.clBottom.height

    private fun getSubmarineHeight() = binding.imageView.height

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
        submarineMoveJob?.cancel()
        obstaclesSpawnJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
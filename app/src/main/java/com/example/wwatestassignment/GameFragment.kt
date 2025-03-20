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
import androidx.lifecycle.lifecycleScope
import com.example.wwatestassignment.databinding.FragmentGameBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by viewModels()

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val submarineScope = CoroutineScope(Dispatchers.Default)
    private var submarineMoveJob: Job? = null
    private val obstacleScope = CoroutineScope(Dispatchers.Default)
    private var obstaclesSpawnJob: Job? = null
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
        moveSubmarineUp()
        moveSubmarineDown()
    }

    override fun onResume() {
        super.onResume()
        spawnObstacle()
        checkCollision()
    }

    private fun checkCollision() {
        checkCollisionJob = lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                delay(150)
                val submarineRect = Rect()
                binding.imageView.getHitRect(submarineRect)

                for (i in 0 until binding.root.childCount) {
                    val child = binding.root.getChildAt(i)
                    // Переконуємось, що не перевіряємо саму субмарину
                    if (child != binding.imageView && child != binding.clBottom) {
                        val childRect = Rect()
                        child.getHitRect(childRect)
                        if (Rect.intersects(submarineRect, childRect)) {
                            Log.d("Collision", "Субмарина зіткнулася з dynamicImageView!")
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
                    x = binding.root.width.toFloat()
                    y = (Math.random() * binding.root.height).toFloat()
                }

                withContext(Dispatchers.Main) {
                    binding.root.addView(dynamicImageView)

                    dynamicImageView.animate()
                        .translationX(-3000f)
                        .setDuration(10000)
                        .withEndAction {
                            binding.root.removeView(dynamicImageView)
                        }
                        .start()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        submarineMoveJob?.cancel()
        obstaclesSpawnJob?.cancel()
    }

    override fun onStop() {
        super.onStop()
        submarineMoveJob?.cancel()
        obstaclesSpawnJob?.cancel()
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
                            getDisplayHeight() - getSubmarineHeight()
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

    private fun getDisplayWidth(): Int {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getDisplayHeight(): Int {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun getSubmarineHeight() = binding.imageView.height

    override fun onDestroy() {
        super.onDestroy()
        submarineMoveJob?.cancel()
        obstaclesSpawnJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
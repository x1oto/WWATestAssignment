package com.example.wwatestassignment

import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.wwatestassignment.databinding.FragmentGameBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by viewModels()

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private var submarineMoveJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    //                    ObjectAnimator.ofFloat(binding.imageView2, "translationX", -getDisplayWidth().toFloat()).apply {
//                        duration = 4500
//                        start()
//                    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moveSubmarineUp()
        moveSubmarineDown()

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun moveSubmarineUp() {
        binding.btMoveToTop.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    submarineMoveJob = lifecycleScope.launch {
                        viewModel.decrement(binding.imageView.y.toInt()).collectLatest {
                            binding.imageView.y = it
                        }
                    }

                    val dynamicImageView = ImageView(requireContext()).apply {
                        setImageResource(R.drawable.pic_iceberg)
                        layoutParams = ConstraintLayout.LayoutParams(100, 100)
                        x = binding.root.width.toFloat() - width
                        y = (Math.random() * binding.root.height).toFloat()
                    }

                    binding.root.addView(dynamicImageView)

                    dynamicImageView.animate()
                        .translationX(-3000f)
                        .setDuration(7000)
                        .withEndAction {
                            // Видаляємо ImageView після завершення анімації
                            binding.root.removeView(dynamicImageView)
                        }
                        .start()
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
                    submarineMoveJob = lifecycleScope.launch {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
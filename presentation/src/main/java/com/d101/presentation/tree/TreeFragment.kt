package com.d101.presentation.tree

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.d101.presentation.R
import com.d101.presentation.databinding.DialogTutorialBinding
import com.d101.presentation.databinding.FragmentTreeBinding
import com.d101.presentation.fruit.BeforeFruitCreateBaseFragment
import com.d101.presentation.fruit.EmotionDumpingFragment
import com.d101.presentation.fruit.FruitDetailDialog
import com.d101.presentation.fruit.FruitDialogInterface
import com.d101.presentation.mapper.FruitMapper.toFruitUiModel
import com.d101.presentation.tutorial.TutorialAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import utils.CustomToast
import utils.repeatOnStarted

@AndroidEntryPoint
class TreeFragment : Fragment() {
    private val viewModel: TreeViewModel by viewModels()
    private var _binding: FragmentTreeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dialog: DialogFragment

    private var messageTypingJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tree, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.mainViewModel = viewModel

        binding.guideButtonImageView.setOnClickListener {
            viewModel.onTutorialButtonClicked()
        }

        binding.createFruitButton.setOnClickListener {
            viewModel.onButtonClick()
        }

        binding.mainTreeImagebutton.setOnClickListener {
            viewModel.onGetTreeMessage()
        }

        binding.mainTreeImagebutton.setOnLongClickListener {
            viewModel.onLongClickEmotionTrashMode()
            true
        }

        viewLifecycleOwner.repeatOnStarted {
            viewModel.messageState.collect {
                when (it) {
                    is TreeMessageState.EnableMessage -> {
                        binding.mainTreeImagebutton.isEnabled = true
                    }

                    is TreeMessageState.NoAccessMessage -> {
                        binding.mainTreeImagebutton.isEnabled = false
                    }
                }
            }
        }

        viewLifecycleOwner.repeatOnStarted {
            viewModel.uiState.collect {
                when (it) {
                    is TreeFragmentViewState.EmotionTrashMode -> {
                        LocalBroadcastManager.getInstance(requireContext())
                            .sendBroadcast(Intent("PAUSE"))
                        val fadeIn = ObjectAnimator.ofFloat(
                            binding.nightLottieView,
                            "alpha",
                            0f,
                            1f,
                        )
                        fadeIn.duration = 500
                        fadeIn.start()
                        binding.createFruitButton
                            .setCompoundDrawablesRelativeWithIntrinsicBounds(
                                ContextCompat.getDrawable(requireContext(), R.drawable.btn_boom),
                                null,
                                null,
                                null,
                            )
                        binding.nightLottieView.visibility = View.VISIBLE
                        binding.nightLottieView.playAnimation()
                    }

                    else -> {
                        LocalBroadcastManager.getInstance(requireContext())
                            .sendBroadcast(Intent("PLAY"))
                        val fadeOut = ObjectAnimator.ofFloat(
                            binding.nightLottieView,
                            "alpha",
                            1f,
                            0f,
                        )
                        fadeOut.duration = 500
                        fadeOut.start()
                        binding.createFruitButton
                            .setCompoundDrawablesRelativeWithIntrinsicBounds(
                                ContextCompat.getDrawable(requireContext(), R.drawable.btn_apple),
                                null,
                                null,
                                null,
                            )
                        binding.nightLottieView.pauseAnimation()
                    }
                }
            }
        }

        viewLifecycleOwner.repeatOnStarted {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    is TreeViewEvent.MakeFruitEvent -> {
                        dialog = BeforeFruitCreateBaseFragment()
                        FruitDialogInterface.dialog = dialog
                        dialog.show(childFragmentManager, "")
                    }

                    is TreeViewEvent.CheckTodayFruitEvent -> {
                        dialog = FruitDetailDialog.newInstance(
                            viewModel.todayFruit.toFruitUiModel(),
                        )
                        dialog.show(childFragmentManager, "")
                    }

                    is TreeViewEvent.ShowErrorEvent -> {
                        showToast(event.message)
                    }

                    is TreeViewEvent.ChangeTreeMessage -> {
                        typingAnimation(event.message)
                    }

                    TreeViewEvent.ShowTutorialEvent -> showTutorialDialog()

                    is TreeViewEvent.EmotionTrashEvent -> {
                        dialog = EmotionDumpingFragment()
                        dialog.dialog?.window?.setBackgroundDrawable(
                            ColorDrawable(Color.TRANSPARENT),
                        )
                        dialog.show(childFragmentManager, "")
                    }

                    is TreeViewEvent.OnServerMaintaining -> blockApp(event.message)
                    is TreeViewEvent.CompleteCreationEvent -> {
                        binding.createFruitButton.performClick()
                    }
                }
            }
        }
    }

    private fun blockApp(message: String) {
        showToast(message)
        ActivityCompat.finishAffinity(requireActivity())
    }

    private fun showToast(message: String) {
        CustomToast.createAndShow(requireActivity(), message)
    }

    private fun showTutorialDialog() {
        val dialog = createFullScreenDialog()
        val dialogBinding = DialogTutorialBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.tutorialViewpager.adapter = TutorialAdapter(requireActivity())
        TabLayoutMediator(
            dialogBinding.indicatorTabLayout,
            dialogBinding.tutorialViewpager,
        ) { _, _ -> }.attach()
        dialog.show()
    }

    private fun createFullScreenDialog(): Dialog {
        return Dialog(requireContext(), R.style.Base_FTR_FullScreenDialog).apply {
            window?.setBackgroundDrawableResource(R.drawable.bg_white_radius_30dp)
        }
    }

    private fun typingAnimation(message: String) {
        messageTypingJob?.cancel()
        val sb = StringBuilder()

        messageTypingJob = lifecycleScope.launch(Dispatchers.Default) {
            message.forEach {
                delay(50L)
                sb.append(it)
                withContext(Dispatchers.Main) {
                    binding.treeSpeechTextview.text = sb
                }
            }
            viewModel.enableMessage()
        }
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.uiState.value is TreeFragmentViewState.EmotionTrashMode) {
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(
                Intent("PLAY"),
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageTypingJob?.cancel()
        messageTypingJob = null
        _binding = null
    }
}

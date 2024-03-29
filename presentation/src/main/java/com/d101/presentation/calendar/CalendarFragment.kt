package com.d101.presentation.calendar

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.d101.domain.model.Fruit
import com.d101.domain.model.Juice
import com.d101.domain.utils.FruitEmotion
import com.d101.presentation.R
import com.d101.presentation.calendar.adapter.FruitInCalendarListAdapter
import com.d101.presentation.calendar.adapter.FruitListAdapter
import com.d101.presentation.calendar.adapter.LittleFruitImageUrl
import com.d101.presentation.calendar.adapter.LittleFruitListAdapter
import com.d101.presentation.calendar.event.CalendarViewEvent
import com.d101.presentation.calendar.state.CalendarViewState
import com.d101.presentation.calendar.state.JuiceCreatableStatus
import com.d101.presentation.calendar.state.TodayFruitCreationStatus
import com.d101.presentation.calendar.viewmodel.CalendarViewModel
import com.d101.presentation.collection.CollectionActivity
import com.d101.presentation.databinding.DialogJuiceDetailBinding
import com.d101.presentation.databinding.DialogJuiceShakeBinding
import com.d101.presentation.databinding.FragmentCalendarBinding
import com.d101.presentation.fruit.FruitDetailDialog
import com.d101.presentation.mapper.CalendarMapper.toFruitInCalendar
import com.d101.presentation.mapper.FruitMapper.toFruitUiModel
import dagger.hilt.android.AndroidEntryPoint
import utils.CustomToast
import utils.ShakeEventListener
import utils.ShakeSensorModule
import utils.repeatOnStarted

@AndroidEntryPoint
class CalendarFragment : Fragment() {
    private val viewModel: CalendarViewModel by viewModels()
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var dialog: Dialog

    private lateinit var shakeSensor: ShakeSensorModule
    private lateinit var fruitListAdapter: FruitListAdapter
    private lateinit var littleFruitListAdapter: LittleFruitListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_calendar, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBinding()
        subscribeEvent()
        subscribeViewState()
        fruitListAdapter = FruitListAdapter { fruit -> viewModel.onTapFruitDetailButton(fruit) }
        littleFruitListAdapter = LittleFruitListAdapter()
        binding.frientreeCalendar.setCalendarAdapter(
            FruitInCalendarListAdapter {
                viewModel.onWeekSelected(it)
            },
        )

        binding.frientreeCalendar.setOnMonthClickListener {
            viewModel.onClickNextMonth()
        }

        binding.frientreeCalendar.setOnPrevMonthClickListener {
            viewModel.onClickPreviousMonth()
        }
    }

    private fun setBinding() {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
    }

    private fun subscribeEvent() {
        viewLifecycleOwner.repeatOnStarted {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    is CalendarViewEvent.Init -> {
                        viewModel.onInitOccurred()
                    }

                    is CalendarViewEvent.OnCompleteJuiceShake -> {
                        viewModel.onCompleteJuiceShakeOccurred(event.weekDate)
                    }

                    is CalendarViewEvent.OnTapCollectionButton -> {
                        val intent = Intent(requireActivity(), CollectionActivity::class.java)
                        startActivity(intent)
                    }

                    is CalendarViewEvent.OnTapJuiceMakingButton -> {
                        viewModel.onTapJuiceMakingButtonOccurred()
                    }

                    is CalendarViewEvent.OnSetMonth -> {
                        viewModel.onMonthChangedOccurred(event.monthDate)
                    }

                    is CalendarViewEvent.OnSetWeek -> {
                        viewModel.onWeekChangeOccurred(event.weekDate)
                    }

                    CalendarViewEvent.OnShowJuiceShakeDialog -> showShakeJuiceDialog()
                    is CalendarViewEvent.OnShowFruitDetailDialog -> showFruitDetailDialog(
                        event.fruit,
                    )

                    is CalendarViewEvent.OnTapFruitDetailButton -> {
                        viewModel.onTapFruitDetailButtonOccurred(event.fruit)
                    }

                    is CalendarViewEvent.OnShowToast -> {
                        showToast(event.message)
                    }

                    is CalendarViewEvent.OnShowJuiceDetailDialog -> showJuiceDetailDialog(
                        event.juice,
                    )

                    is CalendarViewEvent.OnServerMaintaining -> blockApp(event.message)
                }
            }
        }
    }

    private fun blockApp(message: String) {
        showToast(message)
        ActivityCompat.finishAffinity(requireActivity())
    }

    private fun showToast(message: String) =
        CustomToast.createAndShow(requireActivity(), message)

    private fun subscribeViewState() {
        viewLifecycleOwner.repeatOnStarted {
            viewModel.uiState.collect { state ->
                binding.frientreeCalendar.submitList(
                    state.fruitListForMonth.map {
                        it.toFruitInCalendar(state.selectedWeek, state.nowDate.monthValue)
                    },
                )
                binding.frientreeCalendar.setNowDate(state.nowDate)
                when (state) {
                    is CalendarViewState.JuiceAbsentState -> {
                        setViewsVisibility(isJuicePresent = false)
                        updateFruitListView(state)
                        setJuiceCreatableStatusView(state)
                        setTodayFruitStatisticsView(state)
                    }

                    is CalendarViewState.JuicePresentState -> {
                        setViewsVisibility(isJuicePresent = true)
                        binding.juiceGraph.setFruitList(state.juice.fruitList)
                        updateFruitListView(state)
                        setTodayFruitStatisticsView(state)
                    }
                }
            }
        }
    }

    private fun setViewsVisibility(isJuicePresent: Boolean) {
        binding.juiceOfWeekTextView.visibility = if (isJuicePresent) View.VISIBLE else View.GONE
        binding.juiceOfWeekInfoConstraintLayout.visibility =
            if (isJuicePresent) View.VISIBLE else View.GONE
        binding.juiceMakingButtonLinearLayout.visibility =
            if (isJuicePresent) View.GONE else View.VISIBLE
        binding.juiceReadyTextView.visibility = if (isJuicePresent) View.GONE else View.VISIBLE
        binding.notEnoughFruitsTextView.visibility = if (isJuicePresent) View.GONE else View.VISIBLE
        binding.juiceRequirementsTextView.visibility =
            if (isJuicePresent) View.GONE else View.VISIBLE
    }

    private fun updateFruitListView(state: CalendarViewState) {
        binding.fruitListRecyclerView.adapter = fruitListAdapter
        fruitListAdapter.submitList(state.fruitListForWeek)
        val countList = countFruits(state.fruitListForWeek)
        binding.littleFruitListRecyclerView.adapter = littleFruitListAdapter
        littleFruitListAdapter.submitList(countList)
        binding.fruitListLinearLayout.gravity = Gravity.CENTER_VERTICAL
    }

    private fun setTodayFruitStatisticsView(state: CalendarViewState) {
        when (state.todayFruitCreationStatus) {
            TodayFruitCreationStatus.Created -> {
                binding.todayFruitStatisticsTextView.visibility = View.VISIBLE
                binding.todayFruitStatisticsTextView.text =
                    getString(R.string.today_statistics).format(state.todayFruitStatistics)
            }

            TodayFruitCreationStatus.NotCreated -> {
                binding.todayFruitStatisticsTextView.visibility = View.GONE
            }
        }
    }

    private fun setJuiceCreatableStatusView(state: CalendarViewState.JuiceAbsentState) {
        when (state.juiceCreatableStatus) {
            JuiceCreatableStatus.JuiceCreatable -> {
                binding.juiceReadyTextView.visibility = View.VISIBLE
                binding.notEnoughFruitsTextView.visibility = View.GONE
                binding.juiceRequirementsTextView.visibility = View.GONE
                binding.juiceMakingButtonLinearLayout.isClickable = true
                binding.juiceMakingButtonLinearLayout.setBackgroundResource(
                    R.drawable.btn_white_green_36dp,
                )
                binding.juiceMakingButtonImageView.setImageResource(R.drawable.juice)
                binding.juiceMakingButtonTextView.setTextColor(
                    resources.getColor(R.color.black, null),
                )
            }

            else -> {
                binding.juiceReadyTextView.visibility = View.GONE
                binding.notEnoughFruitsTextView.visibility = View.VISIBLE
                binding.juiceRequirementsTextView.visibility = View.VISIBLE
                binding.juiceRequirementsTextView.text = state.juiceCreatableStatus.message
                binding.juiceMakingButtonLinearLayout.isClickable = false
                binding.juiceMakingButtonLinearLayout.setBackgroundResource(
                    R.drawable.btn_lightgray_lightgray_36dp,
                )
                binding.juiceMakingButtonImageView.setImageResource(R.drawable.juice_stroke)
                binding.juiceMakingButtonTextView.setTextColor(
                    resources.getColor(R.color.gray, null),
                )
            }
        }
    }

    private fun showJuiceDetailDialog(juice: Juice) {
        createFullScreenDialog()
        val dialogBinding = DialogJuiceDetailBinding.inflate(layoutInflater)
        Glide.with(dialogBinding.root).load(juice.juiceImageUrl).into(
            dialogBinding.juiceImageImageView,
        )
        dialogBinding.juiceNameTextView.text = juice.juiceName
        dialogBinding.juiceDescriptionTextView.text = juice.juiceDescription
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    private fun showFruitDetailDialog(fruit: Fruit) {
        FruitDetailDialog.newInstance(fruit.toFruitUiModel()).show(childFragmentManager, null)
    }

    private fun showShakeJuiceDialog() {
        createFullScreenDialog()
        val dialogBinding = DialogJuiceShakeBinding.inflate(layoutInflater)
        val progressBar =
            dialogBinding.shakeProgressBarLinearProgressIndicator
        shakeSensor = ShakeSensorModule(
            requireActivity(),
            object : ShakeEventListener {
                override fun onShakeSensed() {
                    if (progressBar.progress < progressBar.max) {
                        progressBar.progress += 3
                    }

                    if (progressBar.progress >= progressBar.max) {
                        progressBar.progress = progressBar.max
                        shakeSensor.stop()
                        viewModel.onCompleteJuiceShake()
                        dialog.dismiss()
                    }
                }
            },
        )
        dialog.setContentView(dialogBinding.root)
        dialog.setOnDismissListener { shakeSensor.stop() }
        dialog.show()
        shakeSensor.start()
    }

    private fun createFullScreenDialog() {
        dialog = Dialog(requireActivity(), R.style.Base_FTR_FullScreenDialog).apply {
            window?.setBackgroundDrawableResource(R.drawable.bg_white_radius_30dp)
        }
    }

    private fun countFruits(fruits: List<Fruit>): List<Pair<LittleFruitImageUrl, Int>> {
        val counts = mutableMapOf<LittleFruitImageUrl, Int>()

        FruitEmotion.entries.forEach { emotion ->
            fruits.forEach { fruit ->
                if (fruit.fruitEmotion == emotion) {
                    counts[fruit.calendarImageUrl] =
                        counts.getOrDefault(fruit.calendarImageUrl, 0) + 1
                }
            }
        }

        return counts.map { (key, count) -> Pair(key, count) }
    }

    override fun onStop() {
        super.onStop()
        if (::shakeSensor.isInitialized) shakeSensor.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.fxzs.lingxiagent.view.guide

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.util.SharedPreferencesUtil
import com.fxzs.lingxiagent.view.common.BaseFragment
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty


class GuideBannerFragment : BaseFragment<VMEmpty>() {

    var tvStart: TextView? = null
    var viewPager: ViewPager2? = null
    private lateinit var indicatorContainer: LinearLayout
    private val fragments = listOf(
        Banner2Fragment(),
        Banner3Fragment(),
        Banner1Fragment(),
        Banner4Fragment()
    )

    private val handler = Handler(Looper.getMainLooper())
    private var currentPage = 0
    private val autoScrollDelay = 3000L

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (fragments.isNotEmpty()) {
                currentPage = (currentPage + 1) % fragments.size
                viewPager?.setCurrentItem(currentPage, true)
                handler.postDelayed(this, autoScrollDelay)
            }
        }
    }

    override fun getLayoutResource(): Int {
        return R.layout.fragment_banner
    }

    override fun getViewModelClass(): Class<VMEmpty> {
        return VMEmpty::class.java
    }

    override fun initializeViews(view: View?) {
        tvStart = view?.findViewById(R.id.id_tv_start)
        viewPager = view?.findViewById(R.id.id_viewpager)
        tvStart?.setOnClickListener {
//            SharedPreferencesUtil.saveFirstOpen(false)
            (activity as? GuideActivity)?.openScheme()
        }
        setupViewPager()
        setupIndicators()
        startAutoScroll()
    }

    private fun setupViewPager() {
        val adapter = activity?.let { BannerViewPagerAdapter(it, fragments) }
        viewPager?.adapter = adapter

        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        stopAutoScroll()
                    }

                    ViewPager2.SCROLL_STATE_IDLE -> {
                        startAutoScroll()
                    }

                    ViewPager2.SCROLL_STATE_SETTLING -> {

                    }
                }
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                updateIndicators(position)
            }
        })

        viewPager?.offscreenPageLimit = 1
    }

    private fun setupIndicators() {
        indicatorContainer = findViewById(R.id.indicatorContainer)
        indicatorContainer.removeAllViews()

        for (i in fragments.indices) {
            val indicator = ImageView(activity)
            val params = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.dp_16),
                resources.getDimensionPixelSize(R.dimen.dp_8)
            )
            params.marginStart = resources.getDimensionPixelSize(R.dimen.dp_4)
            params.marginEnd = resources.getDimensionPixelSize(R.dimen.dp_4)
            indicator.layoutParams = params

            indicator.setImageResource(
                if (i == 0) R.drawable.indicator_selected
                else R.drawable.indicator_normal
            )
            indicatorContainer.addView(indicator)
        }
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until indicatorContainer.childCount) {
            val indicator = indicatorContainer.getChildAt(i) as ImageView
            indicator.setImageResource(
                if (i == position) R.drawable.indicator_selected
                else R.drawable.indicator_normal
            )
        }
    }

    private fun startAutoScroll() {
        handler.removeCallbacks(autoScrollRunnable)
        handler.postDelayed(autoScrollRunnable, autoScrollDelay)
    }

    private fun stopAutoScroll() {
        handler.removeCallbacks(autoScrollRunnable)
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoScroll()
    }

    override fun setupDataBinding() {
    }

    override fun setupObservers() {
    }
}
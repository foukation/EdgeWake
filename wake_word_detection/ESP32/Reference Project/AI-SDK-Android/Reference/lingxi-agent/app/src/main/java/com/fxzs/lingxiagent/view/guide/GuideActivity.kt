package com.fxzs.lingxiagent.view.guide

import android.R.attr.fragment
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.fxzs.lingxiagent.JumpParameterManager.openScheme
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.util.SharedPreferencesUtil
import com.fxzs.lingxiagent.view.common.BaseActivity
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty
import timber.log.Timber

/**
 *创建者：ZyOng
 *描述：引导
 *创建时间：2026/4/9 16:54
 */
class GuideActivity : BaseActivity<VMEmpty>() {
    companion object {
        var isNext = false
    }

    private var fragmentContainer: FrameLayout? = null
    private var ivBack: ImageView ?= null

    override fun getLayoutResource(): Int {
        return R.layout.activity_guide
    }

    override fun getViewModelClass(): Class<VMEmpty> {
        return VMEmpty::class.java
    }

    override fun setupDataBinding() {
    }

    override fun initializeViews() {
//        if (!SharedPreferencesUtil.getFirstOpen()) {
//            openScheme()
//            return
//        }
        val content = intent.getStringExtra("from")
        fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        ivBack = findViewById<ImageView>(R.id.iv_back)
        if (content == "setting"){
            ivBack?.visibility = View.VISIBLE
        }
        Log.e("测试","isNext = "+isNext)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container,
            if (isNext) GuideBannerFragment() else GuideVoiceSettingFragment()).commit()

        ivBack?.setOnClickListener {
            finish()
        }
    }

    fun openScheme() {
        openScheme("lingxiagent://com.fxzs.lingxiagent", this)
        finish()
    }

    override fun setupObservers() {
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        run loop@ {
            supportFragmentManager.fragments.forEach { fragment ->
                if (fragment is GuideVoiceSettingFragment) {
                    fragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
                    return@loop
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // 遍历所有 Fragment，转发结果
        run loop@ {
            supportFragmentManager.fragments.forEach { fragment ->
                if (fragment is GuideVoiceSettingFragment) {
                    fragment.onActivityResult(requestCode, resultCode, data)
                    return@loop
                }
            }
        }


    }

    fun onGuideEvent() {
        isNext = true
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, GuideBannerFragment()).commit()
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(layoutResource)
        initializeViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        isNext = false
    }


}
package com.fxzs.lingxiagent.view.guide

import android.view.View
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.view.common.BaseFragment
import com.fxzs.lingxiagent.viewmodel.main.VMEmpty


class Banner2Fragment : BaseFragment<VMEmpty>(){
    override fun getLayoutResource(): Int {
        return R.layout.fragment_banner2
    }

    override fun getViewModelClass(): Class<VMEmpty> {
        return VMEmpty::class.java
    }

    override fun initializeViews(view: View?) {

    }

    override fun setupDataBinding() {
    }

    override fun setupObservers() {
    }
}
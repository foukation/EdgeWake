package com.fxzs.lingxiagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.HomeModelEntity
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils
import com.fxzs.lingxiagent.model.chat.callback.SuperEditCallback
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean
import com.fxzs.lingxiagent.model.chat.dto.OptionModel
import com.fxzs.lingxiagent.model.common.Constants
import com.fxzs.lingxiagent.util.AppPermissionRequestManager
import com.fxzs.lingxiagent.util.NetworkUtils
import com.fxzs.lingxiagent.util.ShadowUtils
import com.fxzs.lingxiagent.util.SuperEditUtil
import com.fxzs.lingxiagent.util.ZInputMethod
import com.fxzs.lingxiagent.util.ZUtils
import com.fxzs.lingxiagent.util.audio.TTSManager
import com.fxzs.lingxiagent.view.chat.ChatAdapter
import com.fxzs.lingxiagent.view.chat.SuperChatFragment
import com.fxzs.lingxiagent.view.common.AutoRecordView
import com.fxzs.lingxiagent.view.common.BaseActivity
import com.fxzs.lingxiagent.viewmodel.chat.VMChat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lzf.easyfloat.utils.DisplayUtils
import timber.log.Timber

class WakeVoiceActivity1 : BaseActivity<VMChat>() {
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var bottomSheet: ConstraintLayout? = null;

    private var touchTv: LinearLayout? = null
    private var llBottom: LinearLayout? = null
    private var ll_edit_main: LinearLayout? = null

    private var screenHeight = 0
    private var lastY = 0f
    private var collapsedHeight = 0
    private var voiceContent = ""
    private var rvTop: Int? = 0
    private val TAG = "WakeVoiceActivity";
    private var superEditUtil: SuperEditUtil? = null
    private var rvMaxHeight = 0f
    private var voiceRecordView: AutoRecordView? = null
    private val PERMISSION_REQUEST_RECORD_AUDIO: Int = 1
    private val PRESS_DOWN = 1
    private val PRESS_UP = 2
    private val PRESS_MOVE = 3
    var fragmentContainer: FrameLayout? = null
    private var vmChat: VMChat? = null
    var chatAdapter: ChatAdapter? = null
    var superChatFragment: SuperChatFragment? = null
    private var selectOptionModel: OptionModel? = null
    private var lastState = -1


    override fun getLayoutResource(): Int {
        return R.layout.act_wake_voice1
    }

    override fun getViewModelClass(): Class<VMChat> {
        return VMChat::class.java
    }

    override fun setupDataBinding() {

    }

    override fun initializeViews() {
        voiceContent = intent.getStringExtra("formFloatContent") ?: ""
        bottomSheet = findViewById<ConstraintLayout>(R.id.bottomSheet)
        touchTv = findViewById<LinearLayout>(R.id.ll_touch)
        llBottom = findViewById<LinearLayout>(R.id.ll_bottom_edit)
        fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        ll_edit_main = findViewById<LinearLayout?>(R.id.ll_wake_main)

        bottomSheetBehavior = BottomSheetBehavior.from<View>(bottomSheet!!) // 初始化配置
        ShadowUtils.applyDefaultShadow(llBottom!!, this)

        initConfig() // 初始化监听
        initListener() // 初始化点击事件
        initClick()
        initfgView()
        vmChat = ViewModelProvider(this).get<VMChat>(VMChat::class.java)
        vmChat?.setContext(this)
        setBottomEdit()

    }

    override fun setupObservers() {
        vmChat?.sendMsg?.observe(this) { content ->
            Timber.Forest.tag(TAG).d("发送内容%s", content)
            if (!TextUtils.isEmpty(content)) {
                superEditUtil?.sendCommon(content)
            }

        }

    }

    private fun initConfig() {
        screenHeight =
            ScreenUtils.getScreenHeight(this) - DisplayUtils.getStatusBarHeight(this) // 设置折叠高度
        bottomSheetBehavior?.peekHeight = (screenHeight * 0.7).toInt() // 允许隐藏
        bottomSheetBehavior?.isHideable = true
        bottomSheetBehavior?.skipCollapsed = false // 支持半展开
        bottomSheetBehavior?.isFitToContents = false
        bottomSheetBehavior?.halfExpandedRatio = 0.7f

        // 禁用拖拽（可选）使用按钮
        bottomSheetBehavior?.isDraggable = false //        bottomSheetBehavior?.expandedOffset = 0


    }

    private fun initListener() {
        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Timber.Forest.tag(TAG).d("滑动偏移量：$slideOffset")
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Timber.Forest.tag(TAG).d("完全展开")
                        val intent = Intent(this@WakeVoiceActivity1, MainActivity::class.java)
                        intent.putExtra(Constants.REFRESH_STATUS, true)
                        startActivity(intent)
                        finish()
                        overridePendingTransition(0, R.anim.fade_out_instantly)

                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> Timber.Forest.tag(TAG).d("折叠")
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        Timber.Forest.tag(TAG).d("隐藏")
                        finish()
                    }

                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        Timber.Forest.tag(TAG).d("半展开")
                        val intent = Intent(this@WakeVoiceActivity1, MainActivity::class.java)
                        intent.putExtra(Constants.REFRESH_STATUS, true)
                        startActivity(intent)
                        finish()
                        overridePendingTransition(0, R.anim.fade_out_instantly)
                    }

                    else -> Timber.Forest.tag(TAG).d("其他状态：$newState")
                }
            }
        })
    }

    private fun initClick() {

        touchTv?.setOnTouchListener { v, event ->
            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    lastY = event.rawY
                    collapsedHeight = (screenHeight * 0.7f).toInt()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    val dy = lastY - event.rawY
                    Timber.Forest.tag(TAG).d("移动距离%s", dy)
                    lastY = event.rawY
                    fragmentContainer?.updateRvHeight(dy.toInt())
                    val currentPeek = bottomSheetBehavior?.peekHeight ?: collapsedHeight
                    val newPeek = currentPeek + dy.toInt()

                    val minPeek = dp2px(50)
                    val maxPeek = screenHeight
                    bottomSheetBehavior?.peekHeight = newPeek.coerceIn(minPeek, maxPeek)
                    true
                }

                MotionEvent.ACTION_UP -> {

                    val current = bottomSheetBehavior?.peekHeight ?: collapsedHeight
                    val hideThreshold = dp2px(68)
                    Timber.Forest.tag(TAG)
                        .d("当前高度" + bottomSheetBehavior?.peekHeight + "  屏幕高度" + collapsedHeight  + "  event.actionMasked = "+event.actionMasked)
                    when {
                        current > collapsedHeight -> {
                            bottomSheetBehavior?.state =
                                BottomSheetBehavior.STATE_EXPANDED //  bottomSheetBehavior?.setPeekHeight(screenHeight,true)
                            fragmentContainer?.updateRvHeight(rvMaxHeight.toInt())
                        }

                        current < collapsedHeight - hideThreshold -> {
                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                        }

                        else -> {
                            bottomSheetBehavior?.setPeekHeight(collapsedHeight, true)  // 带动画回弹
                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                        }
                    }
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    true
                }

                MotionEvent.ACTION_CANCEL ->{
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    true
                }

                else -> false
            }
        }



    }

    private fun initfgView() { // 示例：为 RecyclerView 设置简单适配器
        superChatFragment = SuperChatFragment(SuperChatFragment.TYPE_WAKE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, superChatFragment!!).commit()

        fragmentContainer?.post {
            rvTop = fragmentContainer?.top ?: 0
            rvMaxHeight = screenHeight - resources.getDimension(R.dimen.dp_52) - rvTop!!

            var layoutParams: ConstraintLayout.LayoutParams =
                fragmentContainer?.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.height =
                (screenHeight * 0.7 - resources.getDimension(R.dimen.dp_100) - rvTop!!).toInt()
            fragmentContainer?.layoutParams = layoutParams
        }

    }

    // dp 转 px
    private fun dp2px(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    // 简单适配器
    inner class SimpleAdapter(private val data: List<String>) : RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tv: TextView = itemView as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context)
            tv.layoutParams =
                RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, dp2px(40))
            tv.setPadding(dp2px(8), dp2px(8), dp2px(8), dp2px(8))
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tv.text = data[position]
        }

        override fun getItemCount(): Int = data.size
    }

    private fun setBottomEdit() {
        superEditUtil = SuperEditUtil(this, ll_edit_main)
        superEditUtil?.setEditType(SuperChatFragment.TYPE_WAKE)
        showVoiceAnimate()
        superEditUtil?.setCallback(object : SuperEditCallback {
            override fun send(content: String, optionModel: OptionModel) {
                ZUtils.print("SuperChatActivity send = " + content + " OptionModel = " + optionModel.getName()) // 检查网络连接
                if (!NetworkUtils.isNetworkAvailable(this@WakeVoiceActivity1)) {
                    showToast("当前无网络连接，请检查后重试")
                    return
                }

                if (vmChat?.streamEnd?.getValue() == false) {
                    ZUtils.showToast("正在输出，稍后。。")
                    return
                }
                if (!content.isEmpty()) {
                    TTSManager.Companion.getInstance().stop()
                    stopMediaPlay()
                    if (selectOptionModel != null && optionModel != null) {
                        ZUtils.print("选中模型: " + selectOptionModel?.name + ", ID: " + selectOptionModel?.getId())
                        ZUtils.print("选中模型optionModel: " + optionModel.getName() + ", ID: " + optionModel.getId())

                    }
                    selectOptionModel = optionModel
                    vmChat?.setSelectOptionModel(optionModel)
                    vmChat?.sendMessage(content)

                    // 发送消息后重置新对话状态标志
                    superChatFragment?.resetNewConversationFlag()

                    superChatFragment?.scroll2Last(true) // 收起键盘
                    ZInputMethod.closeInputMethod(this@WakeVoiceActivity1, llBottom)
                }
            }

            override fun sendWithFile(content: String?, selectOptionModel: OptionModel?, fileList: MutableList<ChatFileBean?>, isFile: Boolean) {
            }

            override fun voice() {
                checkAudioPermission()
            }

            override fun keyboard() {
            }

            override fun pressDown() {
                if (ContextCompat.checkSelfPermission(this@WakeVoiceActivity1,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { //                    Timber.tag(TAG).e("superEditUtil 录音无权限");
                    //                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
                    //                        // 用户拒绝过，需要给出解释
                    //                        Timber.tag(TAG).e("superEditUtil 录音权限被拒绝");
                    //                    } else {
                    //                        // 直接请求权限
                    //                        ActivityCompat.requestPermissions(requireActivity(),
                    //                                new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                    //                    }
                    AppPermissionRequestManager.requestAudioPermission(this@WakeVoiceActivity1,
                        SuperChatFragment.PERMISSION_REQUEST_RECORD_AUDIO,
                        AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR)
                } else { // 已经有权限
                    superChatFragment?.setAutoRecordView(voiceRecordView)
                    superChatFragment?.voiceStatusHandle(PRESS_DOWN, false, false)
                }
            }

            override fun pressUp(isInArea: Boolean) {
                superChatFragment?.voiceStatusHandle(PRESS_UP, isInArea, false)
            }

            override fun voiceMove(status: Boolean) {
                super.voiceMove(status)
                superChatFragment?.voiceStatusHandle(PRESS_MOVE, false, status)
            }

            override fun modeChange(model: OptionModel?, size: Int) {
                super.modeChange(model, size)
                Timber.Forest.tag(TAG).d("选中模型%s", model?.name)
                if (model != null && model.name != null) {
                    selectOptionModel = model
                    vmChat?.setSelectOptionModel(selectOptionModel)
                    superEditUtil?.hideAddLingXi(View.GONE)
                    if (chatAdapter != null) {
                        chatAdapter?.switchHeadCard(HomeModelEntity.ModelType.LING_XI_MODEL)
                    }
                    if (!TextUtils.isEmpty(voiceContent)) { //先获取模型 在发送数据
                        vmChat?.sendMsg?.postValue(voiceContent)
                    }

                }
            }
        })
    }


    private fun showVoiceAnimate() {
        if (voiceRecordView == null) {
            voiceRecordView = findViewById<AutoRecordView?>(R.id.voiceWakeRecordView)
        }

    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            AppPermissionRequestManager.requestAudioPermission(this,
                PERMISSION_REQUEST_RECORD_AUDIO,
                AppPermissionRequestManager.PERMISSION_AUDIO_MESSAGE_ASR)
            Timber.Forest.tag(TAG).e("checkAudioPermission == 录音无权限")
        }
    }

    private fun stopMediaPlay() {
        if (chatAdapter == null && superChatFragment != null) {
            chatAdapter = superChatFragment?.chatAdapter
        }
        if (chatAdapter != null) {
            val position: Int? = chatAdapter?.getPosition()
            if (position != null && position >= 0) {
                chatAdapter?.setMediaStatus(position)
            }
        }
    }

    fun View.updateRvHeight(marginDp: Int) {
        rvTop = fragmentContainer?.top ?: 0
        var layoutParams: ConstraintLayout.LayoutParams =
            fragmentContainer?.layoutParams as ConstraintLayout.LayoutParams
        if (fragmentContainer?.height!! + marginDp > rvMaxHeight) {
            layoutParams.height = rvMaxHeight.toInt()
        } else {
            layoutParams.height = fragmentContainer?.height!! + marginDp
        }
        fragmentContainer?.layoutParams = layoutParams

    }

    fun getDistanceToBottom(view: View): Int {

        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)

        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)

        val viewBottom = viewLocation[1] + view.height

        return rect.bottom - viewBottom
    }

}
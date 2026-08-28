package com.fxzs.lingxiagent

import android.Manifest
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.AgentStatus
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.HomeModelEntity
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils.getScreenHeight2
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils.getScreenWidth
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils.getScreenWidth2
import com.fxzs.lingxiagent.model.chat.callback.SoftCallback
import com.fxzs.lingxiagent.model.chat.callback.SuperEditCallback
import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean
import com.fxzs.lingxiagent.model.chat.dto.OptionModel
import com.fxzs.lingxiagent.util.AppPermissionRequestManager
import com.fxzs.lingxiagent.util.NetworkUtils
import com.fxzs.lingxiagent.util.ShadowUtils
import com.fxzs.lingxiagent.util.SuperEditUtil
import com.fxzs.lingxiagent.util.ZInputMethod
import com.fxzs.lingxiagent.util.ZUtils
import com.fxzs.lingxiagent.util.audio.TTSManager.Companion.getInstance
import com.fxzs.lingxiagent.view.chat.ChatAdapter
import com.fxzs.lingxiagent.view.chat.SuperChatFragment
import com.fxzs.lingxiagent.view.chat.SuperChatFragment.TYPE_WAKE
import com.fxzs.lingxiagent.view.common.AutoRecordView
import com.fxzs.lingxiagent.view.common.BaseActivity
import com.fxzs.lingxiagent.viewmodel.chat.VMChat
import com.lzf.easyfloat.utils.DisplayUtils
import timber.log.Timber

class WakeVoiceActivity : BaseActivity<VMChat>() {
    private var bottomSheet: ConstraintLayout? = null;

    private var touchTv: LinearLayout? = null
    private var llBottom: LinearLayout? = null
    private var ll_edit_main: LinearLayout? = null

    private var screenHeight = 0
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
    private var downY = 0f
    private var startHeight = 0
    private var newHeight = 0
    private var centerHeight = 0
    val STATE_EXPANDED = 0
    val STATE_DEFAULT = -1
    val STATE_HIDE = 1
    var topPanel: View? = null
    var rootView: View ?= null
    var clContent: ConstraintLayout ?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).hide(//隐藏底部导航栏
            WindowInsetsCompat.Type.navigationBars()
        )
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
    }

    override fun getLayoutResource(): Int {
        return R.layout.act_wake_voice
    }

    override fun getViewModelClass(): Class<VMChat> {
        return VMChat::class.java
    }

    override fun setupDataBinding() {

    }

    override fun isScreen(): Boolean {
        return true
    }

    override fun initializeViews() {
        voiceContent = intent.getStringExtra("formFloatContent") ?: ""
        bottomSheet = findViewById<ConstraintLayout>(R.id.bottomSheet)
        touchTv = findViewById<LinearLayout>(R.id.ll_touch)
        llBottom = findViewById<LinearLayout>(R.id.ll_bottom_edit)
        fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        ll_edit_main = findViewById<LinearLayout?>(R.id.ll_wake_main)
        rootView = findViewById<View>(R.id.root_view)
        clContent = findViewById<ConstraintLayout>(R.id.cl_content)
        if (isTablet){
            val orientation = getResources().configuration.orientation
            val maxWidth = if (orientation == Configuration.ORIENTATION_LANDSCAPE) (getScreenHeight2(this)) else getScreenWidth(this)
            val params = clContent?.layoutParams as ConstraintLayout.LayoutParams
            params.width = maxWidth
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            clContent?.setLayoutParams(params)
        }
//        setLayoutWidth(2)
        ShadowUtils.applyDefaultShadow(llBottom!!, this)

        initConfig() // 初始化监听
        initListener() // 初始化点击事件
        initClick()
        initfgView()
        initFragment()
        vmChat = ViewModelProvider(this).get<VMChat>(VMChat::class.java)
        vmChat?.setContext(this)
        setBottomEdit()

    }

    override fun setupObservers() {
        vmChat?.sendMsg?.observe(this) { content ->
            Timber.tag(TAG).d("发送内容%s", content)
            if (!TextUtils.isEmpty(content)) {
                superEditUtil?.sendCommon(content)
            }

        }

        vmChat?.resendMsg?.observe(this) { content ->
            Timber.tag(TAG).d("重置内容%s", content)
            if (!TextUtils.isEmpty(content)) {
                superEditUtil?.setInputContent(content)
            }

        }

    }

    private fun initConfig() {
        screenHeight = getScreenHeight2(this) - DisplayUtils.getStatusBarHeight(this) // 设置折叠高度
        centerHeight = (screenHeight * 0.6 ).toInt()
    }

    private fun initListener() {
    }

    private var isTouchFinished = false
    private fun initClick() {

        touchTv?.setOnTouchListener { v, event ->
            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    downY = event.rawY
                    startHeight = bottomSheet?.height?:0
                    isTouchFinished = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    val dy = downY - event.rawY   // 注意这里
                    newHeight = startHeight + dy.toInt()
                    val minHeight = dp2px(20)
                    val maxHeight = rvMaxHeight.toInt()

                    if (newHeight < minHeight) newHeight = minHeight
                    if (newHeight > maxHeight) newHeight = maxHeight

                    var layoutParams: ConstraintLayout.LayoutParams = bottomSheet?.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.height =newHeight
                    bottomSheet?.layoutParams = layoutParams
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (isTouchFinished) return@setOnTouchListener true
                    isTouchFinished = true
                    Timber.tag(TAG).d("当前操作%s", "松开")
                    if (lastState == -1){
                        lastState +=1
                        if (newHeight >= centerHeight){
                            animateHeight(bottomSheet!!,newHeight,rvMaxHeight.toInt(),STATE_EXPANDED)
                        }else{
                            animateHeight(bottomSheet!!,newHeight,0,STATE_HIDE)
                        }
                    }

                    v.parent.requestDisallowInterceptTouchEvent(false)
                    true
                }

                MotionEvent.ACTION_CANCEL ->{
                    Timber.tag(TAG).d("当前操作%s", "关闭")
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    true
                }

                else -> false
            }
        }


        rootView?.setOnClickListener {
            finish()
        }


    }

    private fun initfgView() {
        bottomSheet?.post {
            rvTop =  0
            rvMaxHeight = screenHeight.toFloat()
        }
        val startHeight = bottomSheet?.height ?: 0
        val targetHeight = (screenHeight * 0.7 - resources.getDimension(R.dimen.dp_60) - rvTop!!).toInt()

        bottomSheet?.viewTreeObserver?.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    bottomSheet?.viewTreeObserver?.removeOnPreDrawListener(this)
                    animateHeight(bottomSheet!!,startHeight,targetHeight,STATE_DEFAULT)
                    return true
                }
            }
        )

    }

    private fun initFragment(){
        superChatFragment = SuperChatFragment(TYPE_WAKE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, superChatFragment!!).commit()
    }

    // dp 转 px
    private fun dp2px(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }


    private fun setBottomEdit() {
        superEditUtil = SuperEditUtil(this, ll_edit_main)
        superEditUtil?.setEditType(TYPE_WAKE)
        showVoiceAnimate()
        superEditUtil!!.setOnListenSoft(ll_edit_main, object : SoftCallback {
            override fun show() {
//                stopAutoScroll(SuperChatFragment.KEY_HISTORY) // 底部导航的显隐交给 MainActivity 全局监听处理，避免重复触发导致布局抖动
//                if (!isSoft) { //                    isUserTouch = false;
//                    //                    scroll2Last(true);
//                    // 空态界面时不执行自动滚动，避免文案随键盘抖动
//                    if (ll_empty == null || ll_empty.getVisibility() != View.VISIBLE) {
//                        sv_chat_list.smoothScrollTo(0, sv_chat_list.getChildAt(0).getBottom())
//                    }
//                }
//                isSoft = true
            }

            override fun hide() {
//                stopAutoScroll(SuperChatFragment.KEY_HISTORY)
//                if (type == SuperChatFragment.TYPE_HOME) { //首页
//                    //                    rv_function.setVisibility(View.VISIBLE);
//                }
//                if (isSoft) { //                    isUserTouch = false;
//                    //                    scroll2Last(true);
//                    sv_chat_list.post(Runnable { // 空态界面时不执行自动滚动，避免文案随键盘抖动
//                        if (ll_empty == null || ll_empty.getVisibility() != View.VISIBLE) {
//                            sv_chat_list.smoothScrollTo(0, sv_chat_list.getChildAt(0).getBottom())
//                        }
//                    })
//                }
//                isSoft = false
            }
        })

        superEditUtil?.setCallback(object : SuperEditCallback {
            override fun send(content: String, optionModel: OptionModel) {
                ZUtils.print("SuperChatActivity send = " + content + " OptionModel = " + optionModel.getName()) // 检查网络连接
                if (!NetworkUtils.isNetworkAvailable(this@WakeVoiceActivity)) {
                    showToast("当前无网络连接，请检查后重试")
                    return
                }

                if (vmChat?.streamEnd?.getValue() == false) {
                    ZUtils.showToast("正在输出，稍后。。")
                    return
                }
                if (!content.isEmpty()) {
                    getInstance().stop()
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
                    ZInputMethod.closeInputMethod(this@WakeVoiceActivity, llBottom)
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
                if (ContextCompat.checkSelfPermission(this@WakeVoiceActivity,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { //                    Timber.tag(TAG).e("superEditUtil 录音无权限");
                    //                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
                    //                        // 用户拒绝过，需要给出解释
                    //                        Timber.tag(TAG).e("superEditUtil 录音权限被拒绝");
                    //                    } else {
                    //                        // 直接请求权限
                    //                        ActivityCompat.requestPermissions(requireActivity(),
                    //                                new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                    //                    }
                    AppPermissionRequestManager.requestAudioPermission(this@WakeVoiceActivity,
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
                Timber.tag(TAG).d("选中模型%s", model?.name)
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
            Timber.tag(TAG).e("checkAudioPermission == 录音无权限")
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



    fun animateHeight(view: View,startHeight: Int, targetHeight: Int, status: Int = STATE_DEFAULT,duration: Long = 300) {
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        val animator = ValueAnimator.ofInt(startHeight, targetHeight)
        animator.duration = 300
        animator.addUpdateListener {
            layoutParams.height = it.animatedValue as Int
            view.layoutParams = layoutParams
        }

        animator.addListener(onEnd = {
            if (status == STATE_HIDE){
                Timber.tag(TAG).d("隐藏半屏")
                finish()
            }else if (status == STATE_EXPANDED){
                Timber.tag(TAG).d("完全展开")
//                val intent = Intent(this@WakeVoiceActivity, MainActivity::class.java)
//                intent.putExtra(Constants.REFRESH_STATUS, true)
//                startActivity(intent)
//                finish()

                val intent = Intent(this@WakeVoiceActivity, MainActivity::class.java)
                val options: ActivityOptions =
                    ActivityOptions.makeSceneTransitionAnimation(this@WakeVoiceActivity,
                        bottomSheet,
                        "white_panel")
                startActivity(intent, options.toBundle())
            }else{

            }
        })
        animator.start()
    }

    fun setLayoutWidth(landscape: Int){
        if (isTablet){
            val maxWidth = getScreenHeight2(this)
            clContent?.let {
                val params = it.layoutParams as ConstraintLayout.LayoutParams
                params.width = if (landscape == 2) maxWidth else  ConstraintLayout.LayoutParams.MATCH_PARENT
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                it.layoutParams = params
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.tag(TAG).d("方向变化=%s", newConfig.orientation)
        initConfig()
        initfgView()
        val params = clContent?.layoutParams as ConstraintLayout.LayoutParams
        if (newConfig.orientation == 1){
            params.width =  getScreenWidth2(this)
        }else{
            params.width =  getScreenHeight2(this)
        }
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        clContent?.setLayoutParams(params)
    }

    override fun onStop() {
        super.onStop()
        if (!AgentStatus.getStatus().equals(AgentStatus.STATUS_RUNNING)){
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getInstance().stop()
    }
}

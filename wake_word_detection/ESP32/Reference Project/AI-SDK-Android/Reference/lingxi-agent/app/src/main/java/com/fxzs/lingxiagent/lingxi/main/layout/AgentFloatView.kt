package com.fxzs.lingxiagent.layout

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import com.fxzs.lingxiagent.MainActivity
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.service.FloatWindowService
import java.util.Timer
import java.util.TimerTask

class AgentFloatView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attributeSet, defStyleAttr) {
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.float_component, this)
        val floatActionButton = view.findViewById<ImageView>(R.id.float_action_button)
        val floatActionClose = view.findViewById<ImageView>(R.id.close_action)

        // 禁用悬浮窗图标
        floatActionButton?.visibility = GONE

        var timer: Timer? = null
        val handler = Handler()
        floatActionButton?.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        floatActionButton?.setOnLongClickListener {
            floatActionClose.visibility = VISIBLE
            if (timer == null) {
                timer = Timer()
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        handler.post { floatActionClose.visibility = GONE; timer?.cancel(); timer = null; }
                    }
                }, 3000)
            }
            true
        }

        floatActionClose.setOnClickListener {
            timer?.cancel()
            timer = null
            context.stopService(Intent(context, FloatWindowService::class.java))
        }
    }
}
package com.fxzs.lingxiagent.layout

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.lingxi.main.helper.FloatHelper

enum class ModelType{
    DEFAULT,
    TASK_TOAST,
    NO_BUT_TOAST,
}

@SuppressLint("ViewConstructor")
class FloatModelView @JvmOverloads constructor(
    context: Context,
    modelType: ModelType = ModelType.DEFAULT,
    description: String?,
    stopText: String = "停止",
    closeText: String = "关闭",
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    ok:()->Unit = {},
    cancel:()->Unit = {}
) : LinearLayout(context, attributeSet, defStyleAttr){
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.float_model_view, this)
        view.findViewById<ImageButton>(R.id.float_model_view_close).setOnClickListener {
            FloatHelper.closeModel()
        }
        view.findViewById<Button>(R.id.close_button).setOnClickListener {
            FloatHelper.closeModel()
        }
        val modalAction = view.findViewById<LinearLayout>(R.id.model_action)
        val toastAction = view.findViewById<LinearLayout>(R.id.toast_action)
        if (modelType == ModelType.DEFAULT) {
            modalAction.visibility = View.VISIBLE
            toastAction.visibility = View.GONE
        } else if(modelType == ModelType.TASK_TOAST) {
            modalAction.visibility = View.GONE
            toastAction.visibility = View.VISIBLE
        }
        
        val textView = view.findViewById<TextView>(R.id.toast_text)
        textView.text = description
        
        val stopTask = view.findViewById<Button>(R.id.stop_task)
        val closeTask = view.findViewById<Button>(R.id.close_task)
        stopTask.text = stopText
        closeTask.text = closeText
        
        stopTask.setOnClickListener { FloatHelper.closeModel(); cancel() }
        closeTask.setOnClickListener { FloatHelper.closeModel(); ok() }
    }
}
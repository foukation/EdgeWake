package com.fxzs.lingxiagent.view.common;

import com.fxzs.lingxiagent.util.AudioRecorderManager;

/**
 * VoiceInputBox 语音输入框组件使用指南
 * 
 * 这是一个基于Figma设计的通用语音输入框组件，支持文本输入和语音录制功能。
 * 
 * ## 功能特性
 * 1. 文本输入：支持键盘输入，实时监听文本变化
 * 2. 语音录制：集成AudioRecorderManager，支持M4A格式录音
 * 3. 智能切换：根据输入状态自动切换语音/发送按钮
 * 4. 权限管理：自动处理录音权限申请
 * 5. 双向绑定：遵循项目MVVM架构规范
 * 
 * ## 设计规格（基于Figma）
 * - 尺寸：343x52dp
 * - 圆角：16dp
 * - 阴影：elevation 4dp
 * - 背景：白色 #FFFFFF
 * - 提示文字：16sp, #B2B2B2
 * - 语音按钮：40x40dp，右侧对齐
 * 
 * ## XML布局使用
 * ```xml
 * <com.fxzs.lingxiagent.view.common.VoiceInputBox
 *     android:id="@+id/voice_input_box"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:layout_margin="@dimen/dp_16" />
 * ```
 * 
 * ## Java代码使用
 * ```java
 * VoiceInputBox voiceInputBox = findViewById(R.id.voice_input_box);
 * 
 * // 设置文本输入监听
 * voiceInputBox.setOnTextInputListener(new VoiceInputBox.OnTextInputListener() {
 *     @Override
 *     public void onTextChanged(String text) {
 *         // 实时文本变化
 *     }
 *     
 *     @Override
 *     public void onSendClicked(String text) {
 *         // 发送文本消息
 *     }
 * });
 * 
 * // 设置语音输入监听
 * voiceInputBox.setOnVoiceInputListener(new VoiceInputBox.OnVoiceInputListener() {
 *     @Override
 *     public void onVoiceRecordStart() {
 *         // 开始录音
 *     }
 *     
 *     @Override
 *     public void onVoiceRecordStop(String audioFilePath) {
 *         // 录音完成，处理音频文件
 *     }
 *     
 *     @Override
 *     public void onVoiceRecordCancel() {
 *         // 录音取消
 *     }
 *     
 *     @Override
 *     public void onPermissionRequired() {
 *         // 需要录音权限
 *         requestRecordPermission();
 *     }
 * });
 * ```
 * 
 * ## 公共方法
 * - `setText(String text)` - 设置输入文本
 * - `getText()` - 获取当前文本
 * - `setHint(String hint)` - 设置提示文字
 * - `clearText()` - 清空输入内容
 * - `setEnabled(boolean enabled)` - 设置启用状态
 * - `requestInputFocus()` - 请求输入焦点
 * 
 * ## 权限要求
 * 在AndroidManifest.xml中添加：
 * ```xml
 * <uses-permission android:name="android.permission.RECORD_AUDIO" />
 * ```
 * 
 * ## 依赖组件
 * - AudioRecorderManager：音频录制管理器
 * - Figma导出资源：ic_voice_input.png, ic_voice_wave.png, selector_send_button.xml
 * - 新增优化发送图标：基于Figma设计的蓝色圆形发送按钮 (#4690FF)
 * 
 * ## 注意事项
 * 1. 组件会自动管理录音状态，无需手动调用停止录音
 * 2. 在Activity.onDestroy()中会自动清理资源
 * 3. 支持动态切换语音/发送按钮状态
 * 4. 遵循项目双向绑定规范，可与ViewModel配合使用
 * 
 * @author Claude Code
 * @see AudioRecorderManager
 * @see VoiceInputDemoActivity
 */
public class VoiceInputBoxGuide {
    // 这是一个文档类，不包含实际代码
}
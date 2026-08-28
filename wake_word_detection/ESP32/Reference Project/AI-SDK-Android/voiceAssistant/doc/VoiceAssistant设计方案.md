# VoiceAssistant 设计方案（唤醒 → 自动对话 编排器）

> 版本：1.0 ｜ 日期：2026-07-10
> 目标读者：SDK 开发者 / 接入厂商
> 所在模块：`voiceAssistant`

---

## 一、做什么（目标）

在已存在的 `voiceAssistant` 模块里实现一个“编排器” `VoiceAssistant`：

**喊“灵犀灵犀” → 自动开始一轮语音对话 → 答完自动回到待唤醒。**

厂商只需 `start()` 一次，麦克风交接、状态流转全部由 SDK 内部完成。

编排的本质 = 把“唤醒引擎”（`VoiceWakeup`）和“已有的对话引擎”（`ASRIntelligentDialogue`）用一个状态机串起来，核心难点是**唤醒 ↔ ASR 的麦克风交接**。

---

## 二、模块与依赖

`voiceAssistant` 模块已创建，并已接入 `settings.gradle` 与 `ai-sdk/build.gradle`。

需要**修改 `voiceAssistant/build.gradle` 的依赖**：

```gradle
// voiceAssistant/build.gradle
dependencies {
    api project(':voiceWakeUp')             // 拿“唤醒封装”VoiceWakeup（第一步已完成）
    api project(':asrIntelligentDialogue')  // 拿“对话”ASRIntelligentDialogue + DialogueResult / AsrResult
    // 移除原来的 api(name:'wakeup-...aar')：
    //   编排层不直接依赖 raw 唤醒引擎，只使用封装好的 VoiceWakeup
}
```

依赖链（单向、无环）：

```
voiceAssistant
  ├─ voiceWakeUp（内含 wakeup aar）
  └─ asrIntelligentDialogue ─(impl)→ aiFoundationKit ─(api)→ deviceAccess_gatewayProxy
```

---

## 三、对外 API

包名：`com.cmdc.ai.assist.voiceAssistant`

```kotlin
class VoiceAssistant {

    // 初始化：传 Context + 配置（不传用默认：内置提示音）
    fun init(context: Context, config: Config = Config())

    // 注册回调，接收 唤醒 / 识别 / 答案 / 状态 / 错误 事件
    fun setListener(listener: Listener)

    fun start()   // 开始工作：进入“待唤醒”，打开唤醒麦
    fun stop()    // 停止一切：回到 IDLE，全部关闭
    fun release() // 释放资源（退出时调用）

    fun triggerManually()      // 免唤醒：不喊也能直接进对话（等于“按住说话”）
    fun notifySpeakFinished()  // ★ 宿主念完 AI 答案后调用，SDK 才重新打开唤醒麦（保证半双工、防误唤醒）
    fun getState(): State      // 查询当前状态

    // ── 状态：表示“现在处于第几步” ──
    enum class State {
        IDLE_LISTENING,  // 待唤醒（唤醒麦开着监听“灵犀灵犀”）
        WAKED,           // 刚唤醒（放提示音、正在交接麦克风）
        ASR_LISTENING,   // 正在听用户说话（ASR 麦开）
        THINKING,        // 用户说完，送服务器生成答案
        SPEAKING         // 宿主正在念答案（此时两个麦都关闭）
    }

    // ── 配置 ──
    class Config {
        @JvmField var wakeupInitConfig = WakeupInitConfig()  // 透传唤醒引擎参数（阈值 / AEC 等）
        @JvmField var promptMode = PromptMode.BUILTIN        // 提示音模式，默认放内置音
        @JvmField var customPromptResId = 0                  // promptMode=CUSTOM 时，宿主自己的音频资源 id
    }

    // ── 提示音模式 ──
    enum class PromptMode {
        BUILTIN,  // 默认：唤醒瞬间放 SDK 内置提示音（复用 lingxi 的 wakeup.mp3）
        CUSTOM,   // 放宿主提供的音频（customPromptResId）
        NONE      // 不放；只在 onWakeup 回调通知宿主，是否播放由宿主决定
    }

    // ── 回调（都带默认空实现，按需重写）──
    interface Listener {
        fun onStateChanged(state: State) {}                 // 状态变化（可用于更新 UI）
        fun onWakeup(word: String) {}                       // 唤醒命中，word = 命中的唤醒词
        fun onAsrResult(asrResult: AsrResult) {}            // 识别结果（含文本、是否最终、情绪标签等）
        fun onDialogueResult(result: DialogueResult) {}     // AI 的对话答案
        fun onError(code: Int, msg: String) {}              // 出错
    }
}
```

---

## 四、状态机 + 麦克风归属

任一时刻只有一个麦被占用（唤醒麦 与 ASR 麦 是两路独立 `AudioRecord`）。

```
IDLE_LISTENING   唤醒麦【开】监听“灵犀灵犀”
   │  onWakeup（唤醒命中）
   ▼
WAKED            先放提示音“叮” → 立刻关唤醒麦 → 打开 ASR 麦
                 （提示音盖住“唤醒关→ASR开”这零点几秒空窗，避免丢头字）
   │
   ▼
ASR_LISTENING    ASR 麦【开】听用户说话（唤醒麦已关）
   │  用户说完（或一直不说话超 30s，服务端断连 + 兜底回复）
   ▼
THINKING         送服务器，等待 AI 答案（onDialogueResult）
   │
   ▼
SPEAKING         两个麦【全关】，宿主念答案
                 （半双工：避免喇叭声被麦收进去 / 答案含“灵犀”导致误唤醒）
   │  宿主念完 → 调 notifySpeakFinished()
   ▼
IDLE_LISTENING   唤醒麦【重新打开】，回到待唤醒，循环

（对话进行中若再次触发唤醒 → 忽略，不打断当前对话）
```

各状态下的麦克风 / 喇叭占用：

| 状态 | 唤醒麦 | ASR 麦 | 喇叭(TTS，宿主) |
|---|---|---|---|
| IDLE_LISTENING | 开 | 关 | 静 |
| WAKED | 正在关 | 准备开 | 放提示音 |
| ASR_LISTENING | 关 | 开 | 静 |
| THINKING | 关 | 开（收尾） | 静 |
| SPEAKING | 关 | 关 | 响 |
| → IDLE_LISTENING | 重新开 | 关 | 静 |

---

## 五、关键决策（附“为什么”）

| 项 | 结论 | 为什么 |
|---|---|---|
| 麦克风交接 | 唤醒 ↔ ASR 串行；进 onWakeup 先关唤醒 | 两个独立 `AudioRecord` 抢同一物理麦，不串行会 `IllegalStateException` |
| 半双工 | 念答案时两麦全关，靠 `notifySpeakFinished()` 重开唤醒 | 否则喇叭声被麦收进去 / 答案含“灵犀”会误唤醒 |
| 提示音 | 内置复用 `wakeup.mp3`，临时 `MediaPlayer` 播完即释放；可 CUSTOM/NONE | 给用户“可以说话了”的反馈 + 盖住交接空窗；轻量、不依赖 TTS |
| 不说话 | 靠服务端 30s 断连 + 兜底回复 → `onComplete` | `ASRIntelligentDialogue` 由服务端判定结束，无需本地 VAD |
| 防重入 | 布尔安全网 | 进 onWakeup 已先关唤醒，几乎不会重触，留一个保险 |
| 每轮对话 | new 新 ASR 实例 | `ASRIntelligentDialogue` release 后即作废，不能复用 |
| 单轮 | 答完回待唤醒（不做多轮） | 简单稳；多轮后续再加 |
| TTS | SDK 不做，念答案交宿主 | 保持 SDK 半双工、不引入 TTS 引擎 |

---

## 六、提示音（“我在 / 叮”）说明

- **目的 1（主要）**：给用户“叮”一声反馈 = “我听到了，你说吧”。否则喊完毫无反应，用户不知道该不该说话。
- **目的 2（次要）**：盖住“唤醒关 → ASR 开”那零点几秒空窗，用户听到提示音才开口，避免丢头字。
- **实现**：复用 `Reference/lingxi-agent/app/src/main/res/raw/wakeup.mp3`，拷入 `voiceAssistant/src/main/res/raw/`；播放用临时 `MediaPlayer`，`setOnCompletionListener` 内 `release`。
- **注意**：这是**播放预录音效**，不是 **TTS 合成**（不引入 TTS 引擎）。念 AI 的动态答案才需要 TTS，那部分交给宿主。

---

## 七、交付方式（门面）

厂商入口与现有能力保持一致，通过 `AIAssistantManager` 反射获取：

```kotlin
// AIAssistantManager 新增
fun voiceAssistantHelp(): Any? = try {
    Class.forName("com.cmdc.ai.assist.voiceAssistant.VoiceAssistant")
        .getMethod("getInstance").invoke(null)
} catch (e: Exception) { null }
```

厂商用法：

```java
VoiceAssistant va = (VoiceAssistant) AIAssistantManager.getInstance().voiceAssistantHelp();
va.init(context);
va.setListener(new VoiceAssistant.Listener() {
    @Override public void onWakeup(String word) { /* 唤醒了 */ }
    @Override public void onAsrResult(AsrResult asrResult) { /* 识别结果 */ }
    @Override public void onDialogueResult(DialogueResult result) { /* AI 答案；念完调 va.notifySpeakFinished() */ }
});
va.start();
```

> 备注：设计文档 `app-voice-wake-up/doc/语音唤醒能力集成方案.md` §8 原写门面为 `AIFoundationKit.voiceAssistantHelp()`；本方案改挂 `AIAssistantManager`，与现有 `asrIntelligentDialogueHelp()` 等能力入口一致（可按需调整）。

---

## 八、落地改动清单

1. 改 `voiceAssistant/build.gradle`：依赖换成 `voiceWakeUp` + `asrIntelligentDialogue`
2. 拷 `wakeup.mp3` → `voiceAssistant/src/main/res/raw/`（内置提示音）
3. 新建 `voiceAssistant/src/main/java/com/cmdc/ai/assist/voiceAssistant/VoiceAssistant.kt`（状态机 + 麦克风交接 + 提示音，含完整中文注释）
4. `AIAssistantManager.kt` 新增 `voiceAssistantHelp()`（反射）
5. `ai-sdk/proguard-rules.pro` 第 9 节新增 `-keep class com.cmdc.ai.assist.voiceAssistant.** { *; }`
6. `get_errors` 校验 + gradle 构建验证

---

## 九、前提 & 不做

- **前提**：宿主先调用 `AIAssistantManager.initialize(context, config)` 配好鉴权 URL；`VoiceAssistant` 只负责编排，不管鉴权。
- **不做**：多轮连续对话、内置 TTS 播报、清对话历史、fat-aar embed 切换（均可作为后续可选项）。

---

## 十、一句话总结

**`VoiceAssistant` = 一个“总管家”，自动指挥“唤醒”和“对话”轮流用麦，让用户喊一声就能对话，全程免手动。核心是麦克风的串行交接与半双工。**

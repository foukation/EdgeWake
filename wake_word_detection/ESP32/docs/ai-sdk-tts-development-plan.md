# ai-sdk TTS 功能开发计划

---

## 大类一：基础设施

### 1.1 添加组件依赖
- 修改 `ai_sdk_builder/main/idf_component.yml`
- 添加 espressif/esp_audio_codec
- 添加 espressif/esp_audio_effects
- 添加 espressif/esp_codec_dev

### 1.2 创建目录结构
- 创建 `include/ai_sdk/audio/` 目录
- 创建 `src/audio/` 目录
- 创建 `src/audio/codecs/` 目录

---

## 大类二：配置层

### 2.1 硬件类型枚举
- 定义 `AudioHardwareType` 枚举
- 包含 ES8311, ES8388, BOX, NO_CODEC

### 2.2 硬件配置结构体
- 定义 `AudioConfig` 结构体
- I2S 引脚 (mclk, bclk, ws, dout, din)
- I2C 引脚 (sda, scl, port, addr)
- I2C 总线句柄 (i2c_bus_handle)
- 功放引脚 (pa_pin)
- 采样率配置

---

## 大类三：硬件驱动层

### 3.1 硬件抽象基类
- 定义 `IAudioCodec` 接口
- init() 初始化
- open() 打开设备
- close() 关闭设备
- write() 写入 PCM
- setVolume() 设置音量

### 3.2 ES8311 驱动
- 实现 `Es8311Codec` 类
- I2C 控制接口初始化
- I2S 数据接口初始化
- esp_codec_dev 初始化

### 3.3 ES8388 驱动
- 实现 `Es8388Codec` 类
- 类似 ES8311，使用 es8388_codec_new()

### 3.4 BOX 驱动
- 实现 `BoxCodec` 类
- ES8311 (播放) + ES7210 (录音) 组合

### 3.5 NoCodec 驱动
- 实现 `NoCodec` 类
- 直接使用 driver/i2s
- 不需要 I2C

### 3.6 Codec 工厂
- 实现 `createAudioCodec(AudioConfig)` 工厂函数
- 根据 type 创建对应驱动

---

## 大类四：解码层

### 4.1 解码器抽象
- 定义 `IAudioDecoder` 接口
- decode() 解码方法
- getSampleRate() 获取采样率
- getChannels() 获取通道数

### 4.2 MP3 解码器
- 实现 `Mp3Decoder` 类
- 封装 esp_mp3_dec_*

### 4.3 Opus 解码器
- 实现 `OpusDecoder` 类
- 封装 esp_opus_dec_*

### 4.4 解码器工厂
- 实现 `createDecoder(format)` 工厂函数

---

## 大类五：重采样层

### 5.1 重采样器
- 封装 esp_ae_rate_cvt_*
- 支持任意采样率转换

---

## 大类六：TTS 播放器

### 6.1 TtsPlayer 类接口
- init(AudioConfig) 初始化
- playUrl(url, format) 播放
- stop() 停止
- pause() 暂停
- resume() 恢复
- setVolume(volume) 设置音量
- isPlaying() 状态查询

### 6.2 HTTP 流式下载
- 使用 esp_http_client
- 流式读取音频数据
- 支持 HTTPS

### 6.3 播放流程管理
- HTTP 下载任务
- 解码处理
- 重采样处理
- PCM 输出

### 6.4 播放状态管理
- 线程安全的状态标志
- 播放/暂停/停止状态切换

---

## 大类七：集成层

### 7.1 AsrIntelligentDialogue 集成
- 添加 initAudio() 方法
- 添加 getTtsPlayer() 方法
- 添加 setAutoPlayTts() 方法

### 7.2 DialogueResult 处理
- 检测 directive: "Speak"
- 解析 payload 中的 url 和 format
- 调用 TtsPlayer 播放

### 7.3 ASR/TTS 状态管理
- 播放时暂停记录 (可选)
- 播放完成回调

---

## 大类八：测试

### 8.1 ES8311 硬件测试
- 使用开发板测试

### 8.2 ES8388 硬件测试
- 使用开发板测试

### 8.3 NoCodec 硬件测试
- I2S 直连设备测试

### 8.4 MP3 播放测试
- 测试不同采样率的 MP3

### 8.5 Opus 播放测试
- 测试不同采样率的 Opus

---

## 大类九：文档

### 9.1 API 文档
- AudioConfig 使用说明
- TtsPlayer API 说明

### 9.2 集成指南
- 厂商集成步骤
- 示例代码

### 9.3 硬件配置参考
- 各种硬件的配置示例

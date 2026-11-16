# 随手说 · VoiceNotes

一个「微信式」语音速记安卓 App：**点按输入栏打字，按住输入栏说话，松手自动转文字并记下来**。深色界面，录音与文字全部保存在本机，语音识别走阿里云百炼 DashScope。

## 交互方式

| 操作 | 效果 |
|---|---|
| 点按输入栏 | 切换为打字模式，弹出键盘，输入后点右侧按钮保存 |
| 按住输入栏（含左侧声波按钮） | 直接开始录音，**不会弹出键盘**；底部弹出「取消 / 编辑」面板，中央显示计时与实时识别文字 |

> 输入栏默认处于语音模式（显示为纯文字占位，不是可聚焦的输入框），所以长按绝不会误弹输入法；只有短按才会切换成真正的输入框并弹出键盘。发送后自动回到语音模式。
| 按住后左滑到「取消」 | 该按钮变红，松手丢弃，不保存 |
| 按住后右滑到「编辑」 | 该按钮变绿，松手后文字进入输入框，可修改再点发送 |
| 原地松手 | 转写完成，自动记一条（含语音文件） |

其余能力：

- **语音文件本地保存**：每次录音写成 WAV 存到 `filesDir/recordings/`，不需要存储权限。
- **查看时长**：每条语音记录显示录音时长（如 `语音 0:08`），点气泡旁的播放键可回听。
- **再次编辑**：点气泡进入编辑框，可改文字或删除记录（删除时一并删掉 WAV）。
- **暗黑模式**：设置里可选「跟随系统 / 深色 / 浅色」，默认深色。
- **本地优先**：百炼连不上时仍照常录音保存，只跳过转写。

## 快速开始（零基础）

1. 打开[阿里云百炼控制台](https://bailian.console.aliyun.com/) → 模型设置 → API Key，创建一个 Key。
2. 用 Android Studio 打开本项目（或用命令行 `./gradlew assembleDebug`），把 APK 装到安卓 8.0 以上手机。
3. 首次启动会自动弹出设置框，填入 API Key，其余保持默认，点「保存」。
4. 系统弹麦克风权限时点「允许」，即可按住说话。

> API Key 仅写入手机本地的 SharedPreferences，代码里没有硬编码，仓库里也不会有你的 Key。
> 识别模型默认为 `paraformer-realtime-v2`，可在设置里更换为百炼支持的其他实时识别模型。

## 技术栈

- Kotlin 2.0.21 + Jetpack Compose（Material 3）
- AudioRecord 采集 16kHz / 16bit / 单声道 PCM，每 100ms 一帧；同一帧写入 WAV 文件
- OkHttp WebSocket 双向流式对接百炼实时语音识别
- 笔记用 JSON 存 `filesDir/notes.json`，语音用 WAV 存 `filesDir/recordings/`，完全离线可读
- Material 3 深色 / 浅色 / 跟随系统三档主题

## 百炼接口说明

采用**实时识别**而非文件转写：文件转写接口需要公网可下载的音频 URL（Alibaba Cloud 2026a），手机本地录音无法满足；实时接口直接推二进制 PCM 帧，松手后发送 `finish-task` 即可拿到定稿文本（Alibaba Cloud 2026b, 2026c）。

请求域名默认 `wss://dashscope.aliyuncs.com/api-ws/v1/inference`。如果在设置里填了业务空间 ID，则改用 `wss://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/api-ws/v1/inference`。两个域名均由官方文档给出（Alibaba Cloud 2026b）。

## 目录

```
app/src/main/java/com/liweiwen/voicenotes/
├── MainActivity.kt              # 入口
├── audio/PcmRecorder.kt         # 麦克风 PCM 采集
├── audio/WavWriter.kt           # PCM 封装成 WAV 落盘
├── audio/AudioPlayer.kt         # 本地 WAV 回听
├── asr/DashScopeAsr.kt          # 百炼 WebSocket 流式识别
├── data/Stores.kt               # 本地笔记 + 设置存储
└── ui/
    ├── NoteScreen.kt            # 深色界面 + 长按手势 + 编辑/删除
    ├── NoteBubble.kt            # 记录气泡 + 时长 + 播放键
    └── RecordingPanel.kt        # 底部「取消/编辑」录音面板
```

## 构建

```bash
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk
```

## 可验证事实与个人推测

- **可验证事实**：接口地址、鉴权头、事件名、字段名均来自阿里云帮助中心官方文档；本项目源码可逐行核对。
- **个人推测**：Gesture 阈值（80dp）、长按判定（260ms）为手感调参值，不同机型可能需微调。

## References / Works Cited

Alibaba Cloud. “Speech-to-Text Models for Real-Time and File Transcription.” *Alibaba Cloud Model Studio*, 12 Nov. 2026, help.aliyun.com/en/model-studio/asr-model. Accessed 16 Nov. 2026.

Alibaba Cloud. “Real-time Speech Recognition - Qwen.” *Alibaba Cloud Model Studio*, 12 Nov. 2026, help.aliyun.com/en/model-studio/real-time-speech-recognition-user-guide. Accessed 16 Nov. 2026.

Alibaba Cloud. “Paraformer Real-time Speech Recognition Client Events.” *Alibaba Cloud Model Studio*, 10 Oct. 2026, help.aliyun.com/en/model-studio/paraformer-client-events. Accessed 16 Nov. 2026.

Alibaba Cloud. “Server-sent Events for Real-time Speech Recognition (Paraformer).” *Alibaba Cloud Model Studio*, 10 Oct. 2026, help.aliyun.com/en/model-studio/paraformer-server-events. Accessed 16 Nov. 2026.

## License

MIT © 2026 liweiwen

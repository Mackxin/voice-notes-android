# 随手说 · VoiceNotes

一个「微信式」语音速记安卓 App：**点按输入框打字，长按麦克风说话，松手自动转文字并记下来**。录音音频不落盘、笔记只存本机，语音识别走阿里云百炼 DashScope。

## 交互方式

| 操作 | 效果 |
|---|---|
| 点按输入框 | 正常打字，点右侧发送按钮保存 |
| 短按绿色麦克风 | 无操作（避免误触） |
| 长按麦克风 | 开始录音，屏幕上实时显示识别出的文字 |
| 长按后向左滑 | 松手取消，不保存 |
| 长按后向右滑 | 松手后文字进入输入框，可修改再点发送 |
| 原地松手 | 转写完成，自动记一条 |

点击气泡复制文字；长按气泡删除。

## 快速开始（零基础）

1. 打开[阿里云百炼控制台](https://bailian.console.aliyun.com/) → 模型设置 → API Key，创建一个 Key。
2. 用 Android Studio 打开本项目（或用命令行 `./gradlew assembleDebug`），把 APK 装到安卓 8.0 以上手机。
3. 首次启动会自动弹出设置框，填入 API Key，其余保持默认，点「保存」。
4. 系统弹麦克风权限时点「允许」，即可按住说话。

> API Key 仅写入手机本地的 SharedPreferences，代码里没有硬编码，仓库里也不会有你的 Key。
> 识别模型默认为 `paraformer-realtime-v2`，可在设置里更换为百炼支持的其他实时识别模型。

## 技术栈

- Kotlin 2.0.21 + Jetpack Compose（Material 3）
- AudioRecord 采集 16kHz / 16bit / 单声道 PCM，每 100ms 一帧
- OkHttp WebSocket 双向流式对接百炼实时语音识别
- 笔记用 JSON 文件保存在 `filesDir/notes.json`，完全离线可读

## 百炼接口说明

采用**实时识别**而非文件转写：文件转写接口需要公网可下载的音频 URL（Alibaba Cloud 2026a），手机本地录音无法满足；实时接口直接推二进制 PCM 帧，松手后发送 `finish-task` 即可拿到定稿文本（Alibaba Cloud 2026b, 2026c）。

请求域名默认 `wss://dashscope.aliyuncs.com/api-ws/v1/inference`。如果在设置里填了业务空间 ID，则改用 `wss://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/api-ws/v1/inference`。两个域名均由官方文档给出（Alibaba Cloud 2026b）。

## 目录

```
app/src/main/java/com/liweiwen/voicenotes/
├── MainActivity.kt              # 入口
├── audio/PcmRecorder.kt         # 麦克风 PCM 采集
├── asr/DashScopeAsr.kt          # 百炼 WebSocket 流式识别
├── data/Stores.kt               # 本地笔记 + 设置存储
└── ui/NoteScreen.kt             # 聊天式界面 + 微信式手势
```

## 构建

```bash
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk
```

## 可验证事实与个人推测

- **可验证事实**：接口地址、鉴权头、事件名、字段名均来自阿里云帮助中心官方文档；本项目源码可逐行核对。
- **个人推测**：Gesture 阈值（72dp）、长按判定（260ms）为手感调参值，不同机型可能需微调。

## References / Works Cited

Alibaba Cloud. “Speech-to-Text Models for Real-Time and File Transcription.” *Alibaba Cloud Model Studio*, 12 Nov. 2026, help.aliyun.com/en/model-studio/asr-model. Accessed 16 Nov. 2026.

Alibaba Cloud. “Real-time Speech Recognition - Qwen.” *Alibaba Cloud Model Studio*, 12 Nov. 2026, help.aliyun.com/en/model-studio/real-time-speech-recognition-user-guide. Accessed 16 Nov. 2026.

Alibaba Cloud. “Paraformer Real-time Speech Recognition Client Events.” *Alibaba Cloud Model Studio*, 10 Oct. 2026, help.aliyun.com/en/model-studio/paraformer-client-events. Accessed 16 Nov. 2026.

Alibaba Cloud. “Server-sent Events for Real-time Speech Recognition (Paraformer).” *Alibaba Cloud Model Studio*, 10 Oct. 2026, help.aliyun.com/en/model-studio/paraformer-server-events. Accessed 16 Nov. 2026.

## License

MIT © 2026 liweiwen

# 随手说 · VoiceNotes

一个「微信式」语音速记安卓 App（v1.3.0）：**底部输入栏有两种模式——「可按住转文字」和「按住说话」，点左下角按钮即可来回切换，切换带滑动淡入与配色渐变过渡**（对齐微信 8.0.80 的「可按住 转文字」交互）。录音与文字全部保存在本机，语音识别走阿里云百炼 DashScope；声纹光球录音动效、卡片式记录、首页概览、剪贴板快贴一应俱全。

## 底部两种模式（左下角按钮切换）

| 模式 | 占位文字 | 点按文字区 | 长按后原地松手 | 左滑取消 | 右滑 |
|---|---|---|---|---|---|
| **可按住转文字**（默认，薄荷绿） | 可按住 转文字 | 直接打字（弹键盘） | **语音转成文字并记下** | 丢弃，不保存 | 转文字进入编辑框，可改后再保存 |
| **按住说话**（紫罗兰） | 按住说话 | 直接打字（弹键盘） | **只保存语音文件**（真实时长） | 丢弃，不保存 | 语音转成文字并记下 |

> 切换动效：左下角图标（声波 ⇄ 话筒）淡入淡出并旋转 180°，强调色在薄荷绿与紫罗兰间渐变，占位文字横向滑动切换。录音过程中以按下瞬间的模式为准，避免中途切换造成歧义。

## 交互方式

| 操作 | 效果 |
|---|---|
| 点左下角按钮 | 在「可按住转文字」与「按住说话」之间切换（非打字时）；打字模式下点它则收键盘退回语音模式 |
| 点右侧文字区 | 切换为打字模式，弹出键盘，输入后点右侧按钮保存 |
| 按住输入栏（含左侧按钮） | 直接开始录音，**不会弹出键盘**；出现声纹光球 + 涟漪动效，左右两枚药丸按钮随滑动高亮 |
| 按住后往左上角滑到「取消」 | 左药丸变红放大，松手丢弃，不保存 |
| 按住后往右上角滑到「语音转文字」 | 右药丸变绿放大，松手后调用百炼转写；转文字模式下进入编辑框，按住说话模式下直接记成文字 |
| 原地松手 | 按住说话模式**只保存语音文件**；转文字模式**转成文字记下**（无 API Key 或没听清时提示重试） |

**打字模式的四种退出方式**（收键盘、退回语音模式）：

1. **点记录列表的空白处**
2. 点输入框右侧的向下箭头「收起键盘」按钮
3. 按键盘上的「完成 / Done」键
4. 按手机系统返回键（打字模式下会先收键盘，不会直接退出 App）

> 两种语音模式的输入栏都是纯文字占位（不是可聚焦的输入框），所以长按绝不会误弹输入法；只有短按文字区才会切换成真正的输入框并弹出键盘。发送后也自动回到语音模式。

其余能力：

- **语音文件本地保存**：每次录音写成 WAV 存到 `filesDir/recordings/`，不需要存储权限。
- **查看时长**：每条语音记录显示真实录音时长（如 `语音 0:08`）；旧版本记录为 0:00 时按 WAV 文件大小自动反推修正，点气泡旁的播放键可回听。
- **再次编辑**：点气泡进入编辑框，可改文字或删除记录（删除时一并删掉 WAV）。
- **暗黑模式**：设置页可选「跟随系统 / 深色 / 浅色」，默认深色。
- **设置页**：点右上角齿轮进入独立的全屏设置页（不再是弹窗），含「数据管理 / 外观 / 语音转写」。
- **数据管理**：设置页可查看记录条数、语音文件数、占用空间；点「刷新」实时读取本机数据，直接显示数据的**本机保存路径**（`notes.json` 与 `recordings/` 目录），两行路径均可点击，通过自定义 DocumentsProvider 在系统「文件」应用里直接显示「随手说本机数据」目录（含 `notes.json` 与 `recordings/`）；并显示最近 20 条记录的文字与时间；并可「清除语音文件（保留文字）」或「清空全部数据」（二次确认）。
- **全局搜索**：首页右上角放大镜可按记录正文、语音标记和显示时间过滤全部记录，返回键或箭头退出搜索。
- **剪贴板快贴**：打开 App 或从后台回来时自动检测复制内容，输入栏上方出现快贴条，点「粘贴」直接填入草稿；复制变化会实时更新。
- **新视觉**：首页概览卡、卡片式记录（带复制图标 + 语音波形/时长）、悬浮输入栏、声纹光球录音页；深色为深蓝底 + 薄荷绿，浅色为雾白底 + 青绿。
- **本地优先**：百炼连不上时仍照常录音保存；按住说话模式下原地松手完全不请求转写。

## 快速开始（零基础）

1. 打开[阿里云百炼控制台](https://bailian.console.aliyun.com/) → 模型设置 → API Key，创建一个 Key。
2. 用 Android Studio 打开本项目（或用命令行 `./gradlew assembleDebug`），把 APK 装到安卓 8.0 以上手机。
3. 点右上角齿轮进入「设置」页，填入 API Key，其余保持默认，点「保存设置」返回。
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
├── data/VoiceNotesDocumentsProvider.kt # 把本机数据目录暴露给系统「文件」应用
└── ui/
    ├── NoteScreen.kt            # 深色界面 + 长按手势 + 编辑/删除
    ├── NoteBubble.kt            # 记录气泡 + 时长 + 播放键
    └── RecordingPanel.kt        # 声纹光球录音面板 + 波形图标
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

# Task: 实现百度H5活体检测

## 背景
当前使用RGB活体检测（单张照片），防攻击能力弱。需要改为H5活体检测（摇头/眨眼/张嘴），防攻击能力强。

## 百度H5活体检测API文档

### 流程
1. **后端**调用 `/rest/2.0/face/v1/faceliveness` 获取 `session_id` 和 `verifydata`
2. **前端**引入百度H5 SDK JS文件
3. **前端**用 `session_id` 初始化SDK，引导用户完成随机动作（左转头、右转头、眨眼、张嘴）
4. SDK自动拍摄视频并上传到百度
5. **后端**调用 `/rest/2.0/face/v1/faceliveness/query` 查询结果

### API端点

#### 1. 获取活体检测会话
```
POST https://aip.baidubce.com/rest/2.0/face/v1/faceliveness?access_token=xxx
参数: appid=你的appid (从API_KEY获取)
返回: {session_id, verifydata}
```

注意：这个API需要 `appid`，不是 `API_KEY`。百度的 appid 可以从 API_KEY 推导，或者需要在百度控制台查看。

**替代方案**：如果上面的API不可用，可以用百度的 **H5活体检测云服务**：
- 不需要 session_id
- 前端直接引入百度的 H5 SDK
- SDK内部处理所有流程
- 后端只需要验证最终结果

#### 2. 查询活体检测结果
```
POST https://aip.baidubce.com/rest/2.0/face/v1/faceliveness/query?access_token=xxx
参数: session_id=xxx
返回: {result: {face_liveness: 0.99, ...}}
```

## 实现方案

由于百度H5活体检测的前端SDK需要从百度CDN加载，且流程较复杂，采用以下方案：

### 前端方案：视频录制 + 后端验证

1. **前端**：使用 `getUserMedia` 录制一段3-5秒的视频
2. **前端**：从视频中抽取多帧图片（base64）
3. **前端**：将多帧图片发送给后端
4. **后端**：调用百度的 `faceverify` API 进行活体检测
5. **后端**：返回检测结果

这种方式不需要百度的前端SDK，只需要后端API。

### 具体实现

#### 文件1: BaiduFaceService.java
添加新方法 `verifyLivenessMultiFrame(List<String> base64Images)`：
- 对多帧图片分别调用 `faceverify` API
- 综合判断结果（多数通过则通过）
- 返回 LivenessResult

#### 文件2: FaceRegisterController.java  
修改 `/face/detect` 接口：
- 接收 `frames` 参数（多帧base64图片数组）
- 调用 `verifyLivenessMultiFrame` 进行活体检测
- 继续调用 searchFace 进行人脸匹配

#### 文件3: checkin.html
修改人脸识别流程：
1. 点击「开启摄像头」→ 打开摄像头
2. 点击「开始录制」→ 录制3秒视频（提示用户缓慢左右转头）
3. 录制完成后，从视频中抽取5帧图片
4. 将5帧图片发送给后端进行活体检测
5. 显示检测结果

### 关键代码片段

**前端视频录制：**
```javascript
async function recordVideo() {
    const mediaRecorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
    const chunks = [];
    mediaRecorder.ondataavailable = e => chunks.push(e.data);
    mediaRecorder.onstop = async () => {
        const blob = new Blob(chunks, { type: 'video/webm' });
        const frames = await extractFrames(blob, 5); // 抽取5帧
        await verifyWithFrames(frames);
    };
    mediaRecorder.start();
    // 录制3秒后自动停止
    setTimeout(() => mediaRecorder.stop(), 3000);
}

async function extractFrames(videoBlob, count) {
    const video = document.createElement('video');
    video.src = URL.createObjectURL(videoBlob);
    await video.play();
    
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    const frames = [];
    
    for (let i = 0; i < count; i++) {
        video.currentTime = (video.duration / count) * i;
        await new Promise(r => video.onseeked = r);
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx.drawImage(video, 0, 0);
        frames.push(canvas.toDataURL('image/png'));
    }
    return frames;
}
```

**提示用户操作：**
录制时显示提示文字：「请缓慢左右转头，然后正对镜头」

## 需要修改的文件
1. `src/main/java/org/example/service/BaiduFaceService.java` - 添加多帧活体检测方法
2. `src/main/java/org/example/controller/FaceRegisterController.java` - 修改detect接口支持多帧
3. `src/main/resources/templates/checkin.html` - 修改前端为视频录制+抽帧流程

## 测试
- 学号: 2024001, 密码: 123456
- 编译: `cd /root/bighomework && mvn clean compile`

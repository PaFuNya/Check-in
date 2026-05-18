# Task: 实现完整百度人脸识别系统

## 目标
将当前简单的人脸检测升级为：**活体检测 + 人脸注册 + 人脸搜索比对**的完整人脸识别系统。

## 百度AI API Key
- API_KEY: `pBlZfNVlhwgMAikW0WmgktM9`
- SECRET_KEY: `bZhL8Y8SuVpvuEpswES2sclkExF48iiT`
- GROUP_ID: `campus_checkin` (人脸库分组)

## 需要修改的文件

### 1. `src/main/java/org/example/service/BaiduFaceService.java`
完全重写，添加以下方法：
- `detectFace(base64Image)` - 保留，检测人脸
- `addFaceToGroup(userId, base64Image)` - 人脸注册到百度人脸库
- `searchFace(base64Image)` - 在人脸库中搜索匹配
- `verifyLiveness(base64Image)` - 活体检测（RGB活体）

百度API端点：
- 人脸注册: `https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add`
- 人脸搜索: `https://aip.baidubce.com/rest/2.0/face/v3/search`
- 活体检测: `https://aip.baidubce.com/rest/2.0/face/v3/faceverify`
- 人脸检测: `https://aip.baidubce.com/rest/2.0/face/v3/detect`

人脸搜索参数：
```
image=BASE64&image_type=BASE64&group_id_list=campus_checkin&max_face_num=1&match_threshold=80
```

活体检测参数（base64图片方式）：
```
image=BASE64&image_type=BASE64&face_field=spoofing
```
返回结果中 `result.face_list[0].spoofing` < 0.3 表示活体通过。

人脸注册参数：
```
image=BASE64&image_type=BASE64&group_id=campus_checkin&user_id=学号&user_info=学生姓名
```

### 2. `src/main/java/org/example/service/impl/DemoFaceRecognitionServiceImpl.java`
修改 `verifyFace()` 方法流程：
1. 先调用 `detectFace()` 检测是否有脸
2. 再调用 `verifyLiveness()` 活体检测（防照片）
3. 最后调用 `searchFace()` 在人脸库中搜索匹配
4. 三步都通过才算验证成功

修改 `registerFace()` 方法流程：
1. 先调用 `detectFace()` 检测是否有脸
2. 调用 `addFaceToGroup()` 注册到百度人脸库
3. 更新数据库 student.face_image_url 为 "registered"

### 3. `src/main/java/org/example/controller/FaceRegisterController.java`
更新 `/face/detect` 接口，添加 `session` 获取 studentId，调用完整的 `verifyFace()` 流程。

新增 `/face/register` 接口（已有，更新）：
- 从 session 获取 studentId（不用请求体传）
- 调用 `faceRecognitionService.registerFace(studentId, imageData)`
- 返回注册结果

### 4. `src/main/resources/templates/checkin.html`
修改签到页面：

a) 顶部导航旁添加「人脸注册」状态提示
   - 如果学生未注册人脸（student.face_image_url 为空或非 "registered"），显示黄色提示条："您尚未注册人脸，请先注册"
   - 添加「注册人脸」按钮

b) 添加人脸注册弹窗/流程：
   - 弹窗包含摄像头预览和拍照按钮
   - 拍照后调用 `/face/register` 注册
   - 注册成功后隐藏提示条

c) 修改 `verifyFaceWithAPI()` 函数：
   - 调用 `/face/detect`（后端现在会做完整的活体+搜索）
   - 显示详细的验证结果（活体检测、人脸匹对）

d) 初始化时检查人脸注册状态：
   - 在 DOMContentLoaded 中调用一个新接口获取学生信息
   - 如果未注册人脸，显示注册提示

### 5. 数据库
不需要改表，student 表的 face_image_url 字段已有，用于标记是否注册。

## 测试账号
- 学号: 2024001, 密码: 123456 (张三)
- 学号: 2024002, 密码: 123456 (李四)

## 注意事项
- 确保 Maven 编译通过: `cd /root/bighomework && mvn clean compile`
- 项目路径 WSL: /root/bighomework
- 百度API调用需要网络访问 aip.baidubce.com
- 活体检测用 RGB 方式（单张图片），不需要视频流
- 人脸搜索的 match_threshold 设为 80（百度推荐值）
- 如果百度API返回错误（如人脸库为空），应该graceful降级，不要阻塞签到

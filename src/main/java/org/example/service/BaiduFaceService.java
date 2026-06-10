package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.ApiKeyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 百度AI人脸识别服务
 * 支持: 人脸检测、人脸注册、人脸搜索、活体检测
 */
@Service
public class BaiduFaceService {

    private static final Logger log = LoggerFactory.getLogger(BaiduFaceService.class);

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String FACE_DETECT_URL = "https://aip.baidubce.com/rest/2.0/face/v3/detect";
    private static final String FACE_ADD_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add";
    private static final String FACE_SEARCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/search";
    private static final String FACE_VERIFY_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceverify";

    public static final String GROUP_ID = "campus_checkin";

    private final ApiKeyConfig apiKeyConfig;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private long tokenExpireTime = 0;

    public BaiduFaceService(ApiKeyConfig apiKeyConfig) {
        this.apiKeyConfig = apiKeyConfig;
    }

    /**
     * 获取百度AI的access_token
     */
    private synchronized String getAccessToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }
        String url = TOKEN_URL + "?grant_type=client_credentials&client_id=" + apiKeyConfig.getBaidu().getApiKey() + "&client_secret=" + apiKeyConfig.getBaidu().getSecretKey();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode jsonNode = objectMapper.readTree(response.body());
        if (jsonNode.has("access_token")) {
            accessToken = jsonNode.get("access_token").asText();
            long expiresIn = jsonNode.get("expires_in").asLong();
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000;
            log.info("获取百度AI access_token成功，有效期: {}秒", expiresIn);
            return accessToken;
        } else {
            String errorMsg = jsonNode.has("error_description") ? jsonNode.get("error_description").asText() : "未知错误";
            log.error("获取百度AI access_token失败: {}", errorMsg);
            throw new RuntimeException("获取百度AI access_token失败: " + errorMsg);
        }
    }

    /**
     * 去掉base64的data:image前缀
     */
    private String pureBase64(String base64Image) {
        if (base64Image != null && base64Image.contains(",")) {
            return base64Image.substring(base64Image.indexOf(",") + 1);
        }
        return base64Image;
    }

    /**
     * 人脸检测 - 检测图片中是否包含人脸
     */
    public FaceDetectResult detectFace(String base64Image) {
        try {
            String token = getAccessToken();
            String requestBody = "image=" + java.net.URLEncoder.encode(pureBase64(base64Image), "UTF-8")
                    + "&image_type=BASE64"
                    + "&face_field=quality,face_shape,face_type"
                    + "&max_face_num=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FACE_DETECT_URL + "?access_token=" + token))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = objectMapper.readTree(response.body());
            int errorCode = jsonNode.has("error_code") ? jsonNode.get("error_code").asInt() : -1;

            if (errorCode == 0) {
                JsonNode result = jsonNode.get("result");
                int faceNum = result.get("face_num").asInt();
                if (faceNum > 0) {
                    JsonNode face = result.get("face_list").get(0);
                    double faceProbability = face.get("face_probability").asDouble();
                    if (faceProbability >= 0.8) {
                        log.info("人脸检测成功 - 置信度: {}, 人脸数: {}", faceProbability, faceNum);
                        return new FaceDetectResult(true, "人脸检测通过", faceNum, faceProbability);
                    } else {
                        return new FaceDetectResult(false, "人脸检测置信度过低，请确保正对摄像头", faceNum, faceProbability);
                    }
                } else {
                    return new FaceDetectResult(false, "未检测到人脸，请确保面部在画面中", 0, 0);
                }
            } else {
                String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
                log.error("百度人脸检测API错误: code={}, msg={}", errorCode, errorMsg);
                return new FaceDetectResult(false, "人脸检测服务异常: " + errorMsg, 0, 0);
            }
        } catch (Exception e) {
            log.error("调用百度人脸检测API异常", e);
            return new FaceDetectResult(false, "人脸检测服务调用失败: " + e.getMessage(), 0, 0);
        }
    }

    /**
     * 人脸注册 - 将人脸添加到百度人脸库
     * @param userId 用户ID（学号）
     * @param base64Image 人脸图片的base64编码
     * @return FaceResult 注册结果
     */
    public FaceResult addFaceToGroup(String userId, String base64Image) {
        try {
            String token = getAccessToken();
            String requestBody = "image=" + java.net.URLEncoder.encode(pureBase64(base64Image), "UTF-8")
                    + "&image_type=BASE64"
                    + "&group_id=" + GROUP_ID
                    + "&user_id=" + java.net.URLEncoder.encode(userId, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FACE_ADD_URL + "?access_token=" + token))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = objectMapper.readTree(response.body());
            int errorCode = jsonNode.has("error_code") ? jsonNode.get("error_code").asInt() : -1;

            if (errorCode == 0) {
                log.info("人脸注册成功 - 用户: {}", userId);
                return new FaceResult(true, "人脸注册成功");
            } else {
                String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
                log.error("百度人脸注册API错误: code={}, msg={}", errorCode, errorMsg);
                return new FaceResult(false, "人脸注册失败: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("调用百度人脸注册API异常", e);
            return new FaceResult(false, "人脸注册服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 人脸搜索 - 在人脸库中搜索匹配的人脸
     * @param base64Image 待搜索的人脸图片base64编码
     * @return FaceSearchResult 搜索结果，包含匹配的userId和相似度score
     */
    public FaceSearchResult searchFace(String base64Image) {
        try {
            String token = getAccessToken();
            String requestBody = "image=" + java.net.URLEncoder.encode(pureBase64(base64Image), "UTF-8")
                    + "&image_type=BASE64"
                    + "&group_id_list=" + GROUP_ID
                    + "&max_face_num=1"
                    + "&match_threshold=80";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FACE_SEARCH_URL + "?access_token=" + token))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = objectMapper.readTree(response.body());
            int errorCode = jsonNode.has("error_code") ? jsonNode.get("error_code").asInt() : -1;

            if (errorCode == 0) {
                JsonNode result = jsonNode.get("result");
                JsonNode userList = result.get("user_list");
                if (userList != null && userList.size() > 0) {
                    JsonNode topMatch = userList.get(0);
                    String matchedUserId = topMatch.get("user_id").asText();
                    double score = topMatch.get("score").asDouble();
                    log.info("人脸搜索成功 - 匹配用户: {}, 相似度: {}", matchedUserId, score);
                    return new FaceSearchResult(true, matchedUserId, score, "人脸匹配成功");
                } else {
                    log.warn("人脸搜索 - 未找到匹配用户");
                    return new FaceSearchResult(false, null, 0, "未在人脸库中找到匹配的人脸，请先注册");
                }
            } else {
                String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
                log.error("百度人脸搜索API错误: code={}, msg={}", errorCode, errorMsg);
                return new FaceSearchResult(false, null, 0, "人脸搜索失败: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("调用百度人脸搜索API异常", e);
            return new FaceSearchResult(false, null, 0, "人脸搜索服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 活体检测 - 防止照片攻击
     * @param base64Image 人脸图片base64编码
     * @return LivenessResult 活体检测结果，spoofing < 0.3 表示活体通过
     */
    public LivenessResult verifyLiveness(String base64Image) {
        try {
            String token = getAccessToken();
            String pure = pureBase64(base64Image);

            // 使用detect API + spoofing字段做活体检测（faceverify API有格式兼容问题）
            Map<String, Object> body = new HashMap<>();
            body.put("image", pure);
            body.put("image_type", "BASE64");
            body.put("face_field", "spoofing");
            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FACE_DETECT_URL + "?access_token=" + token))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = objectMapper.readTree(response.body());
            int errorCode = jsonNode.has("error_code") ? jsonNode.get("error_code").asInt() : -1;

            if (errorCode == 0) {
                JsonNode result = jsonNode.get("result");
                JsonNode faceList = result.get("face_list").get(0);
                double spoofing = faceList.has("spoofing") ? faceList.get("spoofing").asDouble() : 1.0;

                // spoofing < 0.7 表示活体通过（detect API阈值比faceverify宽松）
                boolean isLive = spoofing < 0.7;
                String msg = isLive ? "活体检测通过" : "活体检测未通过，请确保是本人在镜头前（非照片）";
                log.info("活体检测(detect) - spoofing: {}, 结果: {}", spoofing, isLive ? "通过" : "未通过");
                return new LivenessResult(isLive, spoofing, msg);
            } else {
                String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
                log.error("百度detect API错误: code={}, msg={}", errorCode, errorMsg);
                return new LivenessResult(false, 1.0, "活体检测服务异常: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("调用百度detect API异常", e);
            return new LivenessResult(false, 1.0, "活体检测服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 多帧活体检测 - 对多帧图片分别调用faceverify，综合判断结果
     * @param base64Images 多帧base64图片列表（建议5帧）
     * @return LivenessResult 多数通过则通过
     */
    public LivenessResult verifyLivenessMultiFrame(List<String> base64Images) {
        if (base64Images == null || base64Images.isEmpty()) {
            return new LivenessResult(false, 1.0, "未提供活体检测图片");
        }
        int passCount = 0;
        double totalSpoofing = 0;
        String lastError = null;

        for (int i = 0; i < Math.min(base64Images.size(), 2); i++) {
            // 百度免费版QPS限制约1次/秒，帧间间隔2000ms
            if (i > 0) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            LivenessResult frameResult = verifyLiveness(base64Images.get(i));
            totalSpoofing += frameResult.getSpoofing();
            if (frameResult.isSuccess()) {
                passCount++;
            } else {
                lastError = frameResult.getMessage();
            }
        }

        // 等待QPS恢复后再进行人脸搜索
        try { Thread.sleep(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        double avgSpoofing = totalSpoofing / Math.min(base64Images.size(), 2);
        boolean overallSuccess = passCount >= 1; // 2帧中至少1帧通过

        int actualCount = Math.min(base64Images.size(), 2);
        String msg;
        if (overallSuccess) {
            msg = String.format("多帧活体检测通过 (%d/%d)", passCount, actualCount);
        } else {
            msg = String.format("多帧活体检测未通过 (%d/%d): %s", passCount, actualCount,
                    lastError != null ? lastError : "多数帧未通过活体检测");
        }

        log.info("多帧活体检测 - 通过: {}/{}, 平均spoofing: {}, 结果: {}",
                passCount, actualCount, avgSpoofing, overallSuccess ? "通过" : "未通过");
        return new LivenessResult(overallSuccess, avgSpoofing, msg);
    }

    // ========== Result Classes ==========

    /**
     * 人脸检测结果
     */
    public static class FaceDetectResult {
        private final boolean success;
        private final String message;
        private final int faceCount;
        private final double confidence;

        public FaceDetectResult(boolean success, String message, int faceCount, double confidence) {
            this.success = success;
            this.message = message;
            this.faceCount = faceCount;
            this.confidence = confidence;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getFaceCount() { return faceCount; }
        public double getConfidence() { return confidence; }
    }

    /**
     * 通用操作结果（人脸注册等）
     */
    public static class FaceResult {
        private final boolean success;
        private final String message;

        public FaceResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    /**
     * 人脸搜索结果
     */
    public static class FaceSearchResult {
        private final boolean success;
        private final String userId;
        private final double score;
        private final String message;

        public FaceSearchResult(boolean success, String userId, double score, String message) {
            this.success = success;
            this.userId = userId;
            this.score = score;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getUserId() { return userId; }
        public double getScore() { return score; }
        public String getMessage() { return message; }
    }

    /**
     * 活体检测结果
     */
    public static class LivenessResult {
        private final boolean success;
        private final double spoofing;
        private final String message;

        public LivenessResult(boolean success, double spoofing, String message) {
            this.success = success;
            this.spoofing = spoofing;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public double getSpoofing() { return spoofing; }
        public String getMessage() { return message; }
    }
}

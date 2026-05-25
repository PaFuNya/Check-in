package org.example.service;

/**
 * 人脸验证完整结果，包含检测、活体、搜索三步的详细信息
 */
public class FaceVerificationResult {

    private final boolean success;
    private final String message;
    private final boolean faceDetected;
    private final double faceConfidence;
    private final boolean livenessPassed;
    private final double spoofingScore;
    private final boolean searchPassed;
    private final String matchedUserId;
    private final double matchScore;

    public FaceVerificationResult(boolean success, String message,
                                  boolean faceDetected, double faceConfidence,
                                  boolean livenessPassed, double spoofingScore,
                                  boolean searchPassed, String matchedUserId, double matchScore) {
        this.success = success;
        this.message = message;
        this.faceDetected = faceDetected;
        this.faceConfidence = faceConfidence;
        this.livenessPassed = livenessPassed;
        this.spoofingScore = spoofingScore;
        this.searchPassed = searchPassed;
        this.matchedUserId = matchedUserId;
        this.matchScore = matchScore;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public boolean isFaceDetected() { return faceDetected; }
    public double getFaceConfidence() { return faceConfidence; }
    public boolean isLivenessPassed() { return livenessPassed; }
    public double getSpoofingScore() { return spoofingScore; }
    public boolean isSearchPassed() { return searchPassed; }
    public String getMatchedUserId() { return matchedUserId; }
    public double getMatchScore() { return matchScore; }
}

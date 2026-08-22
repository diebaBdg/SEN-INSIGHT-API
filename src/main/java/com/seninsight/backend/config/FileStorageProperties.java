package com.seninsight.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    private String uploadDir;
    private long maxFileSize = 5120 * 1024; // 5MB par défaut
    private String[] allowedTypes = {"pdf", "doc", "docx", "jpg", "jpeg", "png"};

    // Getters et Setters
    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }

    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }

    public String[] getAllowedTypes() { return allowedTypes; }
    public void setAllowedTypes(String[] allowedTypes) { this.allowedTypes = allowedTypes; }
}
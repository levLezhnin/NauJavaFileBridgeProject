package ru.LevLezhnin.NauJava.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String url;
    private String accessKey;
    private String secretKey;
    private Map<String, BucketProperties> buckets;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Map<String, BucketProperties> getBuckets() {
        return Collections.unmodifiableMap(buckets);
    }

    public void setBuckets(Map<String, BucketProperties> buckets) {
        this.buckets = buckets;
    }

    public static class BucketProperties {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public enum BucketKeys {
        FILE_BUCKET("fileBucket"),
        REPORT_BUCKET("reportBucket");

        private final String key;

        public String getKey() {
            return key;
        }

        BucketKeys(String key) {
            this.key = key;
        }
    }
}

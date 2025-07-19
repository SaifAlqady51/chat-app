package com.example.common.constants;
public class KafkaTopics {

    public static final String USER_EXISTENCE_CHECK_BY_ID = "user-existence-check-by-id";
    public static final String USER_EXISTENCE_REQUEST = "user-existence-request";
    public static final String USER_EXISTENCE_RESPONSE = "user-existence-response";
    private KafkaTopics() {
        // Private constructor to prevent instantiation
    }
}
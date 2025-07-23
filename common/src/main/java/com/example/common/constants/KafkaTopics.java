package com.example.common.constants;
public final class KafkaTopics {
    public static final String USER_EXISTENCE_REQUEST = "user-existence-request";
    public static final String USER_EXISTENCE_RESPONSE = "user-existence-response";

    private KafkaTopics() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
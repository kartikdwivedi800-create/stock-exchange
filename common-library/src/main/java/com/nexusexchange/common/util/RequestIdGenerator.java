package com.nexusexchange.common.util;

import java.util.UUID;

public final class RequestIdGenerator {
    private RequestIdGenerator() {}

    public static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

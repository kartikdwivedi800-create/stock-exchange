package com.nexusexchange.common.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis());

    private IdGenerator() {}

    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    public static Long generateLongId() {
        return COUNTER.incrementAndGet();
    }
}

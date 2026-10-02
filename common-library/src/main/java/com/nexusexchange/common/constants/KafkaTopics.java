package com.nexusexchange.common.constants;

/**
 * Kafka topic names. Must match infra/kafka/create-topics.sh (topics are not auto-created).
 * Keys and producers/consumers per topic: docs/01-architecture.md §6.
 */
public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String USER_EVENTS = "user-events";
    public static final String WALLET_EVENTS = "wallet-events";
    public static final String ORDER_COMMANDS = "order-commands";
    public static final String ORDER_EVENTS = "order-events";
    public static final String TRADE_EVENTS = "trade-events";
    public static final String PRICE_EVENTS = "price-events";
    public static final String SETTLEMENT_EVENTS = "settlement-events";
    public static final String NOTIFICATION_EVENTS = "notification-events";

    /** Suffix of the dead-letter topic for each topic, e.g. "trade-events.DLT". */
    public static final String DLT_SUFFIX = ".DLT";
}

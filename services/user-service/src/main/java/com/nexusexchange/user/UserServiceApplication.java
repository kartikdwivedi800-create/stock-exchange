package com.nexusexchange.user;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        // Run in UTC regardless of the host machine: timestamps are stored and exchanged in UTC,
        // and some JDBC/Postgres combos reject legacy zone names such as "Asia/Calcutta".
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

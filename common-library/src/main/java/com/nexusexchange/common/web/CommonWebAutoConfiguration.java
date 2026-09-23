package com.nexusexchange.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * Registers the shared web beans in every servlet-based service that has common-library on its classpath.
 * Listed in META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * and in the @WebMvcTest slice imports so controller tests get the same error handling.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(GlobalExceptionHandler.class)
public class CommonWebAutoConfiguration {
}

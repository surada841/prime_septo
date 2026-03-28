package com.revshop.config;

import com.revshop.entity.NotificationType;
import com.revshop.entity.OrderStatus;
import com.revshop.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaConstraintSyncConfig {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void syncEnumCheckConstraints() {
        syncSafely("orders", "status", "chk_orders_status", enumValues(OrderStatus.values()));
        syncSafely("notifications", "notification_type", "chk_notifications_notification_type", enumValues(NotificationType.values()));
        syncSafely("payments", "status", "chk_payments_status", enumValues(PaymentStatus.values()));
    }

    private void syncSafely(
            String tableName,
            String columnName,
            String targetConstraintName,
            List<String> allowedValues
    ) {
        try {
            syncEnumConstraint(tableName, columnName, targetConstraintName, allowedValues);
        } catch (Exception e) {
            log.error("Failed to sync enum check constraint for {}.{}: {}", tableName, columnName, e.getMessage(), e);
        }
    }

    private void syncEnumConstraint(
            String tableName,
            String columnName,
            String targetConstraintName,
            List<String> allowedValues
    ) {
        if (!tableExists(tableName)) {
            log.warn("Table '{}' does not exist. Skipping constraint sync.", tableName);
            return;
        }

        List<String> existingConstraints = jdbcTemplate.queryForList(
                """
                SELECT tc.CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS tc
                JOIN information_schema.CHECK_CONSTRAINTS cc
                  ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
                 AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = DATABASE()
                  AND tc.TABLE_NAME = ?
                  AND tc.CONSTRAINT_TYPE = 'CHECK'
                  AND LOWER(cc.CHECK_CLAUSE) LIKE ?
                """,
                String.class,
                tableName,
                "%" + columnName.toLowerCase(Locale.ROOT) + "%in%"
        );

        for (String constraintName : existingConstraints) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CHECK " + constraintName);
            log.info("Dropped existing check constraint '{}' on table '{}'", constraintName, tableName);
        }

        String enumInClause = allowedValues.stream()
                .map(value -> "'" + value.replace("'", "''") + "'")
                .collect(Collectors.joining(","));

        jdbcTemplate.execute(
                "ALTER TABLE " + tableName
                        + " ADD CONSTRAINT " + targetConstraintName
                        + " CHECK (" + columnName + " IN (" + enumInClause + "))"
        );

        log.info("Synced enum check constraint for {}.{} with {} values", tableName, columnName, allowedValues.size());
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private List<String> enumValues(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(Enum::name)
                .toList();
    }
}
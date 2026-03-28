package com.revshop.config;

import com.revshop.entity.NotificationType;
import com.revshop.entity.OrderStatus;
import com.revshop.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.event.EventListener;
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
        syncEnumConstraint(
                "orders",
                "status",
                "chk_orders_status",
                enumValues(OrderStatus.values())
        );
        syncEnumConstraint(
                "notifications",
                "notification_type",
                "chk_notifications_notification_type",
                enumValues(NotificationType.values())
        );
        syncEnumConstraint(
                "payments",
                "status",
                "chk_payments_status",
                enumValues(PaymentStatus.values())
        );
    }

    private void syncEnumConstraint(
            String tableName,
            String columnName,
            String targetConstraintName,
            List<String> allowedValues
    ) {
        String tableUpper = tableName.toUpperCase(Locale.ROOT);
        String matchPattern = "%" + columnName.toLowerCase(Locale.ROOT) + " in (%";

        List<String> existingConstraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM user_constraints
                WHERE table_name = ?
                  AND constraint_type = 'C'
                  AND LOWER(search_condition_vc) LIKE ?
                """,
                String.class,
                tableUpper,
                matchPattern
        );

        for (String constraintName : existingConstraints) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
        }

        String enumInClause = allowedValues.stream()
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(","));

        jdbcTemplate.execute(
                "ALTER TABLE " + tableName
                        + " ADD CONSTRAINT " + targetConstraintName
                        + " CHECK (" + columnName + " IN (" + enumInClause + "))"
        );

        log.info("Synced enum check constraint for {}.{} with {} values", tableName, columnName, allowedValues.size());
    }

    private List<String> enumValues(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(Enum::name)
                .toList();
    }
}

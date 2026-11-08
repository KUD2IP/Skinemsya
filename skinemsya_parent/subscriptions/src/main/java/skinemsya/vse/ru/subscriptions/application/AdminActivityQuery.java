package skinemsya.vse.ru.subscriptions.application;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

@Component
public class AdminActivityQuery {

    private final JdbcTemplate jdbcTemplate;

    public AdminActivityQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<Long, AdminActivity> forUsers(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT u.id, "
                + "(SELECT count(*) FROM events e WHERE e.created_by = u.id AND e.deleted_at IS NULL), "
                + "(SELECT count(*) FROM receipts r JOIN events e ON e.id = r.event_id "
                + "WHERE e.created_by = u.id AND e.deleted_at IS NULL AND r.status = 'PROCESSED') "
                + "FROM users u WHERE u.id IN ("
                + placeholders
                + ")";
        Map<Long, AdminActivity> result = new HashMap<>();
        jdbcTemplate.query(
                sql,
                (RowCallbackHandler) (ResultSet rs) -> {
                    result.put(rs.getLong(1), new AdminActivity((int) rs.getLong(2), (int) rs.getLong(3)));
                },
                userIds.toArray());
        return result;
    }

    public record AdminActivity(int eventsCreated, int receiptCount) {}
}

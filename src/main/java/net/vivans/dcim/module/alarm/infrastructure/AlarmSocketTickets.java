package net.vivans.dcim.module.alarm.infrastructure;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlarmSocketTickets {
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String issue(Authentication auth) {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        String value = UUID.randomUUID().toString();
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        tickets.put(value, new Ticket(auth.getName(), admin, now.plusSeconds(30)));
        return value;
    }

    public Ticket consume(String value) {
        if (value == null) return null;
        Ticket ticket = tickets.remove(value);
        return ticket != null && ticket.expiresAt().isAfter(Instant.now()) ? ticket : null;
    }

    public record Ticket(String username, boolean admin, Instant expiresAt) {}
}

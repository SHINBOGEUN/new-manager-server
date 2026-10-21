package net.vivans.dcim.module.alarm.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AlarmSocketTicketsTest {
    @Test
    void ticketCanBeConsumedOnlyOnceAndKeepsRole() {
        var tickets = new AlarmSocketTickets();
        var auth = new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String ticket = tickets.issue(auth);
        assertThat(tickets.consume(ticket).admin()).isFalse();
        assertThat(tickets.consume(ticket)).isNull();
    }
}

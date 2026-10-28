package jobsonnar.monitor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:monitor-context;DB_CLOSE_DELAY=-1", "monitor.enabled=false"})
class MonitorContextTests {
    @Autowired MonitorRepository repository;
    @Autowired MonitorService service;

    @Test
    void applicationStartsWithLocalDatabaseAndBoundMonitorConfiguration() {
        assertFalse(service.status().enabled());
        assertNotNull(repository.addSearch("Estágio TI", "Brasília"));
    }

    @Test
    void remoteAccessNeedsConfiguredTokenAndLocalAccessWorksWithoutOne() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        var local = new MonitorController(repository, service, "");
        assertThrows(ResponseStatusException.class, () -> local.status(request));
        request.setRemoteAddr("127.0.0.1");
        assertNotNull(local.status(request));

        var secured = new MonitorController(repository, service, "test-token");
        assertThrows(ResponseStatusException.class, () -> secured.status(request));
        request.addHeader("X-Monitor-Token", "test-token");
        assertNotNull(secured.status(request));
    }
}

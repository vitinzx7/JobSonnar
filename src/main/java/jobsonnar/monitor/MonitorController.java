package jobsonnar.monitor;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/monitor")
@CrossOrigin(origins = "http://localhost:5173")
public class MonitorController {
    private final MonitorRepository repository;
    private final MonitorService service;
    private final String token;
    public MonitorController(MonitorRepository repository, MonitorService service,
            @Value("${monitor.api-token:}") String token) {
        this.repository = repository;
        this.service = service;
        this.token = token;
    }

    public record SearchInput(String query, String location) { }
    public record FlagInput(boolean value) { }

    // The monitor is a personal inbox, not a public data endpoint.
    private void authorize(HttpServletRequest request) {
        if (!token.isBlank()) {
            String supplied = request.getHeader("X-Monitor-Token");
            if (supplied != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                    supplied.getBytes(StandardCharsets.UTF_8))) { return; }
        } else {
            try {
                if (InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) { return; }
            } catch (java.net.UnknownHostException ignored) { }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Informe o token do monitor");
    }

    @GetMapping("/status")
    public MonitorService.Status status(HttpServletRequest request) { authorize(request); return service.status(); }
    @GetMapping("/searches")
    public List<MonitorRepository.SavedSearch> searches(HttpServletRequest request) {
        authorize(request); return repository.searches();
    }
    @PostMapping("/searches")
    public MonitorRepository.SavedSearch add(@RequestBody SearchInput input, HttpServletRequest request) {
        authorize(request);
        try { return repository.addSearch(input.query(), input.location()); }
        catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
    @PatchMapping("/searches/{id}")
    public void enable(@PathVariable String id, @RequestBody FlagInput input, HttpServletRequest request) {
        authorize(request); found(repository.setEnabled(id, input.value()));
    }
    @DeleteMapping("/searches/{id}")
    public void delete(@PathVariable String id, HttpServletRequest request) {
        authorize(request); found(repository.deleteSearch(id));
    }
    @GetMapping("/jobs")
    public List<MonitorRepository.SavedJob> jobs(@RequestParam(defaultValue = "false") boolean unseenOnly,
            @RequestParam(defaultValue = "") String source, @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset, HttpServletRequest request) {
        authorize(request); return repository.jobs(unseenOnly, source, limit, offset);
    }
    @PatchMapping("/jobs/{id}/seen")
    public void seen(@PathVariable String id, @RequestBody FlagInput input, HttpServletRequest request) {
        authorize(request); found(repository.setSeen(id, input.value()));
    }
    private void found(int affected) {
        if (affected == 0) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
    }
}

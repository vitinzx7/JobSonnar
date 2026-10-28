package jobsonnar.monitor;

import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.mail.Folder;
import jakarta.mail.Session;
import jobsonnar.dto.JobResponseDto;

@Component
public class AlertMailboxCollector {
    private final AlertEmailParser parser;
    private final boolean enabled;
    private final String host, username, password, folderName;

    public AlertMailboxCollector(AlertEmailParser parser,
            @Value("${monitor.email.enabled:false}") boolean enabled,
            @Value("${monitor.email.host:}") String host,
            @Value("${monitor.email.username:}") String username,
            @Value("${monitor.email.password:}") String password,
            @Value("${monitor.email.folder:JobSonnar}") String folderName) {
        this.parser = parser;
        this.enabled = enabled;
        this.host = host;
        this.username = username;
        this.password = password;
        this.folderName = folderName;
    }

    public boolean enabled() { return enabled; }

    public List<JobResponseDto> collect() throws Exception {
        if (!enabled) { return List.of(); }
        if (host.isBlank() || username.isBlank() || password.isBlank() || folderName.isBlank()) {
            throw new IllegalStateException("Configure host, usuário, senha de aplicativo e pasta dos alertas");
        }
        Properties properties = new Properties();
        properties.setProperty("mail.imaps.connectiontimeout", "10000");
        properties.setProperty("mail.imaps.timeout", "15000");
        properties.setProperty("mail.imaps.writetimeout", "15000");
        properties.setProperty("mail.imaps.ssl.checkserveridentity", "true");
        properties.setProperty("mail.imaps.peek", "true");
        var result = new ArrayList<JobResponseDto>();
        try (var store = Session.getInstance(properties).getStore("imaps")) {
            store.connect(host, username, password);
            Folder folder = store.getFolder(folderName);
            if (!folder.exists()) { throw new IllegalStateException("A pasta de alertas configurada não existe"); }
            try {
                folder.open(Folder.READ_ONLY);
                int total = folder.getMessageCount();
                // Read the dedicated alerts folder, including already-read emails. URL dedup handles repeats.
                // Bound the batch; large folders should be archived outside this collector.
                if (total > 1000) {
                    throw new IllegalStateException("Pasta com mais de 1000 alertas; arquive os já importados");
                }
                for (int i = 1; i <= total; i++) {
                    var message = folder.getMessage(i);
                    if (message.getSize() > 2_000_000) { continue; }
                    result.addAll(parser.parse(message));
                }
            } finally {
                if (folder.isOpen()) { folder.close(false); }
            }
        }
        return result;
    }
}

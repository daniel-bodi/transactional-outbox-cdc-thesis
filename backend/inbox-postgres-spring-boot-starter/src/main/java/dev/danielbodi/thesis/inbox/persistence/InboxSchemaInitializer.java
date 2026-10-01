package dev.danielbodi.thesis.inbox.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author danielbodi
 */
@Slf4j
public class InboxSchemaInitializer extends DataSourceScriptDatabaseInitializer {

    private static final String SCHEMA_LOCATION = "classpath:database/migration/inbox-schema.sql";

    public InboxSchemaInitializer(DataSource dataSource) {
        super(dataSource, schemaSettings());
    }

    @Override
    public boolean initializeDatabase() {
        final boolean applied = super.initializeDatabase();

        if (applied) {
            log.info("Inbox schema initializer executed: [{}] processed_events table created if missing", SCHEMA_LOCATION);
        } else {
            log.info("Inbox schema initializer skipped");
        }

        return applied;
    }

    private static DatabaseInitializationSettings schemaSettings() {
        final DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
        settings.setSchemaLocations(List.of(SCHEMA_LOCATION));
        settings.setMode(DatabaseInitializationMode.ALWAYS);
        return settings;
    }
}

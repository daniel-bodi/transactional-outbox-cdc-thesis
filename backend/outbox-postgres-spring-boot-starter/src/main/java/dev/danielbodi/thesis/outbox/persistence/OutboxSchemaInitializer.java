package dev.danielbodi.thesis.outbox.persistence;

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
public class OutboxSchemaInitializer extends DataSourceScriptDatabaseInitializer {

    private static final String SCHEMA_LOCATION = "classpath:database/migration/outbox-schema.sql";

    public OutboxSchemaInitializer(DataSource dataSource) {
        super(dataSource, schemaSettings());
    }

    @Override
    public boolean initializeDatabase() {
        final boolean applied = super.initializeDatabase();

        if (applied) {
            log.info("Outbox schema initializer executed: [{}] outbox table created if missing", SCHEMA_LOCATION);
        } else {
            log.info("Outbox schema initializer skipped");
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

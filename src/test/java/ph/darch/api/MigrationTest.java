package ph.darch.api;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the Flyway {@code V1__init.sql} migration applies cleanly to a real
 * Postgres database. Uses Testcontainers so no manually provisioned DB is required
 * to run the suite.
 */
@Testcontainers
class MigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void v1MigrationAppliesCleanlyAndCreatesSchema() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(1);
        assertThat(result.migrations.get(0).version).isEqualTo("1");

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            assertTableAndColumn(conn, "admin", "username");
            assertTableAndColumn(conn, "products", "slug");
            assertTableAndColumn(conn, "products", "price");
            assertTableAndColumn(conn, "media_assets", "object_path");
            assertTableAndColumn(conn, "product_media", "position");

            // product_media FK cascade + partial unique index for one-video rule
            assertUniqueIndex(conn, "product_media_one_video");
        }
    }

    private void assertTableAndColumn(Connection conn, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            assertThat(rs.next())
                    .as("table %s should have column %s", table, column)
                    .isTrue();
        }
    }

    private void assertUniqueIndex(Connection conn, String indexName) throws Exception {
        try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, "product_media", true, true)) {
            boolean found = false;
            while (rs.next()) {
                if (indexName.equals(rs.getString("INDEX_NAME"))) {
                    found = true;
                    break;
                }
            }
            assertThat(found).as("unique index %s should exist", indexName).isTrue();
        }
    }
}

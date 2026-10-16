package com.moviehub.movie.repository

import liquibase.command.CommandScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.DriverManager

/**
 * E6-S2, second acceptance criterion: when the catalogue already has data, the seed adds nothing.
 * Runs Liquibase directly (no Spring): schema first, one movie by hand, then the seed context.
 */
class SeedPreconditionTest {

    // DB_CLOSE_DELAY keeps the in-memory database alive between connections for the length of the test
    private val url = "jdbc:h2:mem:seed-precondition;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"

    private fun update(contexts: String) {
        CommandScope("update")
            .addArgumentValue("url", url)
            .addArgumentValue("username", "sa")
            .addArgumentValue("password", "")
            .addArgumentValue("changelogFile", "db/changelog/db.changelog-master.xml")
            .addArgumentValue("contexts", contexts)
            .execute()
    }

    private fun query(sql: String): String = DriverManager.getConnection(url, "sa", "").use { conn ->
        conn.createStatement().executeQuery(sql).use { rs -> rs.next(); rs.getString(1) }
    }

    @Test
    fun `should add nothing when the movie table already has a row`() {
        update("schema")
        DriverManager.getConnection(url, "sa", "").use { conn ->
            conn.createStatement().execute("INSERT INTO movie (title, release_year, created_at, updated_at) VALUES ('Mine', 2000, NOW(), NOW())")
        }

        update("schema,seed")

        assertEquals("1", query("SELECT COUNT(*) FROM movie"))
        assertEquals("MARK_RAN", query("SELECT exectype FROM databasechangelog WHERE id = '0099-seed-movies-and-credits'"))
    }
}

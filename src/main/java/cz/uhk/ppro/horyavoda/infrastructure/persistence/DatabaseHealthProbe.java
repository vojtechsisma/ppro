package cz.uhk.ppro.horyavoda.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Ověření dostupnosti relační databáze jednoduchým dotazem. */
@Component
public class DatabaseHealthProbe {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthProbe(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Provede {@code SELECT 1}; při nedostupnosti databáze vyhodí výjimku. */
    public void ping() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    }
}

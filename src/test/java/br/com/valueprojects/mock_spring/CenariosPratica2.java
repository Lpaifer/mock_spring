package br.com.valueprojects.mock_spring;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Participante;

public final class CenariosPratica2 {
    public static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-28T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    public static final Participante ANA = new Participante(1, "Ana", "11900000001");
    public static final Participante BRUNO = new Participante(2, "Bruno", "11900000002");

    private CenariosPratica2() { }

    public static Jogo jogo(String nome, String data) {
        return new CriadorDeJogo().para(nome)
                .naData(GregorianCalendar.from(LocalDate.parse(data).atStartOfDay(RELOGIO.getZone())))
                .resultado(ANA, 100).resultado(BRUNO, 200).constroi();
    }

    public static DataSource banco() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(ds);
        return ds;
    }
}

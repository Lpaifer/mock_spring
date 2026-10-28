package br.com.valueprojects.mock_spring.test;

import static br.com.valueprojects.mock_spring.CenariosPratica2.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.valueprojects.mock_spring.model.EnviadorSms;
import br.com.valueprojects.mock_spring.model.FinalizaJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import infra.JogoDao;

class JogoDaoTest {
    private JogoDao dao;
    private JdbcTemplate sql;

    @BeforeEach
    void preparaBanco() {
        DataSource dataSource = banco();
        dao = new JogoDao(dataSource);
        sql = new JdbcTemplate(dataSource);
    }

    @Test
    void deveConfirmarTodosOsVencedoresNoBancoAntesDoSms() {
        dao.salva(jogo("Corrida", "2026-10-20"));
        dao.salva(jogo("Moedas", "2026-10-21"));
        EnviadorSms sms = mock(EnviadorSms.class);

        doAnswer(invocacao -> {
            // Outra conexão só consegue ler os dois vencedores após o commit.
            assertEquals(2, sql.queryForObject(
                    "SELECT COUNT(*) FROM JOGO WHERE FINALIZADO = TRUE AND VENCEDOR_ID = 2",
                    Integer.class));
            return null;
        }).when(sms).enviar(any(), any());

        new FinalizaJogo(dao, sms, RELOGIO).finaliza();

        verify(sms, times(2)).enviar(eq(BRUNO), any(Jogo.class));
        assertEquals(2, dao.finalizados().size());
        assertTrue(dao.emAndamento().isEmpty());
    }

    @Test
    void deveDesfazerLoteENaoEnviarSmsSeSegundoJogoFalhar() {
        Jogo primeiro = jogo("Corrida", "2026-10-20");
        Jogo segundo = jogo("Moedas", "2026-10-21");
        dao.salva(primeiro);
        dao.salva(segundo);
        EnviadorSms sms = mock(EnviadorSms.class);

        // Forçar erro real no segundo update, após o primeiro já ter ocorrido.
        sql.execute("ALTER TABLE JOGO ADD CONSTRAINT FALHA_SEGUNDO CHECK "
                + "(ID <> " + segundo.getId() + " OR FINALIZADO = FALSE)");

        assertThrows(IllegalStateException.class,
                () -> new FinalizaJogo(dao, sms, RELOGIO).finaliza());

        verifyNoInteractions(sms);
        assertTrue(dao.finalizados().isEmpty());
        assertEquals(2, dao.emAndamento().size());
        assertEquals(0, sql.queryForObject(
                "SELECT COUNT(*) FROM JOGO WHERE VENCEDOR_ID IS NOT NULL", Integer.class));
    }
}

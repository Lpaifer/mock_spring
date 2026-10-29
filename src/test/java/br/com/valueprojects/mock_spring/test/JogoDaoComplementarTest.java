package br.com.valueprojects.mock_spring.test;

import static br.com.valueprojects.mock_spring.CenariosPratica2.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import br.com.valueprojects.mock_spring.model.*;
import infra.JogoDao;

class JogoDaoComplementarTest {
    private DataSource dataSource;
    private JogoDao dao;
    private JdbcTemplate sql;

    @BeforeEach
    void preparaBancoIsolado() {
        dataSource = banco();
        dao = new JogoDao(dataSource);
        sql = new JdbcTemplate(dataSource);
    }

    @Test
    void persisteParticipantesResultadosEIdentificaVencedorAoRecarregar() {
        Jogo primeiro = jogo("Corrida", "2026-10-20");
        Jogo segundo = jogo("Moedas", "2026-10-21");
        dao.salva(primeiro);
        dao.salva(segundo);
        assertTrue(primeiro.getId() > 0);
        assertEquals(2, dao.emAndamento().size());
        assertTrue(dao.finalizados().isEmpty());
        assertEquals(2, sql.queryForObject("SELECT COUNT(*) FROM PARTICIPANTE", Integer.class));

        dao.finalizaTodos(List.of(new FinalizacaoJogo(primeiro, BRUNO)));

        Jogo salvo = dao.finalizados().get(0);
        assertEquals(primeiro.getId(), salvo.getId());
        assertEquals("Corrida", salvo.getDescricao());
        assertTrue(salvo.isFinalizado());
        assertEquals(BRUNO, salvo.getVencedor());
        assertEquals("11900000002", salvo.getVencedor().getTelefone());
        assertEquals(2, salvo.getResultados().size());
        assertEquals(200, salvo.getResultados().get(1).getMetrica());
        assertEquals(1, dao.emAndamento().size());
    }

    @Test
    void falhaNoCommitFazRollbackENaoEnviaSms() throws Exception {
        dao.salva(jogo("Corrida", "2026-10-20"));
        List<Jogo> jogos = dao.emAndamento();
        Connection conexao = spy(dataSource.getConnection());
        doThrow(new SQLException("Commit indisponível")).when(conexao).commit();
        DataSource dataSourceFalho = mock(DataSource.class);
        when(dataSourceFalho.getConnection()).thenReturn(conexao);
        JogoDao daoFalho = spy(new JogoDao(dataSourceFalho));
        doReturn(jogos).when(daoFalho).emAndamento();
        EnviadorSms sms = mock(EnviadorSms.class);

        assertThrows(IllegalStateException.class, () -> new FinalizaJogo(daoFalho, sms, RELOGIO).finaliza());

        verify(conexao).rollback();
        verifyNoInteractions(sms);
        assertTrue(dao.finalizados().isEmpty());
        assertFalse(jogos.get(0).isFinalizado());
    }

    @Test
    void rejeitaVencedorQueNaoParticipouEFinalizacaoDuplicada() {
        Jogo jogo = jogo("Corrida", "2026-10-20");
        dao.salva(jogo);
        Participante estranho = new Participante(99, "Não participou");
        assertThrows(IllegalStateException.class,
                () -> dao.finalizaTodos(List.of(new FinalizacaoJogo(jogo, estranho))));
        assertTrue(dao.finalizados().isEmpty());
        dao.finalizaTodos(List.of(new FinalizacaoJogo(jogo, BRUNO)));
        assertThrows(IllegalStateException.class,
                () -> dao.finalizaTodos(List.of(new FinalizacaoJogo(jogo, BRUNO))));
        assertEquals(1, dao.finalizados().size());
    }

    @Test
    void salvaEConsultaJogoSemResultados() {
        dao.salva(new Jogo("Ainda sem resultados"));
        assertTrue(dao.emAndamento().get(0).getResultados().isEmpty());
    }

    @Test
    void falhaAoSalvarResultadoDesfazCadastroDoJogo() {
        sql.execute("DROP TABLE RESULTADOS");
        Jogo jogo = jogo("Corrida", "2026-10-20");
        assertThrows(IllegalStateException.class, () -> dao.salva(jogo));
        assertEquals(0, sql.queryForObject("SELECT COUNT(*) FROM JOGO", Integer.class));
        assertEquals(0, jogo.getId());
    }

    @Test
    void falhaDeConexaoEhPropagadaComCausa() throws Exception {
        DataSource indisponivel = mock(DataSource.class);
        SQLException causa = new SQLException("Banco indisponível");
        when(indisponivel.getConnection()).thenThrow(causa);
        var erro = assertThrows(IllegalStateException.class, () -> new JogoDao(indisponivel).emAndamento());
        assertSame(causa, erro.getCause());
    }

    @Test
    void ausenciaDeChaveGeradaFazRollback() throws Exception {
        DataSource fonte = mock(DataSource.class);
        Connection conexao = mock(Connection.class);
        PreparedStatement comando = mock(PreparedStatement.class);
        ResultSet chaves = mock(ResultSet.class);
        when(fonte.getConnection()).thenReturn(conexao);
        when(conexao.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(comando);
        when(comando.getGeneratedKeys()).thenReturn(chaves);
        when(chaves.next()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> new JogoDao(fonte).salva(new Jogo("Corrida")));

        verify(conexao).rollback();
        verify(conexao, never()).commit();
    }
}

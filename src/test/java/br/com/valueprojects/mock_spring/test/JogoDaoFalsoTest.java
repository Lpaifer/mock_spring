package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.*;
import static br.com.valueprojects.mock_spring.CenariosPratica2.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import br.com.valueprojects.mock_spring.model.FinalizacaoJogo;
import infra.JogoDaoFalso;

class JogoDaoFalsoTest {
    @Test
    void fakeTemDadosPorInstanciaEPermiteFinalizacao() {
        var dao = new JogoDaoFalso();
        var jogo = jogo("Corrida", "2026-10-20");
        dao.salva(jogo);
        assertEquals(List.of(jogo), dao.emAndamento());
        assertTrue(new JogoDaoFalso().emAndamento().isEmpty());
        dao.finalizaTodos(List.of(new FinalizacaoJogo(jogo, BRUNO)));
        assertTrue(dao.emAndamento().isEmpty());
        assertEquals(List.of(jogo), dao.finalizados());
        assertEquals(BRUNO, jogo.getVencedor());
    }

    @Test
    void fakeNaoFinalizaParcialmenteQuandoSegundoJogoNaoExiste() {
        var dao = new JogoDaoFalso();
        var primeiro = jogo("Primeiro", "2026-10-20");
        var ausente = jogo("Ausente", "2026-10-20");
        dao.salva(primeiro);
        assertThrows(IllegalStateException.class, () -> dao.finalizaTodos(List.of(
                new FinalizacaoJogo(primeiro, BRUNO), new FinalizacaoJogo(ausente, BRUNO))));
        assertFalse(primeiro.isFinalizado());
        assertTrue(dao.finalizados().isEmpty());
    }
}

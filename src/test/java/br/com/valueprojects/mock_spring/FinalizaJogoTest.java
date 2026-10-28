package br.com.valueprojects.mock_spring;

import static br.com.valueprojects.mock_spring.CenariosPratica2.BRUNO;
import static br.com.valueprojects.mock_spring.CenariosPratica2.RELOGIO;
import static br.com.valueprojects.mock_spring.CenariosPratica2.jogo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import br.com.valueprojects.mock_spring.model.EnviadorSms;
import br.com.valueprojects.mock_spring.model.FinalizacaoJogo;
import br.com.valueprojects.mock_spring.model.FinalizaJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import infra.RepositorioJogos;

class FinalizaJogoTest {
    private final RepositorioJogos dao = mock(RepositorioJogos.class);
    private final EnviadorSms sms = mock(EnviadorSms.class);
    private final FinalizaJogo finalizador = new FinalizaJogo(dao, sms, RELOGIO);

    @Test
    void deveSalvarTodosOsJogosAntesDeEnviarSms() {
        Jogo primeiro = jogo("Corrida", "2026-10-20");
        Jogo segundo = jogo("Moedas", "2026-10-21");
        when(dao.emAndamento()).thenReturn(List.of(primeiro, segundo));

        finalizador.finaliza();

        InOrder ordem = inOrder(dao, sms);
        ordem.verify(dao).emAndamento();
        ordem.verify(dao).finalizaTodos(List.of(
                new FinalizacaoJogo(primeiro, BRUNO),
                new FinalizacaoJogo(segundo, BRUNO)));
        ordem.verify(sms).enviar(BRUNO, primeiro);
        ordem.verify(sms).enviar(BRUNO, segundo);
        ordem.verifyNoMoreInteractions();

        assertTrue(primeiro.isFinalizado());
        assertTrue(segundo.isFinalizado());
        assertEquals(BRUNO, primeiro.getVencedor());
        assertEquals(2, finalizador.getTotalFinalizados());
    }

    @Test
    void naoDeveEnviarSmsQuandoPersistenciaFalhar() {
        Jogo jogo = jogo("Corrida", "2026-10-20");
        when(dao.emAndamento()).thenReturn(List.of(jogo));
        doThrow(new IllegalStateException("Falha ao salvar")).when(dao).finalizaTodos(anyList());

        assertThrows(IllegalStateException.class, finalizador::finaliza);

        verifyNoInteractions(sms);
        assertFalse(jogo.isFinalizado());
        assertNull(jogo.getVencedor());
        assertEquals(0, finalizador.getTotalFinalizados());
    }
}

package br.com.valueprojects.mock_spring;

import static br.com.valueprojects.mock_spring.CenariosPratica2.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import br.com.valueprojects.mock_spring.model.*;
import infra.RepositorioJogos;

class FinalizaJogoComplementarTest {
    private final RepositorioJogos dao = mock(RepositorioJogos.class);
    private final EnviadorSms sms = mock(EnviadorSms.class);
    private final FinalizaJogo finalizador = new FinalizaJogo(dao, sms, RELOGIO);

    @Test
    void falhaNaConsultaNaoEnviaSms() {
        when(dao.emAndamento()).thenThrow(new IllegalStateException("Banco indisponível"));
        assertThrows(IllegalStateException.class, finalizador::finaliza);
        verifyNoInteractions(sms);
        verify(dao, never()).finalizaTodos(anyList());
    }

    @Test
    void semJogosNaoPersisteNemNotifica() {
        when(dao.emAndamento()).thenReturn(List.of());
        finalizador.finaliza();
        verify(dao, never()).finalizaTodos(anyList());
        verifyNoInteractions(sms);
        assertEquals(0, finalizador.getTotalFinalizados());
    }

    @Test
    void ignoraJogosAntigosDaSemanaAtualEFuturos() {
        when(dao.emAndamento()).thenReturn(List.of(jogo("Antigo", "2026-10-18"),
                jogo("Atual", "2026-10-26"), jogo("Futuro", "2026-11-01")));
        finalizador.finaliza();
        verify(dao, never()).finalizaTodos(anyList());
        verifyNoInteractions(sms);
    }

    @Test
    void incluiSegundaEDomingoDaSemanaAnterior() {
        var segunda = jogo("Segunda", "2026-10-19");
        var domingo = jogo("Domingo", "2026-10-25");
        when(dao.emAndamento()).thenReturn(List.of(segunda, domingo));
        finalizador.finaliza();
        assertEquals(2, finalizador.getTotalFinalizados());
        verify(sms).enviar(BRUNO, segunda);
        verify(sms).enviar(BRUNO, domingo);
    }

    @Test
    void loteComJogoSemResultadosNaoPersisteNemNotifica() {
        Jogo valido = jogo("Válido", "2026-10-20");
        Jogo semResultados = jogo("Sem resultados", "2026-10-21");
        // Construção explícita de jogo com a mesma data, sem pontuações.
        semResultados = new Jogo("Sem resultados", semResultados.getData());
        when(dao.emAndamento()).thenReturn(List.of(valido, semResultados));
        assertThrows(RuntimeException.class, finalizador::finaliza);
        verify(dao, never()).finalizaTodos(anyList());
        verifyNoInteractions(sms);
        assertFalse(valido.isFinalizado());
    }

    @Test
    void falhaDeSmsAconteceSomenteDepoisDaPersistenciaEMantemFinalizacao() {
        Jogo primeiro = jogo("Primeiro", "2026-10-20");
        Jogo segundo = jogo("Segundo", "2026-10-21");
        when(dao.emAndamento()).thenReturn(List.of(primeiro, segundo));
        doThrow(new IllegalStateException("Operadora indisponível")).when(sms).enviar(BRUNO, primeiro);
        assertThrows(IllegalStateException.class, finalizador::finaliza);
        InOrder ordem = inOrder(dao, sms);
        ordem.verify(dao).finalizaTodos(anyList());
        ordem.verify(sms).enviar(BRUNO, primeiro);
        verify(sms, never()).enviar(BRUNO, segundo);
        assertTrue(primeiro.isFinalizado());
        assertTrue(segundo.isFinalizado());
        assertEquals(2, finalizador.getTotalFinalizados());
    }

    @Test
    void totalRepresentaSomenteUltimaExecucao() {
        var jogo = jogo("Corrida", "2026-10-20");
        when(dao.emAndamento()).thenReturn(List.of(jogo)).thenReturn(List.of());
        finalizador.finaliza();
        assertEquals(1, finalizador.getTotalFinalizados());
        finalizador.finaliza();
        assertEquals(0, finalizador.getTotalFinalizados());
        verify(sms, times(1)).enviar(BRUNO, jogo);
    }

    @Test
    void permiteRelogioPadraoQuandoNaoHaJogos() {
        when(dao.emAndamento()).thenReturn(List.of());
        new FinalizaJogo(dao, sms).finaliza();
        verifyNoInteractions(sms);
    }
}

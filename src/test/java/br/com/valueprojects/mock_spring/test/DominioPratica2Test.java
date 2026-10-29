package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.*;
import static br.com.valueprojects.mock_spring.CenariosPratica2.*;
import java.util.Calendar;
import org.junit.jupiter.api.Test;
import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.model.*;

class DominioPratica2Test {
    @Test
    void limitaCincoResultadosPorParticipanteMesmoComAlternancia() {
        Jogo jogo = new Jogo("Pontuação");
        for (int i = 0; i < 6; i++) {
            jogo.anota(new Resultado(ANA, i));
            jogo.anota(new Resultado(BRUNO, i));
        }
        assertEquals(10, jogo.getResultados().size());
        assertThrows(UnsupportedOperationException.class, () -> jogo.getResultados().clear());
    }

    @Test
    void preservaDadosDoJogoDoParticipanteEDoPagamento() {
        Calendar data = Calendar.getInstance();
        var builder = new CriadorDeJogo().para("Corrida").naData(data).comId(42).resultado(ANA, 100);
        assertEquals(42, builder.getId());
        assertFalse(builder.isFinalizado());
        builder.finaliza();
        assertTrue(builder.isFinalizado());
        Jogo jogo = builder.constroi();
        assertEquals("Corrida", jogo.getDescricao());
        assertEquals(42, jogo.getId());
        assertEquals(ANA, jogo.getVencedor());
        Calendar copia = jogo.getData();
        copia.add(Calendar.YEAR, 1);
        assertEquals(data, jogo.getData());
        assertThrows(NullPointerException.class, () -> jogo.finaliza(null));

        Pagamento pagamento = new Pagamento(300, data);
        assertEquals(300, pagamento.getValor());
        assertEquals(data, pagamento.getData());
        assertEquals(1, ANA.getId());
        assertEquals("Ana", ANA.getNome());
        assertEquals("11900000001", ANA.getTelefone());
        assertEquals("", new Participante("Sem telefone").getTelefone());
    }

    @Test
    void igualdadeDeParticipantesRespeitaIdENome() {
        Participante igual = new Participante(1, "Ana", "outro telefone");
        assertEquals(ANA, ANA);
        assertEquals(ANA, igual);
        assertEquals(ANA.hashCode(), igual.hashCode());
        assertNotEquals(ANA, null);
        assertNotEquals(ANA, "Ana");
        assertNotEquals(ANA, BRUNO);
        assertNotEquals(ANA, new Participante(1, "Outro"));
        assertNotEquals(ANA, new Participante(1, null));
        Participante semNome = new Participante(1, null);
        assertEquals(semNome, new Participante(1, null));
        assertNotEquals(semNome, ANA);
    }
}

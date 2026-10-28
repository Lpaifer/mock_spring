package br.com.valueprojects.mock_spring.test;

import static br.com.valueprojects.mock_spring.CenariosPratica2.ANA;
import static br.com.valueprojects.mock_spring.CenariosPratica2.BRUNO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Juiz;
import br.com.valueprojects.mock_spring.model.Resultado;

public class JuizTest {
    @Test
    void deveIdentificarParticipanteVencedor() {
        Jogo jogo = new Jogo("Corrida");
        jogo.anota(new Resultado(BRUNO, 150));
        jogo.anota(new Resultado(ANA, 200));

        Juiz juiz = new Juiz();
        juiz.julga(jogo);

        assertEquals(ANA, juiz.getVencedor());
        assertEquals(200, juiz.getPrimeiroColocado());
        assertEquals(150, juiz.getUltimoColocado());
    }
}

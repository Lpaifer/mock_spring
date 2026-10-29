package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.*;
import static br.com.valueprojects.mock_spring.CenariosPratica2.*;
import org.junit.jupiter.api.Test;
import br.com.valueprojects.mock_spring.model.*;

class JuizComplementarTest {
    @Test
    void naoMisturaPontuacoesEntreJogos() {
        Juiz juiz = new Juiz();
        juiz.julga(jogo("Primeiro", "2026-10-20"));
        Jogo segundo = new Jogo("Segundo");
        segundo.anota(new Resultado(ANA, 10));
        juiz.julga(segundo);
        assertEquals(ANA, juiz.getVencedor());
        assertEquals(10, juiz.getPrimeiroColocado());
        assertEquals(10, juiz.getUltimoColocado());
    }

    @Test
    void empateMantemPrimeiroParticipanteComAMaiorPontuacao() {
        Jogo jogo = new Jogo("Empate");
        jogo.anota(new Resultado(ANA, 100));
        jogo.anota(new Resultado(BRUNO, 100));
        Juiz juiz = new Juiz();
        juiz.julga(jogo);
        assertEquals(ANA, juiz.getVencedor());
    }

    @Test
    void pontuacoesNegativasTambemSaoJulgadas() {
        Jogo jogo = new Jogo("Penalidades");
        jogo.anota(new Resultado(ANA, -10));
        jogo.anota(new Resultado(BRUNO, -20));
        Juiz juiz = new Juiz();
        juiz.julga(jogo);
        assertEquals(ANA, juiz.getVencedor());
        assertEquals(-20, juiz.getUltimoColocado());
    }

    @Test
    void semResultadosNaoHaVencedor() {
        Juiz juiz = new Juiz();
        assertThrows(RuntimeException.class, () -> juiz.julga(new Jogo("Vazio")));
        assertNull(juiz.getVencedor());
    }
}

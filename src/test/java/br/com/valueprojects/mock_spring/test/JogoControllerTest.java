package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.*;
import static br.com.valueprojects.mock_spring.CenariosPratica2.*;
import org.junit.jupiter.api.Test;
import br.com.valueprojects.mock_spring.controller.JogoController;
import br.com.valueprojects.mock_spring.model.Resultado;

class JogoControllerTest {
    @Test
    void preservaCadastroListagemEJulgamentoDoControllerExistente() {
        var controller = new JogoController();
        var jogo = controller.criarJogo("Corrida");
        assertSame(jogo, controller.listarJogos().get(0));
        jogo.anota(new Resultado(ANA, 100));
        jogo.anota(new Resultado(BRUNO, 200));
        assertEquals("Primeiro colocado: 200.0, Último colocado: 100.0", controller.julgarJogo(0));
    }
}

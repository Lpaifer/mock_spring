package infra;

import br.com.valueprojects.mock_spring.model.EnviadorSms;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Participante;

/** Adaptador didático; não realiza chamadas a uma operadora de SMS. */
public class SmsSimulado implements EnviadorSms {
    @Override
    public void enviar(Participante vencedor, Jogo jogo) {
        System.out.printf("SMS SIMULADO para %s (%s): você venceu o jogo %s!%n",
                vencedor.getNome(), vencedor.getTelefone(), jogo.getDescricao());
    }
}

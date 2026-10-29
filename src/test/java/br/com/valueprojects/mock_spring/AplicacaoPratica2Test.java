package br.com.valueprojects.mock_spring;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import infra.RepositorioJogos;

@ExtendWith(OutputCaptureExtension.class)
class AplicacaoPratica2Test {
    public static class CapturaContexto implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static ConfigurableApplicationContext contexto;

        @Override
        public void initialize(ConfigurableApplicationContext contexto) {
            CapturaContexto.contexto = contexto;
        }
    }

    @Test
    void aplicacaoExecutaDemonstracaoComBancoESmsSimulado(CapturedOutput saida) {
        try {
            Mock_SpringApplication.main(new String[]{"--spring.main.web-application-type=none",
                    "--pratica2.demo=true", "--context.initializer.classes=" + CapturaContexto.class.getName()});
            var salvos = CapturaContexto.contexto.getBean(RepositorioJogos.class).finalizados();
            assertEquals(2, salvos.size());
            assertEquals("Bruno", salvos.get(0).getVencedor().getNome());
            assertEquals("Ana", salvos.get(1).getVencedor().getNome());
            assertTrue(saida.getOut().contains("SMS SIMULADO para Bruno"));
            assertTrue(saida.getOut().contains("SMS SIMULADO para Ana"));
            assertTrue(saida.getOut().contains("Total finalizado: 2"));
        } finally {
            CapturaContexto.contexto.close();
        }
    }
}

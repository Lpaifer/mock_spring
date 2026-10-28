package br.com.valueprojects.mock_spring;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.model.EnviadorSms;
import br.com.valueprojects.mock_spring.model.FinalizaJogo;
import br.com.valueprojects.mock_spring.model.Participante;
import infra.JogoDao;
import infra.RepositorioJogos;
import infra.SmsSimulado;

@Configuration
public class Pratica2Config {
    @Bean
    RepositorioJogos repositorioJogos(DataSource dataSource) {
        return new JogoDao(dataSource);
    }

    @Bean
    EnviadorSms enviadorSms() {
        return new SmsSimulado();
    }

    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
    }

    @Bean
    FinalizaJogo finalizador(RepositorioJogos dao, EnviadorSms sms, Clock relogio) {
        return new FinalizaJogo(dao, sms, relogio);
    }

    @Bean
    CommandLineRunner demonstracao(RepositorioJogos dao, FinalizaJogo finalizador, Clock relogio,
            @Value("${pratica2.demo:false}") boolean executar) {
        return args -> {
            if (executar) {
                LocalDate segundaAnterior = LocalDate.now(relogio)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
                var data = GregorianCalendar.from(segundaAnterior.atTime(12, 0).atZone(relogio.getZone()));
                Participante ana = new Participante(1, "Ana", "11900000001");
                Participante bruno = new Participante(2, "Bruno", "11900000002");
                dao.salva(new CriadorDeJogo().para("Corrida").naData(data)
                        .resultado(ana, 150).resultado(bruno, 200).constroi());
                dao.salva(new CriadorDeJogo().para("Cata moedas").naData(data)
                        .resultado(ana, 300).resultado(bruno, 100).constroi());
                finalizador.finaliza();
                dao.finalizados().forEach(jogo -> System.out.printf("Jogo salvo: %s; vencedor: %s%n",
                        jogo.getDescricao(), jogo.getVencedor().getNome()));
                System.out.println("Total finalizado: " + finalizador.getTotalFinalizados());
            }
        };
    }
}

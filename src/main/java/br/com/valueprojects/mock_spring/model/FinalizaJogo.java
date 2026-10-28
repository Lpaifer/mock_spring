package br.com.valueprojects.mock_spring.model;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import infra.RepositorioJogos;

public class FinalizaJogo {
    private int total;
    private final RepositorioJogos dao;
    private final EnviadorSms sms;
    private final Clock relogio;

    public FinalizaJogo(RepositorioJogos dao, EnviadorSms sms) {
        this(dao, sms, Clock.systemDefaultZone());
    }

    public FinalizaJogo(RepositorioJogos dao, EnviadorSms sms, Clock relogio) {
        this.dao = dao;
        this.sms = sms;
        this.relogio = relogio;
    }

    public void finaliza() {
        total = 0;
        LocalDate inicioAtual = LocalDate.now(relogio)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate inicioAnterior = inicioAtual.minusWeeks(1);
        List<FinalizacaoJogo> lote = new ArrayList<>();

        for (Jogo jogo : dao.emAndamento()) {
            LocalDate data = jogo.getData().toInstant().atZone(relogio.getZone()).toLocalDate();
            if (!data.isBefore(inicioAnterior) && data.isBefore(inicioAtual)) {
                Juiz juiz = new Juiz();
                juiz.julga(jogo);
                lote.add(new FinalizacaoJogo(jogo, juiz.getVencedor()));
            }
        }
        if (lote.isEmpty()) {
            return;
        }

        // Só retorna após o commit de todo o lote; falha impede qualquer SMS.
        dao.finalizaTodos(List.copyOf(lote));
        for (FinalizacaoJogo item : lote) {
            item.jogo().finaliza(item.vencedor());
        }
        total = lote.size();
        for (FinalizacaoJogo item : lote) {
            sms.enviar(item.vencedor(), item.jogo());
        }
    }

    public int getTotalFinalizados() {
        return total;
    }
}

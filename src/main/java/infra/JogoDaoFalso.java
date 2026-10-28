package infra;

import java.util.ArrayList;
import java.util.List;
import br.com.valueprojects.mock_spring.model.FinalizacaoJogo;
import br.com.valueprojects.mock_spring.model.Jogo;

/** Fake em memória, com dados isolados por instância. */
public class JogoDaoFalso implements RepositorioJogos {
    private final List<Jogo> jogos = new ArrayList<>();

    @Override
    public void salva(Jogo jogo) {
        jogos.add(jogo);
    }

    @Override
    public List<Jogo> emAndamento() {
        return jogos.stream().filter(jogo -> !jogo.isFinalizado()).toList();
    }

    @Override
    public List<Jogo> finalizados() {
        return jogos.stream().filter(Jogo::isFinalizado).toList();
    }

    @Override
    public void finalizaTodos(List<FinalizacaoJogo> lote) {
        for (FinalizacaoJogo item : lote) {
            if (!jogos.contains(item.jogo())) {
                throw new IllegalStateException("Jogo não cadastrado no DAO falso");
            }
        }
        lote.forEach(item -> item.jogo().finaliza(item.vencedor()));
    }
}

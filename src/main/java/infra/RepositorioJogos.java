package infra;

import java.util.List;
import br.com.valueprojects.mock_spring.model.FinalizacaoJogo;
import br.com.valueprojects.mock_spring.model.Jogo;

public interface RepositorioJogos {
    void salva(Jogo jogo);
    List<Jogo> emAndamento();
    List<Jogo> finalizados();

    /** Retorna apenas depois de confirmar a persistência de todo o lote. */
    void finalizaTodos(List<FinalizacaoJogo> finalizacoes);
}

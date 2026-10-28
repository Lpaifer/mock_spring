package infra;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.sql.DataSource;
import br.com.valueprojects.mock_spring.model.FinalizacaoJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Participante;
import br.com.valueprojects.mock_spring.model.Resultado;

public class JogoDao implements RepositorioJogos {
    private final DataSource dataSource;

    public JogoDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void salva(Jogo jogo) {
        int id = transacao(conexao -> {
            try (PreparedStatement ps = conexao.prepareStatement(
                    "INSERT INTO JOGO (DESCRICAO, DATA, FINALIZADO) VALUES (?, ?, FALSE)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, jogo.getDescricao());
                ps.setDate(2, new Date(jogo.getData().getTimeInMillis()));
                ps.executeUpdate();
                try (ResultSet chaves = ps.getGeneratedKeys()) {
                    if (!chaves.next()) {
                        throw new SQLException("Banco não retornou o ID do jogo");
                    }
                    int gerado = chaves.getInt(1);
                    for (Resultado resultado : jogo.getResultados()) {
                        salvaParticipante(conexao, resultado.getParticipante());
                        try (PreparedStatement resultadoSql = conexao.prepareStatement(
                                "INSERT INTO RESULTADOS (JOGO_ID, PARTICIPANTE_ID, VALOR) VALUES (?, ?, ?)")) {
                            resultadoSql.setInt(1, gerado);
                            resultadoSql.setInt(2, resultado.getParticipante().getId());
                            resultadoSql.setDouble(3, resultado.getMetrica());
                            resultadoSql.executeUpdate();
                        }
                    }
                    return gerado;
                }
            }
        });
        jogo.setId(id);
    }

    private void salvaParticipante(Connection conexao, Participante participante) throws SQLException {
        boolean cadastrado;
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT ID FROM PARTICIPANTE WHERE ID = ?")) {
            consulta.setInt(1, participante.getId());
            try (ResultSet existente = consulta.executeQuery()) {
                cadastrado = existente.next();
            }
        }
        if (cadastrado) {
            return;
        }
        try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO PARTICIPANTE (ID, NOME, TELEFONE) VALUES (?, ?, ?)")) {
            ps.setInt(1, participante.getId());
            ps.setString(2, participante.getNome());
            ps.setString(3, participante.getTelefone());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Jogo> emAndamento() {
        return busca(false);
    }

    @Override
    public List<Jogo> finalizados() {
        return busca(true);
    }

    private List<Jogo> busca(boolean finalizado) {
        return transacao(conexao -> {
            List<Jogo> jogos = new ArrayList<>();
            String sql = "SELECT J.*, P.NOME AS VENCEDOR_NOME, P.TELEFONE AS VENCEDOR_TELEFONE "
                    + "FROM JOGO J LEFT JOIN PARTICIPANTE P ON P.ID = J.VENCEDOR_ID "
                    + "WHERE J.FINALIZADO = ? ORDER BY J.ID";
            try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                ps.setBoolean(1, finalizado);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Calendar data = Calendar.getInstance();
                        data.setTime(rs.getDate("DATA"));
                        Jogo jogo = new Jogo(rs.getString("DESCRICAO"), data);
                        jogo.setId(rs.getInt("ID"));
                        carregaResultados(conexao, jogo);
                        if (finalizado) {
                            jogo.finaliza(new Participante(rs.getInt("VENCEDOR_ID"),
                                    rs.getString("VENCEDOR_NOME"), rs.getString("VENCEDOR_TELEFONE")));
                        }
                        jogos.add(jogo);
                    }
                }
            }
            return jogos;
        });
    }

    private void carregaResultados(Connection conexao, Jogo jogo) throws SQLException {
        String sql = "SELECT R.VALOR, P.ID, P.NOME, P.TELEFONE FROM RESULTADOS R "
                + "JOIN PARTICIPANTE P ON P.ID = R.PARTICIPANTE_ID WHERE R.JOGO_ID = ? ORDER BY R.ID";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, jogo.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Participante participante = new Participante(rs.getInt("ID"),
                            rs.getString("NOME"), rs.getString("TELEFONE"));
                    jogo.anota(new Resultado(participante, rs.getDouble("VALOR")));
                }
            }
        }
    }

    @Override
    public void finalizaTodos(List<FinalizacaoJogo> lote) {
        transacao(conexao -> {
            String sql = "UPDATE JOGO SET FINALIZADO = TRUE, VENCEDOR_ID = ? "
                    + "WHERE ID = ? AND FINALIZADO = FALSE AND EXISTS "
                    + "(SELECT 1 FROM RESULTADOS WHERE JOGO_ID = ? AND PARTICIPANTE_ID = ?)";
            try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                for (FinalizacaoJogo item : lote) {
                    ps.setInt(1, item.vencedor().getId());
                    ps.setInt(2, item.jogo().getId());
                    ps.setInt(3, item.jogo().getId());
                    ps.setInt(4, item.vencedor().getId());
                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException("Jogo não pode ser finalizado: " + item.jogo().getId());
                    }
                }
            }
            return null;
        });
    }

    private <T> T transacao(OperacaoSql<T> operacao) {
        try (Connection conexao = dataSource.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                T resultado = operacao.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException erro) {
                conexao.rollback();
                throw erro;
            }
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha na persistência dos jogos", erro);
        }
    }

    @FunctionalInterface
    private interface OperacaoSql<T> {
        T executar(Connection conexao) throws SQLException;
    }
}

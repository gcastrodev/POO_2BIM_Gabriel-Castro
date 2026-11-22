package br.edu.avaliacao.sacm.repository;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.enums.StatusPessoa;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;
import br.edu.avaliacao.sacm.model.Medico;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicoRepository {
  private static final String SELECT_BASE = """
            SELECT p.id, p.nome, p.cpf, p.data_nascimento, p.email, p.telefone,
                   p.cidade, p.estado, p.data_cadastro, p.status,
                   m.crm, m.especialidade, m.disponibilidade, m.valor_consulta
            FROM pessoa p
            JOIN medico m ON m.pessoa_id = p.id
            """;

  private final JdbcTemplate jdbc;

  public MedicoRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public Medico salvar(Medico medico) {
    try {
      Long id = inserirPessoa(medico);
      medico.definirId(id);
      jdbc.update("""
                    INSERT INTO medico (pessoa_id, crm, especialidade, disponibilidade, valor_consulta)
                    VALUES (?, ?, ?, ?, ?)
                    """,
          id, medico.getCrm(), medico.getEspecialidade().name(),
          medico.getDisponibilidade().name(), medico.getValorConsulta());
      return medico;
    } catch (DuplicateKeyException e) {
      throw new RegraNegocioException("CPF ou CRM já cadastrado.", e);
    } catch (DataAccessException e) {
      throw new RegraNegocioException("Erro ao salvar o médico no banco de dados.", e);
    }
  }

  @Transactional
  public void atualizar(Medico medico) {
    try {
      jdbc.update("UPDATE pessoa SET email = ?, telefone = ?, status = ? WHERE id = ?",
          medico.getEmail(), medico.getTelefone(),
          medico.getStatus().name(), medico.getId());
      jdbc.update("UPDATE medico SET disponibilidade = ? WHERE pessoa_id = ?",
          medico.getDisponibilidade().name(), medico.getId());
    } catch (DataAccessException e) {
      throw new RegraNegocioException("Erro ao atualizar o médico no banco de dados.", e);
    }
  }

  public Optional<Medico> buscarPorId(Long id) {
    List<Medico> resultado = jdbc.query(SELECT_BASE + " WHERE p.id = ?", this::mapear, id);
    return resultado.stream().findFirst();
  }

  public List<Medico> listarTodos() {
    return jdbc.query(SELECT_BASE + " ORDER BY p.nome", this::mapear);
  }

  public boolean existePorCrm(String crm) {
    Integer total = jdbc.queryForObject(
        "SELECT COUNT(*) FROM medico WHERE crm = ?", Integer.class, crm);
    return total != null && total > 0;
  }

  public boolean existePorCpf(String cpf) {
    Integer total = jdbc.queryForObject(
        "SELECT COUNT(*) FROM pessoa WHERE cpf = ?", Integer.class, cpf);
    return total != null && total > 0;
  }

  private Long inserirPessoa(Medico p) {
    String sql = """
                INSERT INTO pessoa (nome, cpf, data_nascimento, email, telefone,
                                    cidade, estado, data_cadastro, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, p.getNome());
      ps.setString(2, p.getCpf());
      ps.setDate(3, Date.valueOf(p.getDataNascimento()));
      ps.setString(4, p.getEmail());
      ps.setString(5, p.getTelefone());
      ps.setString(6, p.getCidade());
      ps.setString(7, p.getEstado());
      ps.setTimestamp(8, Timestamp.valueOf(p.getDataCadastro()));
      ps.setString(9, p.getStatus().name());
      return ps;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  private Medico mapear(ResultSet rs, int linha) throws SQLException {
    return new Medico(
        rs.getLong("id"),
        rs.getString("nome"),
        rs.getString("cpf"),
        rs.getObject("data_nascimento", LocalDate.class),
        rs.getString("email"),
        rs.getString("telefone"),
        rs.getString("cidade"),
        rs.getString("estado"),
        rs.getObject("data_cadastro", LocalDateTime.class),
        StatusPessoa.valueOf(rs.getString("status")),
        rs.getString("crm"),
        Especialidade.valueOf(rs.getString("especialidade")),
        DisponibilidadeMedico.valueOf(rs.getString("disponibilidade")),
        rs.getBigDecimal("valor_consulta"));
  }
}

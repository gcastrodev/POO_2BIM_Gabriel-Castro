package br.edu.avaliacao.sacm.repository;

import br.edu.avaliacao.sacm.enums.StatusPessoa;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;
import br.edu.avaliacao.sacm.model.Paciente;
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
public class PacienteRepository {
  private static final String SELECT_BASE = """
            SELECT p.id, p.nome, p.cpf, p.data_nascimento, p.email, p.telefone,
                   p.cidade, p.estado, p.data_cadastro, p.status,
                   pa.numero_prontuario, pa.convenio, pa.contato_emergencia,
                   pa.telefone_emergencia
            FROM pessoa p
            JOIN paciente pa ON pa.pessoa_id = p.id
            """;

  private final JdbcTemplate jdbc;

  public PacienteRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public Paciente salvar(Paciente paciente) {
    try {
      Long id = inserirPessoa(paciente);
      paciente.definirId(id);
      paciente.gerarNumeroProntuario();   // PRT-000001, a partir do id
      jdbc.update("""
                    INSERT INTO paciente (pessoa_id, numero_prontuario, convenio,
                                          contato_emergencia, telefone_emergencia)
                    VALUES (?, ?, ?, ?, ?)
                    """,
          id, paciente.getNumeroProntuario(), paciente.getConvenio(),
          paciente.getContatoEmergencia(), paciente.getTelefoneEmergencia());
      return paciente;
    } catch (DuplicateKeyException e) {
      throw new RegraNegocioException("CPF já cadastrado.", e);
    } catch (DataAccessException e) {
      throw new RegraNegocioException("Erro ao salvar o paciente no banco de dados.", e);
    }
  }

  @Transactional
  public void atualizar(Paciente paciente) {
    try {
      jdbc.update("UPDATE pessoa SET email = ?, telefone = ?, status = ? WHERE id = ?",
          paciente.getEmail(), paciente.getTelefone(),
          paciente.getStatus().name(), paciente.getId());
      jdbc.update("""
                    UPDATE paciente SET convenio = ?, contato_emergencia = ?,
                                        telefone_emergencia = ?
                    WHERE pessoa_id = ?
                    """,
          paciente.getConvenio(), paciente.getContatoEmergencia(),
          paciente.getTelefoneEmergencia(), paciente.getId());
    } catch (DataAccessException e) {
      throw new RegraNegocioException("Erro ao atualizar o paciente no banco de dados.", e);
    }
  }

  public Optional<Paciente> buscarPorId(Long id) {
    List<Paciente> resultado = jdbc.query(SELECT_BASE + " WHERE p.id = ?", this::mapear, id);
    return resultado.stream().findFirst();
  }

  public List<Paciente> listarTodos() {
    return jdbc.query(SELECT_BASE + " ORDER BY p.nome", this::mapear);
  }

  public boolean existePorCpf(String cpf) {
    Integer total = jdbc.queryForObject(
        "SELECT COUNT(*) FROM pessoa WHERE cpf = ?", Integer.class, cpf);
    return total != null && total > 0;
  }

  private Long inserirPessoa(Paciente p) {
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

  private Paciente mapear(ResultSet rs, int linha) throws SQLException {
    return new Paciente(
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
        rs.getString("numero_prontuario"),
        rs.getString("convenio"),
        rs.getString("contato_emergencia"),
        rs.getString("telefone_emergencia"));
  }
}

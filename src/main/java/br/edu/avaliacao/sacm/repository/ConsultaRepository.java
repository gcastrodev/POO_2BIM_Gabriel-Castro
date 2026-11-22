package br.edu.avaliacao.sacm.repository;

import br.edu.avaliacao.sacm.enums.StatusConsulta;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;
import br.edu.avaliacao.sacm.model.Consulta;
import br.edu.avaliacao.sacm.model.Medico;
import br.edu.avaliacao.sacm.model.Paciente;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ConsultaRepository {
  private static final String SELECT_BASE = """
            SELECT id, medico_id, paciente_id, data_hora, motivo, status,
                   data_agendamento, observacao
            FROM consulta
            """;

  // Dados crus da tabela, antes de buscar o médico e o paciente
  private record Linha(Long id, Long medicoId, Long pacienteId, LocalDateTime dataHora,
                       String motivo, StatusConsulta status,
                       LocalDateTime dataAgendamento, String observacao) {}

  private final JdbcTemplate jdbc;
  private final MedicoRepository medicoRepository;
  private final PacienteRepository pacienteRepository;

  public ConsultaRepository(JdbcTemplate jdbc, MedicoRepository medicoRepository,
                            PacienteRepository pacienteRepository) {
    this.jdbc = jdbc;
    this.medicoRepository = medicoRepository;
    this.pacienteRepository = pacienteRepository;
  }

  public Consulta salvar(Consulta c) {
    String sql = """
                INSERT INTO consulta (medico_id, paciente_id, data_hora, motivo,
                                      status, data_agendamento, observacao)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
    try {
      KeyHolder keyHolder = new GeneratedKeyHolder();
      jdbc.update(con -> {
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, c.getMedico().getId());
        ps.setLong(2, c.getPaciente().getId());
        ps.setTimestamp(3, Timestamp.valueOf(c.getDataHora()));
        ps.setString(4, c.getMotivo());
        ps.setString(5, c.getStatus().name());
        ps.setTimestamp(6, Timestamp.valueOf(c.getDataAgendamento()));
        ps.setString(7, c.getObservacao());
        return ps;
      }, keyHolder);
      c.definirId(keyHolder.getKey().longValue());
      return c;
    } catch (DataAccessException e) {
      throw new RegraNegocioException("Erro ao salvar a consulta no banco de dados.", e);
    }
  }

  // RN07: status e observação alterados precisam ir para o banco
  public void atualizar(Consulta c) {
    try {
      jdbc.update("UPDATE consulta SET status = ?, observacao = ? WHERE id = ?",
          c.getStatus().name(), c.getObservacao(), c.getId());
    } catch (DataAccessException e) {
      throw new RegraNegocioException("Erro ao atualizar a consulta no banco de dados.", e);
    }
  }

  public Optional<Consulta> buscarPorId(Long id) {
    return consultar("WHERE id = ?", id).stream().findFirst();
  }

  public List<Consulta> listarTodas() {
    return consultar("");
  }

  public List<Consulta> listarPorMedico(Long medicoId) {
    return consultar("WHERE medico_id = ?", medicoId);
  }

  public List<Consulta> listarPorPaciente(Long pacienteId) {
    return consultar("WHERE paciente_id = ?", pacienteId);
  }

  // RN04: só AGENDADA e CONFIRMADA bloqueiam o horário (RN07)
  public boolean existeConflitoMedico(Long medicoId, LocalDateTime dataHora) {
    Integer total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM consulta
                WHERE medico_id = ? AND data_hora = ?
                  AND status IN ('AGENDADA', 'CONFIRMADA')
                """, Integer.class, medicoId, Timestamp.valueOf(dataHora));
    return total != null && total > 0;
  }

  public boolean existeConflitoPaciente(Long pacienteId, LocalDateTime dataHora) {
    Integer total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM consulta
                WHERE paciente_id = ? AND data_hora = ?
                  AND status IN ('AGENDADA', 'CONFIRMADA')
                """, Integer.class, pacienteId, Timestamp.valueOf(dataHora));
    return total != null && total > 0;
  }

  private List<Consulta> consultar(String where, Object... args) {
    List<Linha> linhas = jdbc.query(SELECT_BASE + where + " ORDER BY data_hora",
        (rs, i) -> new Linha(
            rs.getLong("id"),
            rs.getLong("medico_id"),
            rs.getLong("paciente_id"),
            rs.getObject("data_hora", LocalDateTime.class),
            rs.getString("motivo"),
            StatusConsulta.valueOf(rs.getString("status")),
            rs.getObject("data_agendamento", LocalDateTime.class),
            rs.getString("observacao")),
        args);
    return linhas.stream().map(this::montar).toList();
  }

  private Consulta montar(Linha l) {
    Medico medico = medicoRepository.buscarPorId(l.medicoId())
        .orElseThrow(() -> new RegraNegocioException(
            "Médico não encontrado: id " + l.medicoId()));
    Paciente paciente = pacienteRepository.buscarPorId(l.pacienteId())
        .orElseThrow(() -> new RegraNegocioException(
            "Paciente não encontrado: id " + l.pacienteId()));
    return new Consulta(l.id(), medico, paciente, l.dataHora(), l.motivo(),
        l.status(), l.dataAgendamento(), l.observacao());
  }
}

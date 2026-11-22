package br.edu.avaliacao.sacm.model;

import br.edu.avaliacao.sacm.enums.StatusConsulta;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Consulta {
  private static final DateTimeFormatter FORMATO =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private Long id;
  private final Medico medico;
  private final Paciente paciente;
  private final LocalDateTime dataHora;
  private final String motivo;
  private StatusConsulta status;
  private final LocalDateTime dataAgendamento;
  private String observacao;

  // Agendamento novo: começa AGENDADA (RN06) e exige data futura (RN05)
  public Consulta(Medico medico, Paciente paciente, LocalDateTime dataHora, String motivo) {
    this(null, medico, paciente, dataHora, motivo,
        StatusConsulta.AGENDADA, LocalDateTime.now(), null);
    if (!dataHora.isAfter(LocalDateTime.now())) {
      throw new RegraNegocioException("A data/hora da consulta deve ser futura.");
    }
  }

  // Reconstrução a partir do banco (consultas antigas não passam pela regra de data futura)
  public Consulta(Long id, Medico medico, Paciente paciente, LocalDateTime dataHora,
                  String motivo, StatusConsulta status,
                  LocalDateTime dataAgendamento, String observacao) {
    if (medico == null || paciente == null) {
      throw new RegraNegocioException("Médico e paciente são obrigatórios.");
    }
    if (dataHora == null) {
      throw new RegraNegocioException("A data/hora é obrigatória.");
    }
    if (motivo == null || motivo.isBlank()) {
      throw new RegraNegocioException("O motivo da consulta é obrigatório.");
    }
    this.id = id;
    this.medico = medico;
    this.paciente = paciente;
    this.dataHora = dataHora;
    this.motivo = motivo.trim();
    this.status = status;
    this.dataAgendamento = dataAgendamento;
    this.observacao = observacao;
  }

  public void definirId(Long id) {
    if (this.id != null) {
      throw new IllegalStateException("O id já foi definido.");
    }
    this.id = id;
  }

  // RN06: AGENDADA -> CONFIRMADA
  public void confirmar() {
    if (status != StatusConsulta.AGENDADA) {
      throw new RegraNegocioException(
          "Só é possível confirmar uma consulta AGENDADA. Status atual: " + status);
    }
    this.status = StatusConsulta.CONFIRMADA;
  }

  // RN06: AGENDADA ou CONFIRMADA -> CANCELADA
  public void cancelar() {
    if (!isAtiva()) {
      throw new RegraNegocioException(
          "Só é possível cancelar uma consulta ativa. Status atual: " + status);
    }
    this.status = StatusConsulta.CANCELADA;
  }

  // RN06: CONFIRMADA -> FINALIZADA
  public void finalizar() {
    if (status != StatusConsulta.CONFIRMADA) {
      throw new RegraNegocioException(
          "Só é possível finalizar uma consulta CONFIRMADA. Status atual: " + status);
    }
    this.status = StatusConsulta.FINALIZADA;
  }

  // "Ativa" = ocupa o horário do médico e do paciente (RN04 / RN07)
  public boolean isAtiva() {
    return status == StatusConsulta.AGENDADA || status == StatusConsulta.CONFIRMADA;
  }

  public String getResumo() {
    return String.format("Consulta #%s | %s | %s | %s | %s | %s",
        id, dataHora.format(FORMATO), medico.getNome(),
        paciente.getNome(), status, motivo);
  }

  public Long getId() {
    return id;
  }

  public Medico getMedico() {
    return medico;
  }

  public Paciente getPaciente() {
    return paciente;
  }

  public LocalDateTime getDataHora() {
    return dataHora;
  }

  public String getMotivo() {
    return motivo;
  }

  public StatusConsulta getStatus() {
    return status;
  }

  public LocalDateTime getDataAgendamento() {
    return dataAgendamento;
  }

  public String getObservacao() {
    return observacao;
  }

}

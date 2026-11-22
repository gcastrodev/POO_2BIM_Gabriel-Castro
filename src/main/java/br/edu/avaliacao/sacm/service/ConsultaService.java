package br.edu.avaliacao.sacm.service;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.exception.ConflitoHorarioException;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;
import br.edu.avaliacao.sacm.interfaces.Agendavel;
import br.edu.avaliacao.sacm.model.Consulta;
import br.edu.avaliacao.sacm.model.Medico;
import br.edu.avaliacao.sacm.model.Paciente;
import br.edu.avaliacao.sacm.repository.ConsultaRepository;
import br.edu.avaliacao.sacm.repository.MedicoRepository;
import br.edu.avaliacao.sacm.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ConsultaService implements Agendavel {
  private static final DateTimeFormatter FORMATO =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final MedicoRepository medicoRepository;
  private final PacienteRepository pacienteRepository;
  private final ConsultaRepository consultaRepository;

  public ConsultaService(MedicoRepository medicoRepository,
                         PacienteRepository pacienteRepository,
                         ConsultaRepository consultaRepository) {
    this.medicoRepository = medicoRepository;
    this.pacienteRepository = pacienteRepository;
    this.consultaRepository = consultaRepository;
  }

  // ---------- Cadastros (RN01) ----------

  public Medico cadastrarMedico(String nome, String cpf, LocalDate dataNascimento,
                                String email, String telefone, String cidade, String estado,
                                String crm, Especialidade especialidade,
                                BigDecimal valorConsulta) {
    // O construtor valida e normaliza CPF e CRM; só depois checamos duplicidade
    Medico medico = new Medico(nome, cpf, dataNascimento, email, telefone,
        cidade, estado, crm, especialidade, valorConsulta);
    if (medicoRepository.existePorCpf(medico.getCpf())) {
      throw new RegraNegocioException("Já existe uma pessoa cadastrada com este CPF.");
    }
    if (medicoRepository.existePorCrm(medico.getCrm())) {
      throw new RegraNegocioException("Já existe um médico cadastrado com este CRM.");
    }
    return medicoRepository.salvar(medico);
  }

  public Paciente cadastrarPaciente(String nome, String cpf, LocalDate dataNascimento,
                                    String email, String telefone, String cidade, String estado,
                                    String convenio, String contatoEmergencia,
                                    String telefoneEmergencia) {
    Paciente paciente = new Paciente(nome, cpf, dataNascimento, email, telefone,
        cidade, estado, convenio, contatoEmergencia, telefoneEmergencia);
    if (pacienteRepository.existePorCpf(paciente.getCpf())) {
      throw new RegraNegocioException("Já existe uma pessoa cadastrada com este CPF.");
    }
    return pacienteRepository.salvar(paciente);
  }

  public void alterarDisponibilidadeMedico(Long medicoId, DisponibilidadeMedico valor) {
    Medico medico = buscarMedico(medicoId);
    medico.alterarDisponibilidade(valor);
    medicoRepository.atualizar(medico);
  }

  public List<Medico> listarMedicos() {
    return medicoRepository.listarTodos();
  }

  public List<Paciente> listarPacientes() {
    return pacienteRepository.listarTodos();
  }

  // ---------- Agendamento (RN02 a RN05) ----------

  @Override
  @Transactional
  public Consulta agendarConsulta(Long medicoId, Long pacienteId,
                                  LocalDateTime dataHora, String motivo) {
    Medico medico = buscarMedico(medicoId);
    Paciente paciente = buscarPaciente(pacienteId);

    // RN02: ambos precisam estar ATIVOS
    if (!medico.isAtivo()) {
      throw new RegraNegocioException("O médico está INATIVO e não pode receber agendamentos.");
    }
    if (!paciente.podeAgendar()) {
      throw new RegraNegocioException("O paciente está INATIVO e não pode agendar consultas.");
    }
    // RN03: médico precisa estar DISPONIVEL
    if (!medico.isDisponivel()) {
      throw new RegraNegocioException("O médico está INDISPONÍVEL para novos agendamentos.");
    }

    // RN05: o construtor valida motivo preenchido e data futura
    Consulta consulta = new Consulta(medico, paciente, dataHora, motivo);

    // RN04: sem conflito de horário
    verificarConflitoHorario(medico, paciente, dataHora);

    return consultaRepository.salvar(consulta);
  }

  // RN04 + RN07: só consultas AGENDADA/CONFIRMADA bloqueiam o horário
  public void verificarConflitoHorario(Medico medico, Paciente paciente, LocalDateTime dataHora) {
    if (consultaRepository.existeConflitoMedico(medico.getId(), dataHora)) {
      throw new ConflitoHorarioException("O médico " + medico.getNome()
          + " já possui consulta AGENDADA ou CONFIRMADA em " + dataHora.format(FORMATO) + ".");
    }
    if (consultaRepository.existeConflitoPaciente(paciente.getId(), dataHora)) {
      throw new ConflitoHorarioException("O paciente " + paciente.getNome()
          + " já possui consulta AGENDADA ou CONFIRMADA em " + dataHora.format(FORMATO) + ".");
    }
  }

  // ---------- Mudanças de status (RN06 e RN07) ----------

  @Override
  public void cancelarConsulta(Long consultaId) {
    Consulta consulta = buscarConsulta(consultaId);
    consulta.cancelar();
    consultaRepository.atualizar(consulta);
  }

  public void confirmarConsulta(Long consultaId) {
    Consulta consulta = buscarConsulta(consultaId);
    consulta.confirmar();
    consultaRepository.atualizar(consulta);
  }

  public void finalizarConsulta(Long consultaId) {
    Consulta consulta = buscarConsulta(consultaId);
    consulta.finalizar();
    consultaRepository.atualizar(consulta);
  }

  // ---------- Consultas ----------

  public List<Consulta> listarConsultas() {
    return consultaRepository.listarTodas();
  }

  public List<Consulta> listarConsultasPorMedico(Long medicoId) {
    buscarMedico(medicoId);
    return consultaRepository.listarPorMedico(medicoId);
  }

  public List<Consulta> listarConsultasPorPaciente(Long pacienteId) {
    buscarPaciente(pacienteId);
    return consultaRepository.listarPorPaciente(pacienteId);
  }

  // ---------- Auxiliares ----------

  private Medico buscarMedico(Long id) {
    return medicoRepository.buscarPorId(id)
        .orElseThrow(() -> new RegraNegocioException("Médico não encontrado: id " + id));
  }

  private Paciente buscarPaciente(Long id) {
    return pacienteRepository.buscarPorId(id)
        .orElseThrow(() -> new RegraNegocioException("Paciente não encontrado: id " + id));
  }

  private Consulta buscarConsulta(Long id) {
    return consultaRepository.buscarPorId(id)
        .orElseThrow(() -> new RegraNegocioException("Consulta não encontrada: id " + id));
  }
}

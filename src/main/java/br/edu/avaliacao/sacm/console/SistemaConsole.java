package br.edu.avaliacao.sacm.console;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;
import br.edu.avaliacao.sacm.model.Consulta;
import br.edu.avaliacao.sacm.model.Medico;
import br.edu.avaliacao.sacm.model.Paciente;
import br.edu.avaliacao.sacm.service.ConsultaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

@Component
public class SistemaConsole implements CommandLineRunner {
  private static final DateTimeFormatter FORMATO_DATA =
      DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter FORMATO_DATA_HORA =
      DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

  private final ConsultaService consultaService;
  private final Scanner scanner = new Scanner(System.in);
  private boolean executando = true;

  public SistemaConsole(ConsultaService consultaService) {
    this.consultaService = consultaService;
  }

  @Override
  public void run(String... args) {
    while (executando) {
      try {
        exibirMenu();
      } catch (RegraNegocioException e) {
        // RN09: mensagem compreensível; a causa técnica continua em e.getCause()
        System.out.println("\n[ERRO] " + e.getMessage());
      } catch (NoSuchElementException e) {
        executando = false;   // entrada do teclado encerrada
      }
    }
  }

  public void exibirMenu() {
    System.out.println("""

                ===== SACM - Agendamento de Consultas =====
                1 - Cadastrar médico
                2 - Cadastrar paciente
                3 - Listar médicos e pacientes
                4 - Alterar disponibilidade do médico
                5 - Agendar consulta
                6 - Listar consultas
                7 - Alterar status da consulta
                0 - Encerrar
                """);
    String opcao = lerTexto("Opção: ");
    switch (opcao) {
      case "1" -> cadastrarMedico();
      case "2" -> cadastrarPaciente();
      case "3" -> listarPessoas();
      case "4" -> alterarDisponibilidade();
      case "5" -> agendarConsulta();
      case "6" -> listarConsultas();
      case "7" -> alterarStatusConsulta();
      case "0" -> encerrar();
      default -> System.out.println("Opção inválida.");
    }
  }

  public void cadastrarMedico() {
    System.out.println("\n--- Cadastro de médico ---");
    String nome = lerTexto("Nome: ");
    String cpf = lerTexto("CPF (11 dígitos): ");
    LocalDate nascimento = lerData("Data de nascimento (dd/MM/aaaa): ");
    String email = lerTexto("E-mail: ");
    String telefone = lerTexto("Telefone: ");
    String cidade = lerTexto("Cidade: ");
    String estado = lerTexto("Estado (UF): ");
    String crm = lerTexto("CRM: ");
    Especialidade especialidade = lerEspecialidade();
    BigDecimal valor = lerValor("Valor da consulta (ex.: 250,00): ");

    Medico medico = consultaService.cadastrarMedico(nome, cpf, nascimento, email,
        telefone, cidade, estado, crm, especialidade, valor);
    System.out.println("Médico cadastrado com sucesso! ID: " + medico.getId());
  }

  public void cadastrarPaciente() {
    System.out.println("\n--- Cadastro de paciente ---");
    String nome = lerTexto("Nome: ");
    String cpf = lerTexto("CPF (11 dígitos): ");
    LocalDate nascimento = lerData("Data de nascimento (dd/MM/aaaa): ");
    String email = lerTexto("E-mail: ");
    String telefone = lerTexto("Telefone: ");
    String cidade = lerTexto("Cidade: ");
    String estado = lerTexto("Estado (UF): ");
    String convenio = lerTexto("Convênio: ");
    String contato = lerTexto("Contato de emergência (nome): ");
    String telefoneEmergencia = lerTexto("Telefone de emergência: ");

    Paciente paciente = consultaService.cadastrarPaciente(nome, cpf, nascimento, email,
        telefone, cidade, estado, convenio, contato, telefoneEmergencia);
    System.out.println("Paciente cadastrado com sucesso! ID: " + paciente.getId()
        + " | Prontuário: " + paciente.getNumeroProntuario());
  }

  private void listarPessoas() {
    List<Medico> medicos = consultaService.listarMedicos();
    System.out.println("\n--- Médicos ---");
    if (medicos.isEmpty()) {
      System.out.println("Nenhum médico cadastrado.");
    }
    for (Medico m : medicos) {
      System.out.println("[ID " + m.getId() + "] " + m.getDescricao()
          + " | " + m.getStatus() + " | " + m.getDisponibilidade());
    }
    List<Paciente> pacientes = consultaService.listarPacientes();
    System.out.println("\n--- Pacientes ---");
    if (pacientes.isEmpty()) {
      System.out.println("Nenhum paciente cadastrado.");
    }
    for (Paciente p : pacientes) {
      System.out.println("[ID " + p.getId() + "] " + p.getDescricao()
          + " | " + p.getStatus());
    }
  }

  private void alterarDisponibilidade() {
    Long medicoId = lerLong("ID do médico: ");
    String opcao = lerTexto("1 - Disponível | 2 - Indisponível: ");
    DisponibilidadeMedico valor = switch (opcao) {
      case "1" -> DisponibilidadeMedico.DISPONIVEL;
      case "2" -> DisponibilidadeMedico.INDISPONIVEL;
      default -> throw new RegraNegocioException("Opção inválida.");
    };
    consultaService.alterarDisponibilidadeMedico(medicoId, valor);
    System.out.println("Disponibilidade alterada para " + valor + ".");
  }

  public void agendarConsulta() {
    System.out.println("\n--- Agendar consulta ---");
    Long medicoId = lerLong("ID do médico: ");
    Long pacienteId = lerLong("ID do paciente: ");
    LocalDateTime dataHora = lerDataHora("Data e hora (dd/MM/aaaa HH:mm): ");
    String motivo = lerTexto("Motivo: ");

    Consulta consulta = consultaService.agendarConsulta(medicoId, pacienteId, dataHora, motivo);
    System.out.println("Consulta agendada com sucesso!");
    System.out.println(consulta.getResumo());
  }

  public void listarConsultas() {
    List<Consulta> consultas = consultaService.listarConsultas();
    System.out.println("\n--- Consultas ---");
    if (consultas.isEmpty()) {
      System.out.println("Nenhuma consulta cadastrada.");
    }
    consultas.forEach(c -> System.out.println(c.getResumo()));
  }

  public void alterarStatusConsulta() {
    Long consultaId = lerLong("ID da consulta: ");
    String opcao = lerTexto("1 - Confirmar | 2 - Cancelar | 3 - Finalizar: ");
    switch (opcao) {
      case "1" -> {
        consultaService.confirmarConsulta(consultaId);
        System.out.println("Consulta confirmada.");
      }
      case "2" -> {
        consultaService.cancelarConsulta(consultaId);
        System.out.println("Consulta cancelada. O horário está liberado.");
      }
      case "3" -> {
        consultaService.finalizarConsulta(consultaId);
        System.out.println("Consulta finalizada.");
      }
      default -> throw new RegraNegocioException("Opção inválida.");
    }
  }

  public void encerrar() {
    System.out.println("Encerrando o sistema. Até logo!");
    executando = false;
  }

  // ---------- Leitura de dados ----------

  private String lerTexto(String mensagem) {
    System.out.print(mensagem);
    return scanner.nextLine().trim();
  }

  private Long lerLong(String mensagem) {
    try {
      return Long.parseLong(lerTexto(mensagem));
    } catch (NumberFormatException e) {
      throw new RegraNegocioException("Informe um número inteiro válido.");
    }
  }

  private LocalDate lerData(String mensagem) {
    try {
      return LocalDate.parse(lerTexto(mensagem), FORMATO_DATA);
    } catch (DateTimeParseException e) {
      throw new RegraNegocioException("Data inválida. Use o formato dd/MM/aaaa.");
    }
  }

  private LocalDateTime lerDataHora(String mensagem) {
    try {
      return LocalDateTime.parse(lerTexto(mensagem), FORMATO_DATA_HORA);
    } catch (DateTimeParseException e) {
      throw new RegraNegocioException("Data/hora inválida. Use o formato dd/MM/aaaa HH:mm.");
    }
  }

  private BigDecimal lerValor(String mensagem) {
    try {
      return new BigDecimal(lerTexto(mensagem).replace(",", "."));
    } catch (NumberFormatException e) {
      throw new RegraNegocioException("Valor inválido. Exemplo: 250,00");
    }
  }

  private Especialidade lerEspecialidade() {
    Especialidade[] valores = Especialidade.values();
    for (int i = 0; i < valores.length; i++) {
      System.out.println((i + 1) + " - " + valores[i]);
    }
    try {
      int indice = Integer.parseInt(lerTexto("Especialidade (número): "));
      return valores[indice - 1];
    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
      throw new RegraNegocioException("Especialidade inválida.");
    }
  }
}

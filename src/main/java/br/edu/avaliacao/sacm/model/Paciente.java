package br.edu.avaliacao.sacm.model;

import br.edu.avaliacao.sacm.enums.StatusPessoa;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Paciente extends Pessoa {
  private String numeroProntuario;
  private String convenio;
  private String contatoEmergencia;
  private String telefoneEmergencia;

  // Cadastro novo: o prontuário ainda não existe, é gerado depois do id
  public Paciente(String nome, String cpf, LocalDate dataNascimento,
                  String email, String telefone, String cidade, String estado,
                  String convenio, String contatoEmergencia, String telefoneEmergencia) {
    this(null, nome, cpf, dataNascimento, email, telefone, cidade, estado,
        LocalDateTime.now(), StatusPessoa.ATIVO,
        null, convenio, contatoEmergencia, telefoneEmergencia);
  }

  // Reconstrução a partir do banco
  public Paciente(Long id, String nome, String cpf, LocalDate dataNascimento,
                  String email, String telefone, String cidade, String estado,
                  LocalDateTime dataCadastro, StatusPessoa status,
                  String numeroProntuario, String convenio,
                  String contatoEmergencia, String telefoneEmergencia) {
    super(id, nome, cpf, dataNascimento, email, telefone, cidade, estado,
        dataCadastro, status);
    this.numeroProntuario = numeroProntuario;
    this.convenio = convenio;
    this.contatoEmergencia = contatoEmergencia;
    this.telefoneEmergencia = telefoneEmergencia;
  }

  // Gera PRT-000001 a partir do id. Só funciona uma vez e depois do id existir.
  public void gerarNumeroProntuario() {
    if (getId() == null) {
      throw new IllegalStateException("O paciente ainda não tem id.");
    }
    if (numeroProntuario != null) {
      throw new IllegalStateException("O prontuário já foi gerado.");
    }
    this.numeroProntuario = String.format("PRT-%06d", getId());
  }

  public void atualizarConvenio(String valor) {
    this.convenio = valor;
  }

  public void atualizarContatoEmergencia(String nome, String telefone) {
    this.contatoEmergencia = nome;
    this.telefoneEmergencia = telefone;
  }

  // RN02: só paciente ATIVO pode agendar
  public boolean podeAgendar() {
    return isAtivo();
  }

  @Override
  public String getDescricao() {
    return "Paciente " + getNome() + " - Prontuário " + numeroProntuario;
  }

  public String getNumeroProntuario() {
    return numeroProntuario;
  }

  public String getConvenio() {
    return convenio;
  }

  public String getContatoEmergencia() {
    return contatoEmergencia;
  }

  public String getTelefoneEmergencia() {
    return telefoneEmergencia;
  }

}

package br.edu.avaliacao.sacm.model;

import br.edu.avaliacao.sacm.enums.StatusPessoa;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public abstract class Pessoa {

  private Long id;
  private final String nome;
  private final String cpf;
  private final LocalDate dataNascimento;
  private String email;
  private String telefone;
  private final String cidade;
  private final String estado;
  private final LocalDateTime dataCadastro;
  private StatusPessoa status;

  // Cadastro novo: sem id, data de cadastro agora, status ATIVO
  protected Pessoa(String nome, String cpf, LocalDate dataNascimento,
                   String email, String telefone, String cidade, String estado) {
    this(null, nome, cpf, dataNascimento, email, telefone, cidade, estado,
        LocalDateTime.now(), StatusPessoa.ATIVO);
  }

  // Reconstrução a partir do banco (e base do construtor acima)
  protected Pessoa(Long id, String nome, String cpf, LocalDate dataNascimento,
                   String email, String telefone, String cidade, String estado,
                   LocalDateTime dataCadastro, StatusPessoa status) {
    if (nome == null || nome.isBlank()) {
      throw new RegraNegocioException("O nome é obrigatório.");
    }
    String cpfNumeros = cpf == null ? "" : cpf.replaceAll("\\D", "");
    if (cpfNumeros.length() != 11) {
      throw new RegraNegocioException("CPF inválido: informe 11 dígitos.");
    }
    if (dataNascimento == null || dataNascimento.isAfter(LocalDate.now())) {
      throw new RegraNegocioException("Data de nascimento inválida.");
    }
    this.id = id;
    this.nome = nome.trim();
    this.cpf = cpfNumeros;
    this.dataNascimento = dataNascimento;
    this.email = email;
    this.telefone = telefone;
    this.cidade = cidade;
    this.estado = estado == null ? null : estado.trim().toUpperCase();
    this.dataCadastro = dataCadastro;
    this.status = status;
  }

  // O id só pode ser definido uma vez (quando o banco gera)
  public void definirId(Long id) {
    if (this.id != null) {
      throw new IllegalStateException("O id já foi definido.");
    }
    this.id = id;
  }

  public int getIdade() {
    return Period.between(dataNascimento, LocalDate.now()).getYears();
  }

  public void atualizarContato(String email, String telefone) {
    this.email = email;
    this.telefone = telefone;
  }

  public void ativar() { this.status = StatusPessoa.ATIVO; }

  public void desativar() { this.status = StatusPessoa.INATIVO; }

  public boolean isAtivo() { return status == StatusPessoa.ATIVO; }

  public abstract String getDescricao();


  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getCpf() {
    return cpf;
  }

  public LocalDate getDataNascimento() {
    return dataNascimento;
  }

  public String getEmail() {
    return email;
  }

  public String getTelefone() {
    return telefone;
  }

  public String getCidade() {
    return cidade;
  }

  public String getEstado() {
    return estado;
  }

  public LocalDateTime getDataCadastro() {
    return dataCadastro;
  }

  public StatusPessoa getStatus() {
    return status;
  }
}

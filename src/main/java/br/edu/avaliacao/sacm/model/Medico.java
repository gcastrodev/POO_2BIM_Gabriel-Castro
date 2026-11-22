package br.edu.avaliacao.sacm.model;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.enums.StatusPessoa;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Medico extends Pessoa {
  private final String crm;
  private final Especialidade especialidade;
  private DisponibilidadeMedico disponibilidade;
  private final BigDecimal valorConsulta;

  // Cadastro novo: ATIVO e DISPONIVEL
  public Medico(String nome, String cpf, LocalDate dataNascimento,
                String email, String telefone, String cidade, String estado,
                String crm, Especialidade especialidade, BigDecimal valorConsulta) {
    this(null, nome, cpf, dataNascimento, email, telefone, cidade, estado,
        LocalDateTime.now(), StatusPessoa.ATIVO,
        crm, especialidade, DisponibilidadeMedico.DISPONIVEL, valorConsulta);
  }

  // Reconstrução a partir do banco
  public Medico(Long id, String nome, String cpf, LocalDate dataNascimento,
                String email, String telefone, String cidade, String estado,
                LocalDateTime dataCadastro, StatusPessoa status,
                String crm, Especialidade especialidade,
                DisponibilidadeMedico disponibilidade, BigDecimal valorConsulta) {
    super(id, nome, cpf, dataNascimento, email, telefone, cidade, estado,
        dataCadastro, status);
    if (crm == null || crm.isBlank()) {
      throw new RegraNegocioException("O CRM é obrigatório.");
    }
    if (especialidade == null) {
      throw new RegraNegocioException("A especialidade é obrigatória.");
    }
    if (valorConsulta == null || valorConsulta.compareTo(BigDecimal.ZERO) <= 0) {
      throw new RegraNegocioException("O valor da consulta deve ser maior que zero.");
    }
    this.crm = crm.trim().toUpperCase();
    this.especialidade = especialidade;
    this.disponibilidade = disponibilidade;
    this.valorConsulta = valorConsulta;
  }

  public void alterarDisponibilidade(DisponibilidadeMedico valor) {
    if (valor == null) {
      throw new RegraNegocioException("Informe a disponibilidade.");
    }
    this.disponibilidade = valor;
  }

  public boolean isDisponivel() {
    return disponibilidade == DisponibilidadeMedico.DISPONIVEL;
  }

  // RN02 + RN03: precisa estar ATIVO e DISPONIVEL
  public boolean podeAtender() {
    return isAtivo() && isDisponivel();
  }

  @Override
  public String getDescricao() {
    return "Dr(a). " + getNome() + " - CRM " + crm + " - " + especialidade;
  }

  public String getCrm() {
    return crm;
  }

  public Especialidade getEspecialidade() {
    return especialidade;
  }

  public DisponibilidadeMedico getDisponibilidade() {
    return disponibilidade;
  }

  public BigDecimal getValorConsulta() {
    return valorConsulta;
  }
}

package br.edu.avaliacao.sacm.exception;

public class RegraNegocioException extends RuntimeException {
  public RegraNegocioException(String message) {

    super(message);
  }

  // RN09: preserva a causa técnica (ex.: SQLException) para diagnóstico
  public RegraNegocioException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}


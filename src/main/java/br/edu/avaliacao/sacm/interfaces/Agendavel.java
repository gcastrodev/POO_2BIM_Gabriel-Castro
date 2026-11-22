package br.edu.avaliacao.sacm.interfaces;

import br.edu.avaliacao.sacm.model.Consulta;
import java.time.LocalDateTime;

public interface Agendavel {
  Consulta agendarConsulta(Long medicoId, Long pacienteId,
                           LocalDateTime dataHora, String motivo);

  void cancelarConsulta(Long consultaId);
}

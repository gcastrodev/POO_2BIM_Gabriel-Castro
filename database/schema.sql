CREATE TABLE IF NOT EXISTS pessoa (
                                      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      nome            VARCHAR(120) NOT NULL,
    cpf             CHAR(11)     NOT NULL UNIQUE,
    data_nascimento DATE         NOT NULL,
    email           VARCHAR(120),
    telefone        VARCHAR(20),
    cidade          VARCHAR(80),
    estado          CHAR(2),
    data_cadastro   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(10)  NOT NULL DEFAULT 'ATIVO',
    CONSTRAINT ck_pessoa_status CHECK (status IN ('ATIVO', 'INATIVO'))
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medico (
                                      pessoa_id       BIGINT PRIMARY KEY,
                                      crm             VARCHAR(20)   NOT NULL UNIQUE,
    especialidade   VARCHAR(30)   NOT NULL,
    disponibilidade VARCHAR(15)   NOT NULL DEFAULT 'DISPONIVEL',
    valor_consulta  DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_medico_pessoa FOREIGN KEY (pessoa_id) REFERENCES pessoa (id),
    CONSTRAINT ck_medico_disp CHECK (disponibilidade IN ('DISPONIVEL', 'INDISPONIVEL')),
    CONSTRAINT ck_medico_esp CHECK (especialidade IN
('CLINICO_GERAL', 'CARDIOLOGIA', 'DERMATOLOGIA', 'PEDIATRIA', 'ORTOPEDIA'))
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS paciente (
                                        pessoa_id           BIGINT PRIMARY KEY,
                                        numero_prontuario   VARCHAR(20) NOT NULL UNIQUE,
    convenio            VARCHAR(80),
    contato_emergencia  VARCHAR(120),
    telefone_emergencia VARCHAR(20),
    CONSTRAINT fk_paciente_pessoa FOREIGN KEY (pessoa_id) REFERENCES pessoa (id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS consulta (
                                        id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        medico_id        BIGINT       NOT NULL,
                                        paciente_id      BIGINT       NOT NULL,
                                        data_hora        DATETIME     NOT NULL,
                                        motivo           VARCHAR(255) NOT NULL,
    status           VARCHAR(15)  NOT NULL DEFAULT 'AGENDADA',
    data_agendamento DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacao       VARCHAR(500),
    CONSTRAINT fk_consulta_medico   FOREIGN KEY (medico_id)   REFERENCES medico (pessoa_id),
    CONSTRAINT fk_consulta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (pessoa_id),
    CONSTRAINT ck_consulta_status CHECK (status IN
('AGENDADA', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA')),
    INDEX idx_consulta_medico_data   (medico_id, data_hora),
    INDEX idx_consulta_paciente_data (paciente_id, data_hora)
    ) ENGINE=InnoDB;
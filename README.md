# SACM - Sistema de Agendamento de Consultas Médicas

Projeto da avaliação do 2º bimestre de Programação Orientada a Objetos.
Aplicação de console em Java 21 com Spring Boot (Spring JDBC) e MariaDB.

- **Integrante:** Gabriel Castro
- **Turma:** Engenharia de Software
- **SGBD:** MariaDB (uso autorizado pelo professor)

## Requisitos

- JDK 21
- MariaDB 13.0.2 em execução local
- Maven Wrapper (já incluso: `./mvnw`)

## Configuração do banco

1. Crie o schema e o usuário no MariaDB:

```sql
CREATE DATABASE sacm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'sacm_user'@'localhost' IDENTIFIED BY '<sua_senha>';
GRANT ALL PRIVILEGES ON sacm_db.* TO 'sacm_user'@'localhost';
FLUSH PRIVILEGES;
```

2. Crie as tabelas:

```bash
mariadb -u sacm_user -p sacm_db < database/schema.sql
```

## Credenciais

As credenciais **não ficam no código**. O `application.properties` lê duas
variáveis de ambiente:

```bash
export DB_USER=sacm_user
export DB_PASSWORD='<sua_senha>'
```

No IntelliJ, defina as mesmas variáveis em *Run > Edit Configurations >
Environment variables*.

## Execução

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

O menu do sistema aparece no terminal.

## Menu

| Opção | Ação |
|---|---|
| 1 | Cadastrar médico |
| 2 | Cadastrar paciente (prontuário gerado automaticamente, `PRT-000001`) |
| 3 | Listar médicos e pacientes |
| 4 | Alterar disponibilidade do médico |
| 5 | Agendar consulta |
| 6 | Listar consultas |
| 7 | Confirmar, cancelar ou finalizar consulta |
| 0 | Encerrar |

Médicos e pacientes compartilham a mesma sequência de IDs (ambos são `pessoa`).
Use a opção 3 para consultar os IDs.

## Estrutura

```
src/main/java/br/edu/avaliacao/sacm/
├── model        Pessoa (abstrata), Medico, Paciente, Consulta
├── enums        StatusPessoa, DisponibilidadeMedico, Especialidade, StatusConsulta
├── interfaces   Agendavel
├── repository   MedicoRepository, PacienteRepository, ConsultaRepository (JdbcTemplate)
├── service      ConsultaService (regras RN01 a RN07)
├── exception    RegraNegocioException, ConflitoHorarioException
└── console      SistemaConsole
database/schema.sql
```

## Roteiro de demonstração

1. Cadastrar dois médicos e dois pacientes (opções 1 e 2).
2. Agendar uma consulta válida (opção 5).
3. Tentar agendar o mesmo horário para o mesmo médico (conflito de médico).
4. Tentar agendar o mesmo horário para o mesmo paciente com outro médico (conflito de paciente).
5. Marcar um médico como indisponível e tentar agendar (opção 4, depois 5).
6. Cancelar a consulta (opção 7) e agendar de novo no horário liberado (opção 5).
7. Encerrar e reiniciar o sistema; as consultas continuam salvas (opção 6).

Para conferir a persistência no banco:

```sql
SELECT id, medico_id, paciente_id, data_hora, status, motivo
FROM consulta ORDER BY id;
```

## Uso de IA

Declarado na seção 9 do relatório técnico.
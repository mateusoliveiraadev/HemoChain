# Screencast do código — roteiro para gravação

Duração sugerida: 7–9 minutos. Abrir os arquivos reais no editor, com fonte legível e sem segredos. Gravar áudio ou legenda. YouTube permanece pendente da equipe.

| Tempo | Arquivo/ação | Explicação acessível |
|---|---|---|
| 0:00–0:40 | Árvore `src/main/java/com/hemochain` e POM | “Mantivemos Java 17 e Spring Boot, separados em entidades, repositórios, serviços e controllers. As telas estáticas chamam a API; PostgreSQL é usado no container e H2 no desenvolvimento.” |
| 0:40–1:30 | `EventoBolsa`, `TipoEventoBolsa`, `EventoBolsaRepository` | “Cada evento aponta para uma bolsa e guarda tipo, instante UTC, origem e detalhes. Não há setters nem métodos de update/delete no repositório. Os callbacks recusam alterações pelo JPA.” |
| 1:30–2:10 | `RastreabilidadeService`, `BolsaService`, `ValidadeService` | “O evento só pode ser inserido dentro de uma transação existente. Cadastro, edição e descarte gravam o histórico junto da mudança. A edição preserva antes e depois. Um erro no evento reverte a operação.” |
| 2:10–2:50 | `ProtecaoHistoricoPostgres`, `EvolucaoStatusBolsaH2` | “A proteção também existe no PostgreSQL por gatilho contra alteração, exclusão e truncate. A inicialização atualiza a restrição dos status antigos sem apagar bolsas. Isso não impede um administrador de remover a proteção e não é blockchain.” |
| 2:50–3:40 | `EntregaHospitalar`, `RequisicaoHospitalar`, enums de estado | “A requisição já existia, mas ainda não tinha vínculo com bolsas. A nova entrega representa uma atestação manual de algo ocorrido, associando uma bolsa e uma requisição. Uma requisição de várias bolsas pode ficar parcialmente atendida.” |
| 3:40–4:50 | `AtendimentoHospitalarService` | “O serviço trava a requisição e a bolsa para evitar duas operações concorrentes. Reaproveita a compatibilidade de hemácias, limita quantidade, valida hospital, condição e validade. Não conformidade bloqueia uso. Confirmação e utilização repetidas geram conflito.” |
| 4:50–5:25 | `verificarPendencias`, `VerificacaoRecebimentoAgendada`, propriedades | “Cada entrega recebe seu prazo. O verificador encontra as atrasadas, reavalia sob trava e grava um único alerta e evento. Ele roda ao iniciar, periodicamente e na consulta. O prazo padrão é 24 horas.” |
| 5:25–6:00 | Controllers e DTOs | “O gestor atesta entrega; o hospital correspondente confirma e registra uso. Os DTOs aceitam apenas os campos necessários. Status, momento e origem são definidos no servidor. O contexto por cabeçalho é didático e não substitui autenticação robusta.” |
| 6:00–6:40 | `estoque.html`, `requisicoes.html`, `atendimento.html` | “As telas foram conectadas mantendo a identidade visual. O gestor acompanha histórico e pendências; o hospital confirma condição e uso. A sugestão FEFO continua manual; não há reserva automática.” |
| 6:40–7:40 | Testes de aceitação, rollback e PostgreSQL | “Os cenários BDD chamam a API sem uma transação externa escondendo problemas de commit. Testamos hospital incorreto, duplicidade, vencimento, parcial e não conformidade. Provocamos falha de auditoria para provar rollback. No PostgreSQL testamos SQL direto, migração e concorrência.” |
| 7:40–8:20 | Executar `mvn verify` ou abrir log real; CI/CD | “A homologação registra o comando, o ambiente e o resultado efetivamente executado. O job PostgreSQL complementa a suíte padrão e bloqueia o avanço antes da publicação. Push de branch não dispara deploy de produção; ele está restrito à main.” |
| 8:20–8:45 | README e relatório | “A documentação distingue o que foi validado e o que depende da equipe, incluindo publicação dos vídeos. O projeto permanece um protótipo acadêmico e o apoio de IA está declarado.” |

Antes de publicar, reproduzir o arquivo inteiro, conferir áudio/legendas e legibilidade. Não apresentar logs antigos como resultado do HEAD atual. Adicionar ao README somente a URL real após publicação autorizada e conferência de acesso.

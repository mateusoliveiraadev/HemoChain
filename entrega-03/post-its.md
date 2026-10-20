# POST-IT — Entrega 03

## US01 — Cadastro e controle de estoque de hemocomponentes

**Como** gestor de estoque do hemocentro,
**Eu quero** cadastrar, consultar e atualizar bolsas de hemocomponentes,
**Para que** eu acompanhe o estoque disponível e suas datas de validade.

### Funcionalidades implementadas

- Identificação única por bolsa, normalizada em maiúsculas; bloqueio de duplicidades no serviço e no H2.
- Tipo de componente, ABO/Rh, volume em mL e data de coleta.
- Validade calculada no servidor, com tabela configurável em `application.properties`.
- Consulta, edição dos campos permitidos e filtros de status.
- Validação dos campos obrigatórios, volume de 1 a 10.000 mL e coleta não futura.
- Persistência H2 em arquivo e tela integrada ao Spring Boot.
- Somente o perfil gestor acessa os endpoints de estoque, seguindo o padrão anterior.

### Limitações reais

- Uma identificação representa uma bolsa; não há agrupamento de várias bolsas por lote.
- Os prazos são exclusivamente didáticos, não regras clínicas oficiais.
- Não há FEFO, reserva, compatibilidade transfusional ou transporte.
- Bolsas descartadas não podem ser editadas nem excluídas pela API.
- O acesso continua usando o cabeçalho didático `X-Perfil`, sem sessão/token; não é autenticação robusta.

## US07 — Alertas de vencimento e descarte controlado

**Como** gestor de estoque,
**Eu quero** visualizar alertas de bolsas próximas do vencimento e o descarte automático das vencidas,
**Para que** eu priorize sua utilização e mantenha o registro do descarte.

### Funcionalidades implementadas

- Alertas persistentes e limiares configuráveis, inicialmente 5 e 2 dias.
- Destaque de bolsas prioritárias na tela e consulta dos alertas ativos.
- Verificação na inicialização, diariamente à meia-noite, nas consultas e ao cadastrar/editar.
- Descarte quando a validade é igual ou anterior à data atual.
- Exclusão das vencidas da consulta de disponíveis, preservando os registros.
- Histórico mínimo na própria bolsa: data/hora, motivo e responsável `SISTEMA`.
- Proteção transacional e restrições no H2 contra duplicações.
- Alertas são recalculados após edição; alertas de descartadas deixam de aparecer como ativos.

### Limitações reais

- A notificação ocorre no painel local, sem e-mail ou serviço externo.
- A aplicação precisa estar em execução para o agendamento; ao reiniciar, verifica pendências.
- As datas usam o fuso local do servidor, sem precisão de horário para a validade.
- Não há alocação de bolsas; a exclusão é da lista de disponíveis, sem implementar US03.
- O histórico cobre somente descarte, sem representar a rastreabilidade completa de US09.

## Participação de IA

Implementação e testes preparados com apoio do Codex. O grupo deve revisar, compreender o código e adequar a declaração às regras da disciplina antes de apresentar.

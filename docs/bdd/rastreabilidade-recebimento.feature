# language: pt
# Executados em AtendimentoHospitalarAceitacaoTest e HistoricoPostgresIT (SQL real).
Funcionalidade: Rastreabilidade e recebimento hospitalar (US09 e US10)
  Cenario: Consulta do historico
    Dado que uma bolsa passou por diferentes estados
    Quando o gestor consultar seu historico
    Entao os eventos registrados aparecem em ordem cronologica
    E cada evento informa data/hora UTC e responsavel ou origem

  Cenario: Protecao dos registros
    Dado que um evento foi registrado
    Quando alguem tentar modifica-lo ou exclui-lo
    Entao a operacao sera bloqueada
    E o registro original sera preservado

  Cenario: Bolsa anterior a implementacao
    Dado uma bolsa legada sem eventos
    Quando o gestor consultar seu historico
    Entao a lista estara vazia e a interface explicara a ausencia de historico retroativo

  Cenario: Recebimento integro
    Dado que uma bolsa compativel foi entregue ao hospital solicitante
    E o gestor atestou a entrega ocorrida associando bolsa e requisicao
    Quando o hospital autorizado confirmar recebimento com condicao integra
    Entao a requisicao de uma bolsa sera atualizada para RECEBIDA
    E a bolsa ficara apta ao registro de utilizacao enquanto valida

  Cenario: Confirmacao pendente
    Dado que uma bolsa entregue ultrapassou o prazo configurado de confirmacao
    Quando a verificacao de pendencias for executada
    Entao o sistema gerara um alerta persistido uma unica vez
    E sinalizara a requisicao como RECEBIMENTO_PENDENTE
    E registrara o evento no historico da bolsa

  Cenario: Hospital diferente
    Dado uma entrega pertencente ao hospital A
    Quando o hospital B tentar consultar, confirmar ou utilizar a bolsa
    Entao o acesso sera negado sem alterar status nem historico

  Cenario: Duplicidade ou uso antes de recebimento
    Quando houver uma confirmacao repetida ou tentativa de uso antes de recebimento integro
    Entao a operacao sera recusada com conflito sem duplicar eventos

  Cenario: Nao conformidade
    Quando o hospital confirmar recebimento nao conforme com observacao
    Entao a bolsa ficara BLOQUEADA e a requisicao NAO_CONFORME
    E a utilizacao sera recusada

  Cenario: Atendimento parcial
    Dado uma requisicao de duas bolsas
    Quando apenas uma bolsa for recebida ou utilizada
    Entao a requisicao indicara estado parcial ate o atendimento das duas bolsas

  Cenario: Falha ao registrar evento
    Quando a persistencia do evento falhar durante uma transicao
    Entao a transacao revertera a mudanca da bolsa e da entrega

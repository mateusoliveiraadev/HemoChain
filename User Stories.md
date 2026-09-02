**Histórias de Usuário - HemoChain**

**US01- Cadastro e controle de estoque de hemocomponentes:**

Como gestor de estoque do hemocentro, Eu quero cadastrar e atualizar os lotes de hemocomponentes (tipo, ABO/Rh, volume, data de coleta e validade), Para que eu tenha visibilidade em tempo real do que está disponível e evite desabastecimento ou descarte por vencimento.

**Detalhes de negócio:** 

Cada bolsa cadastrada deve conter tipo do componente (concentrado de hemácias, plasma, plaquetas etc.), tipagem ABO/Rh, data de coleta, data de validade calculada automaticamente conforme o tipo de componente, e status (disponível, reservado, em trânsito, descartado). O sistema deve impedir cadastro duplicado do mesmo lote e sinalizar inconsistências de dados obrigatórios.

**Cenários de validação (BDD):**

**Cenário 1**: 
Cadastro de novo lote com dados válidos
Dado que estou autenticado como gestor de estoque
Quando eu cadastro uma bolsa com tipo "Concentrado de Hemácias", ABO "O-", data de coleta "01/08/2026"
Então o sistema calcula automaticamente a data de validade
E o lote é listado no estoque com status "Disponível"

**Cenário 2:**
Tentativa de cadastro com dado obrigatório ausente
Dado que estou cadastrando uma nova bolsa
Quando eu não informo a tipagem ABO/Rh
Então o sistema bloqueia o cadastro
E exibe mensagem indicando o campo obrigatório faltante

**US02- Registro de requisição hospitalar:** 

Como hospital solicitante, Eu quero registrar uma requisição de hemocomponente informando tipo, quantidade, ABO/Rh do paciente e prazo necessário, Para que o hemocentro processe meu pedido com prioridade e prazo corretos.

**Detalhes de negócio:**

A requisição deve conter identificador do hospital, tipo de componente, quantidade, compatibilidade ABO/Rh necessária, nível de urgência (rotina, urgente, emergencial) e prazo limite de entrega. Requisições emergenciais entram automaticamente em fila de prioridade máxima.

**Cenários de validação (BDD):**

**Cenário 1:**
Registro de requisição emergencial
Dado que sou um hospital cadastrado no sistema
Quando eu registro uma requisição com urgência "Emergencial" para tipo "A+"
Então a requisição é criada com prioridade máxima
E o gestor de estoque recebe uma notificação imediata

**Cenário 2:**
Requisição com quantidade indisponível no estoque
Dado que o estoque possui apenas 2 bolsas do tipo solicitado
Quando eu solicito 5 bolsas desse tipo
Então o sistema registra a requisição como "Parcialmente atendível"
E sugere hemocentros parceiros com estoque complementar


**US03- Alocação automática de bolsas compatíveis (FEFO):**

Como sistema de gestão de estoque, Eu quero alocar automaticamente as bolsas compatíveis com a requisição priorizando as mais próximas do vencimento (FEFO — First Expired, First Out), Para que se reduza o descarte por validade vencida e se garanta compatibilidade segura ao paciente.

**Detalhes de negócio:** 

O algoritmo de compatibilidade deve seguir as regras didáticas de compatibilidade ABO/Rh (ex.: O- é doador universal, AB+ é receptor universal). Entre as bolsas compatíveis disponíveis, o sistema deve sempre priorizar a de menor prazo de validade restante antes de considerar outros critérios, como proximidade de localização.

**Cenários de validação (BDD):** 

**Cenário 1:**
Alocação priorizando bolsa com validade mais próxima
Dado que existem duas bolsas compatíveis com a requisição, uma vencendo em 3 dias e outra em 10 dias
Quando o sistema realiza a alocação automática
Então a bolsa que vence em 3 dias é selecionada primeiro

**Cenário 2:**
Nenhuma bolsa compatível disponível
Dado que não há bolsas compatíveis com o tipo sanguíneo solicitado
Quando o sistema tenta alocar a requisição
Então a requisição é marcada como "Aguardando estoque compatível"
E o gestor é notificado para acionar hemocentros parceiros

**US04- Cálculo de rota de distribuição:**

Como operador de logística, Eu quero que o sistema calcule a rota de menor custo (tempo/distância) entre o hemocentro e o hospital solicitante, Para que a entrega respeite a janela de tempo exigida e minimize o risco de ruptura da cadeia de frio.


**Detalhes de negócio:**

A malha de distribuição é modelada como um grafo, onde os nós representam hemocentros, pontos de apoio e hospitais, e as arestas representam trajetos com peso (tempo estimado). O sistema deve calcular o caminho mínimo respeitando restrições de janela de tempo de entrega e evitando trajetos com tempo estimado superior ao limite de estabilidade térmica do componente transportado.

**Cenários de validação (BDD):**

**Cenário 1:**
Cálculo de rota dentro da janela de tempo permitida
Dado que a requisição possui prazo máximo de entrega de 2 horas
Quando o sistema calcula a rota entre o hemocentro e o hospital
Então a rota de menor tempo é selecionada
E o tempo estimado de entrega é exibido ao operador

**Cenário 2:**
Rota inviável dentro do limite de estabilidade térmica
Dado que o componente exige transporte em no máximo 90 minutos
Quando todas as rotas disponíveis excedem esse tempo
Então o sistema alerta que não há rota viável
E sugere um ponto de apoio intermediário para reabastecimento térmico


**US05- Monitoramento de temperatura durante o transporte:**

Como operador de logística, Eu quero acompanhar em tempo real a temperatura simulada da bolsa durante o transporte, Para que eu possa agir rapidamente caso a temperatura saia da faixa segura e evitar perda do componente.

**Detalhes de negócio:**

Cada componente possui uma faixa de temperatura ideal (ex.: concentrado de hemácias entre 2°C e 6°C). O sistema recebe leituras simuladas de telemetria periodicamente durante o trajeto e compara com a faixa de referência do tipo de componente transportado.

**Cenários de validação (BDD):**

**Cenário 1:**
Temperatura dentro da faixa ideal
Dado que uma bolsa de concentrado de hemácias está em trânsito
Quando a leitura de temperatura simulada indica 4°C
Então o status da entrega permanece "Normal"

**Cenário 2:**
Temperatura fora da faixa de segurança
Dado que uma bolsa de concentrado de hemácias está em trânsito
Quando a leitura de temperatura simulada indica 9°C
Então o sistema gera um alerta crítico
E notifica o operador de logística e o hospital destinatário


**US06- Painel de indicadores de estoque e demanda:**

Como gestor do hemocentro, Eu quero visualizar um painel com indicadores de estoque atual, demanda histórica e projeção de desabastecimento por tipo sanguíneo, Para que eu possa antecipar decisões de captação de doadores e redistribuição entre unidades.

**Detalhes de negócio:**

O painel deve exibir estatística descritiva (média e variação de consumo por tipo sanguíneo em período configurável) e uma projeção simples baseada em probabilidade histórica de demanda, sinalizando tipos sanguíneos em risco de ruptura de estoque nos próximos dias.

**Cenários de validação (BDD):**

**Cenário 1:**
Visualização de tipo sanguíneo em risco de desabastecimento
Dado que o consumo médio diário do tipo "O-" é maior que o estoque atual disponível
Quando o gestor acessa o painel de indicadores
Então o tipo "O-" é destacado com alerta de "Risco de desabastecimento"

**Cenário 2:**
Consulta de histórico de demanda por período
Dado que existem dados de requisições dos últimos 30 dias
Quando o gestor filtra o painel pelo período "Últimos 30 dias"
Então o sistema exibe o gráfico de consumo por tipo sanguíneo nesse intervalo

**US07- Alerta de vencimento próximo e descarte controlado:**

Como gestor de estoque, Eu quero receber alertas automáticos de bolsas próximas do vencimento, Para que eu possa priorizar sua utilização ou redistribuição antes do descarte, reduzindo o desperdício.

**Detalhes de negócio:**

O sistema deve verificar diariamente a validade de todas as bolsas em estoque e gerar alertas configuráveis (ex.: 5 dias, 2 dias antes do vencimento). Bolsas vencidas devem ser automaticamente movidas para status "Descartado" e removidas da lista de alocação disponível, mantendo o registro para fins de auditoria.

**Cenários de validação (BBD):**

**Cenário 1:**
Alerta de bolsa próxima do vencimento
Dado que uma bolsa vence em 2 dias
Quando o sistema executa a verificação diária de validade
Então um alerta é enviado ao gestor de estoque
E a bolsa é sinalizada como "Prioritária para uso"


**Cenário 2:**
Bolsa vencida é automaticamente descartada
Dado que uma bolsa atingiu sua data de validade sem ter sido utilizada
Quando o sistema executa a verificação diária de validade
Então o status da bolsa é alterado para "Descartado"
E a bolsa deixa de aparecer na lista de alocação disponível
E o evento é registrado no histórico de auditoria

**US08- Controle de acesso por perfil de usuário:**

Como administrador do sistema, Eu quero definir perfis de acesso distintos para gestores de hemocentro, operadores de logística e hospitais solicitantes, Para que cada usuário visualize e execute apenas as ações compatíveis com sua função, garantindo segurança e conformidade com a LGPD.

**Detalhes de negócio:**

O sistema deve utilizar apenas dados sintéticos, sem informações reais de doadores ou pacientes. Cada perfil possui permissões específicas: hospitais só visualizam e criam suas próprias requisições; operadores de logística visualizam rotas e telemetria; gestores têm acesso completo ao estoque e aos indicadores.

**Cenários de validação (BDD):**

**Cenário 1:**
Hospital tenta acessar o estoque completo do hemocentro
Dado que estou autenticado com perfil "Hospital"
Quando eu tento acessar a tela de gestão de estoque completo
Então o acesso é negado
E sou redirecionado para minha área de requisições

**Cenário 2:**
Gestor acessa todas as funcionalidades do sistema
Dado que estou autenticado com perfil "Gestor de Estoque"
Quando eu acesso o menu principal do sistema
Então tenho acesso às telas de estoque, indicadores, requisições e rotas

**US09- Rastreabilidade completa da bolsa (chain of custody):**

Como gestor de estoque do hemocentro, Eu quero que o sistema registre automaticamente todos os eventos pelos quais uma bolsa passa (coleta, cadastro, reserva, alocação, transporte, entrega e uso/descarte), Para que eu tenha um histórico auditável de ponta a ponta e possa responder rapidamente a qualquer questionamento sobre a origem e o destino de um hemocomponente.

**Detalhes de negócio:**

Cada mudança de status da bolsa (disponível, reservado, em trânsito, entregue, descartado etc.) deve gerar um registro imutável de evento, contendo data/hora, usuário ou processo responsável pela mudança e o status anterior/novo. O histórico completo de uma bolsa deve poder ser consultado a partir do seu identificador único, do cadastro até o desfecho final (uso ou descarte), sem possibilidade de edição ou exclusão dos registros já gravados.

**Cenários de validação (BDD):**

**Cenário 1:**
Consulta do histórico completo de uma bolsa
Dado que uma bolsa já passou pelos status "Disponível", "Reservado", "Em trânsito" e "Entregue"
Quando o gestor consulta o histórico dessa bolsa pelo identificador
Então o sistema exibe todos os eventos em ordem cronológica, com data/hora e responsável por cada mudança

**Cenário 2:**
Tentativa de alteração de um evento já registrado
Dado que existe um evento de mudança de status já gravado no histórico de uma bolsa
Quando qualquer usuário tenta editar ou excluir esse evento
Então o sistema bloqueia a operação
E mantém o registro original inalterado

**US10- Confirmação de recebimento e uso pela unidade hospitalar:**

Como hospital solicitante, Eu quero confirmar o recebimento da bolsa entregue e registrar se ela foi efetivamente utilizada na transfusão, Para que o hemocentro tenha visibilidade do desfecho de cada requisição e possa fechar corretamente o ciclo de rastreabilidade do estoque.

**Detalhes de negócio:**

Ao receber a entrega, o hospital deve confirmar o recebimento informando data/hora e condição da bolsa (íntegra ou com alguma não conformidade, como violação de lacre ou temperatura fora da faixa). Posteriormente, o hospital deve registrar o desfecho final: "Utilizada em transfusão" ou "Não utilizada" (com motivo, ex.: cancelamento do procedimento). Requisições sem confirmação de recebimento dentro de um prazo configurável devem gerar alerta automático para o operador de logística.

**Cenários de validação (BDD):**

**Cenário 1:**
Confirmação de recebimento sem não conformidades
Dado que uma bolsa foi entregue ao hospital solicitante
Quando o hospital confirma o recebimento informando condição "Íntegra"
Então o status da requisição é atualizado para "Recebido"
E a bolsa fica disponível para registro de uso na transfusão

**Cenário 2:**
Ausência de confirmação de recebimento dentro do prazo
Dado que uma bolsa foi entregue há mais tempo do que o prazo configurado para confirmação
Quando o sistema executa a verificação periódica de pendências
Então um alerta é gerado para o operador de logística
E a requisição é sinalizada como "Recebimento pendente"


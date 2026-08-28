Histórias de Usuário - HemoChain

**US01- Cadastro e controle de estoque de hemocomponentes:**

Como gestor de estoque do hemocentro, Eu quero cadastrar e atualizar os lotes de hemocomponentes (tipo, ABO/Rh, volume, data de coleta e validade), Para que eu tenha visibilidade em tempo real do que está disponível e evite desabastecimento ou descarte por vencimento.

**Detalhes de negócio:** 

Cada bolsa cadastrada deve conter tipo do componente (concentrado de hemácias, plasma, plaquetas etc.), tipagem ABO/Rh, data de coleta, data de validade calculada automaticamente conforme o tipo de componente, e status (disponível, reservado, em trânsito, descartado). O sistema deve impedir cadastro duplicado do mesmo lote e sinalizar inconsistências de dados obrigatórios.

**Cenário de validação (BDD):**

Cenário: Cadastro de novo lote com dados válidos
Dado que estou autenticado como gestor de estoque
Quando eu cadastro uma bolsa com tipo "Concentrado de Hemácias", ABO "O-", data de coleta "01/08/2026"
Então o sistema calcula automaticamente a data de validade
E o lote é listado no estoque com status "Disponível"

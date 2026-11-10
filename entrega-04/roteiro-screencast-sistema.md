# Screencast do sistema — roteiro para gravação

Duração sugerida: 6–8 minutos. Gravar a aplicação real em 1440×1000 ou 1920×1080. Usar áudio em português ou legendas completas. Publicação no YouTube e URL no README permanecem pendentes da equipe; nenhum link foi inventado.

## Preparação

1. Subir ambiente exclusivo com PostgreSQL por Compose; conferir os dois serviços saudáveis e `/actuator/health` UP. Não usar o banco de produção.
2. Cadastrar contas sintéticas de gestor e hospital pela tela de login, em perfis de navegador separados. Nome sugerido: Gestor Demo e Hospital Demo. Nenhum dado de paciente.
3. Para demonstrar atraso em segundos, somente nesta base de demonstração, definir `RECEBIMENTO_PRAZO=PT5S` e `RECEBIMENTO_VERIFICACAO_MS=3600000` antes de iniciar. Explicar em voz alta que o padrão é 24 horas e a redução é didática. Novas entregas recebem esse prazo; as antigas conservam o original.
4. Preparar duas bolsas de hemácias O− com códigos únicos: uma coletada hoje e outra ontem, volume 450 mL. Datas e códigos devem ser anotados no roteiro de tomada. A mais antiga deve aparecer antes pelo FEFO.

## Sequência de telas, ações e falas

| Tempo | Tela e ação real | Fala sugerida |
|---|---|---|
| 0:00–0:30 | Menu do gestor | “O HemoChain é um protótipo acadêmico para acompanhar estoque de hemocomponentes, validade e atendimento hospitalar. Vamos seguir a jornada de uma bolsa com dados sintéticos.” |
| 0:30–1:15 | Estoque: cadastrar bolsa O−, coleta e volume; consultar | “A validade é calculada no servidor com valores didáticos. O cadastro já gera o primeiro evento de rastreabilidade. O estoque e os alertas das entregas anteriores continuam integrados.” |
| 1:15–2:00 | Perfil hospital: cadastrar Hospital Demo, selecionar contexto, criar requisição de 1 hemácia A− | “O hospital declara seu contexto de atendimento e registra tipo, quantidade, urgência e prazo. O acesso continua didático, sem autenticação de produção.” |
| 2:00–2:45 | Gestor: Entregas e recebimento, selecionar requisição, examinar sugestão | “A US03 sugere hemácias ABO/Rh compatíveis em ordem de validade. Esta é uma escolha manual. A reserva automática ainda está pendente.” |
| 2:45–3:15 | Selecionar bolsa, marcar atestação e registrar entrega ocorrida | “Registramos uma entrega já ocorrida nesta demonstração sintética. O sistema associa a bolsa à requisição e remove sua disponibilidade. Não estamos simulando um módulo de transporte.” |
| 3:15–4:00 | Hospital: abrir a mesma requisição, condição Íntegra, confirmar | “Somente o hospital correspondente ao contexto da requisição confirma o recebimento. A condição e o momento são persistidos. A requisição passa a recebida e a bolsa fica apta ao registro de uso enquanto válida.” |
| 4:00–4:30 | Registrar utilização | “Agora o hospital registra a utilização. O sistema bloqueia uso antes de recebimento íntegro, uso repetido e bolsa vencida. Não armazenamos dados de paciente.” |
| 4:30–5:15 | Gestor: atualizar e consultar histórico final | “Aqui vemos cadastro, entrega atestada, recebimento e uso em ordem cronológica, com data, origem e detalhes. Esses registros não possuem edição ou exclusão pela API, e o PostgreSQL também bloqueia alteração direta.” |
| 5:15–6:15 | Segunda requisição e bolsa; registrar entrega, aguardar prazo reduzido; Verificar pendências | “Esta entrega não foi confirmada. Ultrapassado o prazo didático de cinco segundos, a verificação gera um alerta e marca recebimento pendente. O padrão de configuração é 24 horas. O alerta não é duplicado ao verificar novamente.” |
| 6:15–6:45 | Opcional: confirmar tardiamente e atualizar alertas | “A confirmação resolve a pendência ativa, mas o alerta registrado e seu evento permanecem no histórico.” |
| 6:45–7:15 | Retornar ao menu | “O resultado é um MVP integrado. Ainda faltam reserva automática, transporte, autenticação robusta e validação clínica. Este protótipo não está homologado para uso hospitalar real.” |

## Conferência da gravação e publicação

- Verificar que os status mudaram de verdade, os eventos apareceram e não há tela de erro omitida.
- Mostrar somente dados sintéticos; ocultar endereços pessoais, credenciais e outras abas.
- Ouvir o áudio ou revisar cada legenda. Reproduzir o arquivo inteiro antes de publicar.
- Publicar no YouTube pela conta autorizada da equipe com acesso compatível com a disciplina; verificar acesso em janela sem login.
- Inserir o link real na seção 3 da Entrega 04 no README e registrar URL/data no relatório.
- Capturas reproduzíveis são geradas pelo script `scripts/entrega04-ui.cjs`; elas não substituem o vídeo exigido.

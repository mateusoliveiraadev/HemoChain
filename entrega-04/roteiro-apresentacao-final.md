# Apresentação final — HemoChain integrado

Tempo sugerido: 10 minutos. Usar a mesma base sintética e a sequência contínua do screencast do sistema.

1. **Problema (1 min).** “Estoques, requisições e confirmações separados dificultam acompanhar validade e o desfecho de uma bolsa. O HemoChain reúne essas informações num protótipo funcional.” Não usar números de impacto não medidos.
2. **Tecnologias (30 s).** Java 17, Spring Boot, JPA, PostgreSQL, interface HTML/JavaScript, Docker Compose, testes Maven e GitHub Actions; Render como destino de deploy existente. Explicar cada tecnologia pela função.
3. **Estoque e validade (1 min).** Login gestor; cadastrar bolsa sintética, mostrar validade calculada, lista e histórico inicial. Explicar alertas e descarte preservando o registro. Os prazos são didáticos.
4. **Requisição (1 min).** No perfil hospital, escolher contexto e solicitar uma hemácia A−, com quantidade, urgência e prazo. Não mostrar um pedido preexistente como se tivesse sido criado durante a apresentação.
5. **Compatibilidade e associação (1 min).** Gestor vê O− e A− compatíveis em FEFO. Seleciona manualmente e atesta entrega já ocorrida no cenário sintético. A reserva automática da US03 continua pendente. US04/US05/US06 não foram implementadas nesta entrega.
6. **Recebimento e uso (1 min 30 s).** Hospital da requisição confirma condição íntegra e registra uso. Mostrar estado RECEBIDA e depois UTILIZADA. Explicar bloqueios de duplicidade, hospital diferente, bolsa vencida e não conformidade.
7. **Rastreabilidade (1 min).** Gestor consulta o histórico final em ordem cronológica, com momento e origem. Demonstrar que não existe ação de edição/exclusão. A proteção SQL tem teste real, mas não é necessário executar SQL durante a apresentação.
8. **Pendência (1 min).** Uma segunda entrega sintética sem confirmação ultrapassa o prazo reduzido da base de demonstração; executar Verificar pendências e mostrar alerta/status. Informar o padrão de 24 horas. Não alterar datas diretamente no banco para simular sucesso.
9. **Qualidade e benefícios (1 min).** Apresentar resultado real da suíte, testes PostgreSQL, healthchecks e evidência de persistência após reinício. Benefícios esperados: consulta integrada, menor retrabalho de acompanhamento, visibilidade do desfecho e preservação do histórico; sem afirmar ganho quantitativo medido.
10. **Limites e próximos passos (1 min).** Protótipo acadêmico, sem homologação hospitalar real; autenticação por cabeçalhos didáticos, sem prova criptográfica, sem transporte/temperatura, sem reserva automática; fluxo de associação hospitalar limitado a hemácias. Prioridades futuras dependem de revisão de domínio, segurança e validação clínica.

## Divisão sugerida entre integrantes

Uma pessoa apresenta contexto/estoque; outra demonstra requisição/recebimento/uso; outra explica histórico, testes e limitações. Ensaiar a troca de perfis e ter o roteiro aberto em outro dispositivo. Atribuições nominais ficam a cargo da equipe.

## Plano de contingência honesto

Se a URL Render estiver dormindo ou indisponível, usar a instância local Compose validada e explicar a mudança. Se ocorrer erro real, mostrar a mensagem e recorrer às capturas identificadas como evidências anteriores, sem fingir uma execução ao vivo. Não usar produção para demonstração com configuração de cinco segundos.

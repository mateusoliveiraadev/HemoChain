# language: pt
# Aceitacao da US03 (parte 1): compatibilidade ABO/Rh de hemacias + FEFO.
# Implementado em src/test/java/com/hemochain/controller/CompatibilidadeAceitacaoTest.java
# Regras DIDATICAS, nao clinicas oficiais.
Funcionalidade: Sugerir bolsas compativeis para um receptor
  Como gestor de estoque
  Eu quero consultar as bolsas de hemacias compativeis com o tipo sanguineo do receptor
  Para que a bolsa mais proxima do vencimento seja usada primeiro, com seguranca

  Cenario: Sugere so compativeis, mais proxima do vencimento primeiro
    Dado hemacias O- (vence em 5 dias), A- (vence em 35 dias), A+ e B- disponiveis
    Quando o gestor consulta as bolsas compativeis para um receptor A-
    Entao a lista traz O- e depois A-
    E nao traz A+ nem B-

  Cenario: Bolsa vencida nao e sugerida
    Dado uma bolsa O- vencida e outra O- valida
    Quando o gestor consulta as bolsas compativeis para um receptor O+
    Entao somente a bolsa valida aparece

  Cenario: Apenas o gestor consulta
    Quando um usuario sem o perfil de gestor consulta as bolsas compativeis
    Entao o acesso e negado (403)

  Cenario: Receptor invalido ou ausente
    Quando o gestor consulta com um tipo sanguineo desconhecido ou sem informar o tipo
    Entao a resposta e um erro de requisicao (400)

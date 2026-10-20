# HemoChain

## Sobre o Projeto
Um Sistema inteligente para gestão da cadeia de hemocomponentes, integrando estoque, compatibilidade, validade e distribuição hospitalar com segurança e eficiência.


## Tecnologias usadas: 
Java e SpringBoot


## Como rodar o projeto: 
Requer JDK 17. Na pasta do projeto, execute `./mvnw.cmd spring-boot:run` e abra http://localhost:8080.
Cadastre um usuário sintético com perfil **Gestor de Hemocentro**, faça login e selecione **Estoque e validade**.
O H2 grava em `./data/hemochain`; mantenha o mesmo diretório de execução para reutilizar o banco.
Para compilar, testar e gerar o JAR: `./mvnw.cmd verify`. Encerre a aplicação antes de reempacotar o JAR no Windows.


## Integrantes:

|Nome|Email|Data de entrada no grupo|
|----|-----|------------------------|
|Mateus Felipe de Souza Oliveira|mfso2@cesar.school|24/08
|Vinicius da Cruz Mendes|vcm5@cesar.school|21/08
|Luiz Felipe Matias Xavier|lfmx@cesar.school|21/08
|Matheus de Assis Carvalho Conolly Silva|maccs@cesar.school|21/08
|Igor Kaua de Souza Siqueira|ikss2@cesar.school|21/08
|Bruno José Cavalcanti Duarte Filho|bjcdf@cesar.school|21/08
|Bruno Sottomayor Martin|bsm4@cesar.school|21/08

# 1° Entrega 31/08 (Finalizado):
### 1. Histórias de Usuário
Foram definidas histórias de usuário claras e concisas, contendo descrição da funcionalidade, valor de negócio e critérios de aceitação utilizando BDD.

📄 Documento com as histórias:
https://github.com/mateusoliveiraadev/HemoChain/blob/main/User%20Stories.md

---

### 2. Protótipo Lo-Fi (Figma)
Protótipo de baixa fidelidade do HemoChain, desenvolvido no Figma de forma simples e intuitiva, contemplando as histórias de usuário US01, US02, US05, US06, US07 e US08 nesta primeira versão.

🎨 Acessar protótipo no Figma: https://www.figma.com/proto/9bukhNOO1d01FuRTQAeykD/EmoChain-entrega-01?node-id=3-3&p=f&t=Zfx04rPM9AhRBeHz-0&scaling=min-zoom&content-scaling=fixed&page-id=0%3A1&starting-point-node-id=3%3A3

---

### 3. Screencast do Protótipo

Demonstração da navegação e dos principais fluxos do primeiro protótipo Lo-Fi do HemoChain, contemplando as histórias de usuário US01, US02, US05, US06, US07 e US08.

▶️ Assistir no Youtube: https://youtu.be/oIFck2dseac?is=qg6glqN-vfdn1oYj

---

# 2° Entrega 21/09 (Finalizado):
### 1. Histórias de Usuário
Nesta entrega, implementamos mais 2 histórias ao nosso protótipo: a US08 - Controle de acesso por perfil de usuário e a US02 - Registro de requisição hospitalar. Com isso, ampliamos as funcionalidades do sistema e avançamos na implementação dos principais recursos previstos para o projeto.

📄 Documento com as histórias implementadas:
https://github.com/user-attachments/files/32494810/post-its.md


---

### 2. Issue/Bug Tracker
Este Bug Tracker reúne os principais problemas identificados durante o desenvolvimento, documentando suas causas e respectivas correções.
Também apresenta a classificação dos bugs por tipo, severidade e status, facilitando o acompanhamento e controle das correções.


<img width="660" height="640" alt="Captura de tela 2026-09-21 230835" src="https://github.com/user-attachments/assets/708f2148-4038-416a-a0e9-4acd630c6d7a" />

---

### 3. Screencast do Protótipo 
Neste vídeo, apresentamos uma demonstração do protótipo do HemoChain, mostrando o funcionamento do sistema e suas principais funcionalidades na prática.

▶️ Assistir no Youtube: https://youtu.be/DEyscop7MSQ

---

### 4. Screencast do Código 
Neste vídeo, apresentamos a implementação da aplicação utilizando Spring, explicando sua estrutura e demonstrando as histórias implementadas durante o desenvolvimento do projeto.

▶️ Assistir no Youtube: https://youtu.be/abhnkK4AO8s

---

# Entrega 03 — US01 + US07

### 1. Histórias de Usuário
#### Histórias implementadas — formato POST-IT

**US01 — Cadastro e controle de estoque**

**Como** gestor de estoque, **eu quero** cadastrar, consultar e atualizar bolsas, **para que** eu acompanhe os hemocomponentes disponíveis e sua validade.

Implementado: código único por bolsa, componente, ABO/Rh, volume, coleta, validade automática configurável, status, consulta, edição, validações e persistência H2.

**US07 — Alertas de vencimento e descarte**

**Como** gestor de estoque, **eu quero** receber alertas de vencimento e acompanhar descartes automáticos, **para que** eu priorize o uso e preserve o registro do descarte.

Implementado: limiares configuráveis de 5 e 2 dias, destaque de prioridade, descarte automático, histórico mínimo e verificação na inicialização, agendada e nas consultas. Vencidas não aparecem entre as disponíveis.

[POST-IT completo, funcionalidades e limitações](entrega-03/post-its.md).

#### Configuração didática

Os valores são **exclusivamente acadêmicos, não regras clínicas oficiais**. Em `src/main/resources/application.properties`:

| Componente | Dias após coleta | Propriedade |
|---|---:|---|
| Concentrado de hemácias | 35 | `estoque.validade.hemacias` |
| Plasma | 365 | `estoque.validade.plasma` |
| Plaquetas | 5 | `estoque.validade.plaquetas` |
| Crioprecipitado | 365 | `estoque.validade.crioprecipitado` |

- Alertas: `estoque.alertas.dias=5,2`. Cada limiar pode produzir um alerta distinto para a mesma bolsa.
- Agendamento: `estoque.verificacao.cron=0 0 0 * * *`, à meia-noite no fuso do servidor.
- Validade igual ou anterior ao dia atual implica descarte, sem apagar a bolsa.
- Histórico mínimo na bolsa: data/hora, motivo e responsável `SISTEMA`.
- Coleta histórica é aceita para representar estoque existente; se vencida, a bolsa é descartada automaticamente.
- Uma identificação representa uma bolsa. Volume permitido: 1 a 10.000 mL; coleta futura é rejeitada.

#### Classes, API e testes

Fluxo: `BolsaHemocomponente → BolsaRepository → BolsaService / ValidadeService → BolsaController`.
`AlertaValidade` e seu repositório persistem notificações; `VerificacaoValidadeAgendada` aciona a verificação. A tela `estoque.html` usa o menu existente.

| Método | Endpoint | Finalidade |
|---|---|---|
| POST | `/bolsas` | Cadastrar |
| GET | `/bolsas` | Listar; filtro opcional `status=DISPONIVEL` ou `DESCARTADO` |
| GET | `/bolsas/{id}` | Consultar dados e histórico mínimo do descarte |
| PUT | `/bolsas/{id}` | Editar campos permitidos de bolsa válida |
| GET | `/bolsas/alertas` | Consultar alertas ativos |

Os endpoints exigem `X-Perfil: GESTOR_HEMOCENTRO`, preservando o mecanismo didático da Entrega 02. Não é autenticação robusta. Não há exclusão física, FEFO, reserva, transporte ou rastreabilidade completa.

Testes novos: `EstoqueControllerTest` e `ValidadeServiceTest`, incluindo BDD das duas histórias, datas de fronteira, duplicidades, edição e histórico persistido. A suíte existente de US02/US08, indicadores e estruturas foi preservada.

📄 Documento com as histórias implementadas: [POST-IT — Entrega 03](entrega-03/post-its.md).

---

### 2. Issue/Bug Tracker
<img width="1600" height="1080" alt="bug-tracker" src="https://github.com/user-attachments/assets/b5005ea8-578f-4345-aead-b7e4b3bcf017" />

---

### 3. Screencast do Protótipo 
Neste vídeo, apresentamos uma demonstração prática do sistema desenvolvido, mostrando suas funcionalidades e como a aplicação funciona na prática.

O objetivo é demonstrar o funcionamento do sistema, apresentar seus principais recursos e mostrar os resultados do desenvolvimento do projeto.

▶️ Assistir no Youtube: https://youtu.be/-vNAE4rWjag

---

### 4. Screencast do Código 
Neste vídeo, apresentamos uma explicação do código desenvolvido para a aplicação utilizando Java e Spring Boot, demonstrando sua estrutura e o funcionamento dos principais componentes do projeto.

O objetivo é apresentar como o código foi organizado, explicar as principais partes da aplicação e mostrar como o Spring Boot é utilizado no desenvolvimento do backend.

▶️ Assistir no Youtube: https://youtu.be/LUkFA9IbRyM

---







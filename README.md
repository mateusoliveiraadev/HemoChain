# HemoChain

## Sobre o Projeto
Um Sistema inteligente para gestão da cadeia de hemocomponentes, integrando estoque, compatibilidade, validade e distribuição hospitalar com segurança e eficiência.


## Tecnologias usadas: 
Java 17, Spring Boot 3, PostgreSQL, Docker/Compose e GitHub Actions


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

## Como rodar o projeto

### Com Docker (recomendado, igual em qualquer maquina)

Requisitos: Docker com Compose v2. Nada de Java ou Maven instalado.

```bash
git clone https://github.com/mateusoliveiraadev/HemoChain.git
cd HemoChain
cp .env.example .env            # Windows (cmd): copy .env.example .env
docker compose up -d --build
docker compose ps               # api e db devem aparecer como "healthy"
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

Abra http://localhost:8080. Para parar: `docker compose down` (os dados ficam no volume `hemochain-pgdata`). Para apagar tambem os dados: `docker compose down -v`.

Se a porta 8080 estiver ocupada, troque `API_PORT` no `.env`.

### Sem Docker (desenvolvimento local, banco H2 em arquivo)

Requer JDK 17. Na pasta do projeto, execute `./mvnw.cmd spring-boot:run` (Windows) ou `./mvnw spring-boot:run` e abra http://localhost:8080.
Cadastre um usuario sintetico com perfil **Gestor de Hemocentro**, faca login e selecione **Estoque e validade**.
O H2 grava em `./data/hemochain`; mantenha o mesmo diretorio de execucao para reutilizar o banco.
Para compilar, testar e gerar o JAR: `./mvnw.cmd verify`. Encerre a aplicacao antes de reempacotar o JAR no Windows.

---

# Desafio do PI: Container, TDD e Deploy automatizados

URL publica do sistema: **(preencher apos o primeiro deploy, ver `docs/DEPLOY.md`)**

## E1. Container automatizado

| Arquivo | Papel |
|---|---|
| `Dockerfile` | Multi-stage. Estagio 1 (`maven:3.9-eclipse-temurin-17`) compila e roda `mvn verify`, incluindo todos os testes. Estagio 2 (`eclipse-temurin:17-jre-alpine`) leva somente o JAR, roda como usuario sem privilegios e tem `HEALTHCHECK`. |
| `compose.yaml` | API + PostgreSQL 16, volume nomeado, healthcheck nos dois servicos e `depends_on` com `service_healthy`. |
| `.env.example` | Variaveis necessarias. O `.env` real esta no `.gitignore`. |
| `application-prod.properties` | Perfil usado no container: tudo vem de variaveis de ambiente. |

Se um teste falhar, `docker build` falha: nao existe imagem com teste vermelho.

Evidencias: `docs/evidencias/` (prints de `docker compose ps` e da API respondendo).

## E2. TDD

### Story map (Release 1)

| Backbone | Cadastrar estoque | Controlar validade | Requisitar | Alocar | Governar acesso |
|---|---|---|---|---|---|
| **Release 1** | US01 | US07 | US02 | **US03 (parte 1)** | US08 |
| Depois | | | | US03 reserva (T4), US04 rota | US05 temperatura, US06 painel |

### Historias da Release 1 quebradas em tarefas

**US03 (parte 1): hemacias compativeis por FEFO.** Entregue via TDD neste desafio.

| Tarefa | Nivel do teste | Teste | Situacao |
|---|---|---|---|
| T1 Regra ABO/Rh de hemacias | Unitario | `CompatibilidadeServiceTest` | feito |
| T2 Consulta de bolsas disponiveis ordenada por validade | Integracao (com banco) | `BolsaCompativelRepositoryTest` | feito |
| T3 Endpoint `GET /bolsas/compativeis?receptor=` restrito ao gestor | Aceitacao (BDD) | `CompatibilidadeAceitacaoTest` + `docs/bdd/compatibilidade.feature` | feito |
| T4 Reservar a bolsa para uma requisicao (US03 parte 2) | A definir | | pendente |

**US01 (estoque).** T1 validade por componente (unitario `ValidadeServiceTest`); T2 cadastro, duplicidade e campos obrigatorios (aceitacao `EstoqueControllerTest`); T3 persistencia e consulta (integracao, mesmo teste, H2).

**US07 (alertas e descarte).** T1 limiares 5 e 2 dias e fronteiras (unitario `ValidadeServiceTest`); T2 descarte automatico preserva o registro (integracao `EstoqueControllerTest`); T3 vencidas fora da lista de disponiveis (aceitacao).

**US02 e US08.** `RequisicaoServiceTest`, `UsuarioServiceTest`, `AcessoPorPerfilTest`, `HospitalServiceTest`.

### Testes por nivel

| Nivel | Onde | Como rodar |
|---|---|---|
| Unitario | `src/test/java/.../service`, `.../estrutura` | `mvn test` |
| Integracao (com banco) | `BolsaCompativelRepositoryTest` (`@DataJpaTest`), `EstoqueControllerTest` | `mvn test` |
| Aceitacao | `CompatibilidadeAceitacaoTest` (cenarios Gherkin em `docs/bdd/`) | `mvn test` |
| Integracao com PostgreSQL real | job `container` da esteira (sobe o compose e consulta `/actuator/health`) | `docker compose up -d --build --wait` |

Rodar tudo: `./mvnw verify` (ou `docker build .`, que roda os testes no 1o estagio).

### Historico red -> green

Na branch `feature/pi-container-tdd-deploy`:

```
test: compatibilidade ABO/Rh de hemacias com FEFO (RED)     <- so testes; o build falha de proposito
feat: sugere hemacias compativeis (ABO/Rh) por FEFO ... (GREEN)
```

Veja com `git log --oneline`. O "vermelho" aqui e falha de compilacao (as classes ainda nao existiam), que e a forma mais comum do primeiro passo do TDD em Java.

### Quais regras ganharam teste primeiro, e por que

- **Compatibilidade ABO/Rh (US03).** Primeiro teste: um erro nessa tabela pode sugerir sangue incompativel para um paciente. E o maior risco do dominio, por isso a tabela completa dos 8 receptores esta no teste.
- **FEFO e bolsa vencida fora da sugestao.** Evita usar bolsa vencida e reduz descarte.
- **Acesso so do gestor ao endpoint.** Dado sensivel de estoque.

### O que ficou sem teste (e por que)

- As regras de US01, US07, US02 e US08 foram entregues antes deste desafio. Elas tem testes, mas **nao ha historico red -> green delas no Git**.
- Telas estaticas em `src/main/resources/static/*.html` (sem teste automatizado de interface).
- `DadosSinteticosService` e `DadosSinteticosController` (gerador de dados de demonstracao).
- Agendamento por `cron` em horario real (o teste chama o metodo de verificacao diretamente).
- Autenticacao real: o acesso por perfil usa o cabecalho `X-Perfil`, mecanismo didatico das entregas anteriores.

### Testes na esteira

O job `test` roda `mvn verify` a cada push e a cada pull request. Teste vermelho deixa o job vermelho e, com a protecao da `main` ativada (ver `docs/DEPLOY.md`), o merge e bloqueado.

## E3. Deploy automatizado

```
push/PR --> [test: mvn verify] --> [container: compose sobe do zero + health] --> (so na main) [publish: imagem no GHCR] --> [deploy: Render] --> health da URL publica
```

- Ferramenta: **GitHub Actions** (`.github/workflows/ci-cd.yml`).
- Fluxo de branch: **GitHub Flow**. Branches curtas -> pull request -> `main` protegida -> deploy a cada merge.
- Provedor: **Render**, servico por imagem Docker (GHCR) + PostgreSQL gerenciado.
- Segredos: no cofre do GitHub (`RENDER_DEPLOY_HOOK_URL`) e nas variaveis do servico no Render. Nada no repositorio.
- Rollback: workflow **Rollback** (`.github/workflows/rollback.yml`). Informe o SHA de um commit estavel; a imagem dele ja esta no GHCR. Alternativa: botao *Rollback* do proprio Render.
- Passo a passo para configurar tudo (uma unica vez): `docs/DEPLOY.md`.
- Evidencias (execucao vermelha que barrou, verde que publicou, URL no ar): `docs/evidencias/`.

## Decisoes

| Decisao | Escolha | Alternativas | Por que |
|---|---|---|---|
| Imagem base | `eclipse-temurin:17-jre-alpine` (runtime) e `maven:3.9-eclipse-temurin-17` (build) | `eclipse-temurin:17-jdk`, `openjdk` | JRE alpine e menor e tem menos pacotes vulneraveis; a compilacao fica no 1o estagio. Java 17 e o `java.version` do `pom.xml`. |
| Dados | PostgreSQL 16 com volume nomeado `hemochain-pgdata` | H2 em arquivo, MySQL, bind mount | Os dados sobrevivem ao `docker compose down`; PostgreSQL e o que o Render oferece como banco gerenciado, entao dev e producao usam o mesmo motor. H2 segue nos testes por ser rapido. |
| Rede | Rede propria do Compose; a API acessa o banco por `db:5432` | `localhost`, IP fixo | O DNS interno resolve o nome do servico; o banco nao e publicado no host. |
| Testes | Compatibilidade ABO/Rh primeiro | Validade, cadastro | Um erro ali poe um paciente em risco. |
| Fluxo de branch | GitHub Flow | Git Flow, GitLab Flow | Grupo pequeno, uma versao no ar, deploy a cada merge na `main`. |
| CI/CD e deploy | GitHub Actions + Render (imagem do GHCR) | GitLab CI, Railway, Fly.io | O codigo ja esta no GitHub; o Render tem plano gratuito e implanta uma imagem por tag, o que torna o rollback trivial. |
| Segredos | GitHub Secrets e variaveis do Render; `.env` no `.gitignore` | `.env` versionado | Senha de banco e URL do hook nunca entram no Git. `.env.example` so tem valores ficticios. |
| Healthcheck | Actuator `/actuator/health` | Endpoint proprio | Padrao do Spring Boot, sem detalhes expostos. |

## Uso de IA

Declarado conforme a regra da disciplina. A IA rascunhou; o grupo valida e assina.

- **Ferramenta:** Claude (Anthropic), via Claude Code. As entregas anteriores usaram o Codex (ver Entrega 03).
- **O que a IA produziu neste desafio:** `Dockerfile`, `compose.yaml`, `.env.example`, perfil `prod`, workflows do GitHub Actions, `docs/DEPLOY.md`, os testes e a implementacao da compatibilidade ABO/Rh, este texto do README.
- **Prompt principal:** "Atue como Engenheiro de Software Sênior e Especialista em DevOps. Com base na estrutura atual do repositório HemoChain e nos requisitos do Desafio do PI (Container, TDD e Deploy Automatizados), realize a implementação das seguintes entregas diretamente na estrutura de pastas do projeto:

Regras de Negócio e TDD: Implemente a lógica de compatibilidade sanguínea ABO/Rh para os 8 tipos de receptores utilizando desenvolvimento guiado por testes (TDD).

Containerização: Crie o Dockerfile (utilizando multi-stage build para otimização), o compose.yaml configurado com o perfil prod e o arquivo .env.example.

Automação (CI/CD): Configure os workflows do GitHub Actions para automação de build, testes e deploy.

Documentação: Elabore o guia detalhado em docs/DEPLOY.md e atualize o README.md com as instruções de execução do projeto.

Diretrizes de execução: Priorize código limpo, siga as boas práticas do Spring Boot e salve todas as alterações diretamente nos arquivos correspondentes do repositório."
- **Limite conhecido:** o ambiente da IA nao conseguia baixar dependencias do Maven nem executar os testes. A regra de compatibilidade foi conferida em Java puro (tabela dos 8 receptores); os testes Spring, o `docker build`, o `docker compose` e a esteira **ainda precisam ser executados pelo grupo**, e a evidencia deve ser registrada em `docs/evidencias/`.
- **Validacao do grupo:** (preencher) revisado por Bruno Martin em 20/10/2026.

---

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

# 3° Entrega 19/10 (Finalizado):

### 1. Histórias de Usuário
Neste documento, apresentamos as histórias de usuário definidas para a Entrega 03 do projeto HemoChain, com foco no gerenciamento de estoque de hemocomponentes e no controle de vencimentos

📄 Documento com as histórias implementadas:
[post-its.md](https://github.com/user-attachments/files/33717714/post-its.md)

---

### 2. Issue/Bug Tracker
Neste material, apresentamos o Bug Tracker da Entrega 03 do projeto HemoChain, documentando dois bugs identificados e corrigidos durante o desenvolvimento do sistema.

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

# 4° Entrega 09/11 (Em Andamento):

### 1. Histórias de Usuário

📄 Documento com as histórias implementadas:

---

### 2. Issue/Bug Tracker

---

### 3. Screencast do Protótipo 
▶️ Assistir no Youtube:

---

### 4. Screencast do Código 
▶️ Assistir no Youtube:

---














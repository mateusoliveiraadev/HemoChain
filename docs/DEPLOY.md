# Deploy: configuracao unica (GitHub Actions + Render)

Faca uma vez. Depois, cada merge na `main` com a esteira verde publica sozinho.
Os nomes de menus podem mudar com o tempo; confira na documentacao da plataforma.

## 1. Proteger a main (GitHub)

Settings > Branches > Add branch protection rule para `main`:
- Require a pull request before merging
- Require status checks to pass before merging: marque **Build e testes (Maven)** e **Container sobe do zero (compose + healthcheck)**
- Do not allow bypassing (ninguem faz push direto na main)

As checks so aparecem na lista depois de rodarem uma vez (abra o primeiro PR antes).

## 2. Banco no Render

New > PostgreSQL. Anote o **host interno**, o nome do banco, o usuario e a senha.
(O plano gratuito do Render para PostgreSQL tem prazo de validade; confira o plano vigente.)

## 3. Imagem publica no GHCR

A primeira execucao do job `publish` (primeiro merge na main) cria o pacote `ghcr.io/<dono>/hemochain`.
Em GitHub > Packages > hemochain > Package settings, deixe a visibilidade **Public**
(ou cadastre credenciais do registry no Render).

## 4. Servico web no Render

New > Web Service > **Existing Image** > `ghcr.io/<dono-em-minusculo>/hemochain:latest`.
Variaveis de ambiente do servico:

| Variavel | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host-interno>:5432/<banco>` |
| `SPRING_DATASOURCE_USERNAME` | usuario do banco |
| `SPRING_DATASOURCE_PASSWORD` | senha do banco (marque como secreta) |

Health Check Path: `/actuator/health`. O Render define `PORT`; a aplicacao le essa variavel.
Em Settings do servico, copie a **Deploy Hook URL**.

## 5. Segredos e variaveis no GitHub

Settings > Environments > New environment: `production`.
- Secret `RENDER_DEPLOY_HOOK_URL`: a Deploy Hook URL do passo 4
- Variable `APP_URL`: a URL publica, por exemplo `https://hemochain.onrender.com`

## 6. Primeiro deploy

Abra um PR da branch `feature/pi-container-tdd-deploy` para `main`, espere a esteira ficar verde e faca o merge.
O job `publish` envia a imagem (tag = SHA do commit) e o job `deploy` chama o hook com
`imgURL=ghcr.io/<dono>/hemochain:<sha>` e espera `APP_URL/actuator/health` responder `UP`.

## Rollback

Actions > **Rollback** > Run workflow > informe o SHA completo de um commit anterior que estava bom.
A imagem desse commit continua no GHCR. Alternativa: aba Events/Deploys do Render > Rollback.

## Aviso de seguranca

O controle de acesso por perfil usa o cabecalho `X-Perfil` (mecanismo didatico das entregas anteriores):
qualquer pessoa que conheca o cabecalho acessa o estoque. Use **somente dados sinteticos** no ambiente publico.

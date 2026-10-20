# certs/

Opcional. Se a sua rede (empresa, faculdade) inspeciona HTTPS e o build do Docker falha com
"PKIX path building failed", coloque aqui o certificado raiz da rede em formato PEM
(arquivo .crt ou .pem). O Dockerfile importa tudo que estiver nesta pasta no truststore do
Java do estagio de build. Os certificados NAO vao para o Git (veja .gitignore).
Em redes normais (e na esteira do GitHub) esta pasta fica vazia.

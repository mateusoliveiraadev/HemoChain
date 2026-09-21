package com.hemochain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hemochain.entity.PerfilUsuario;
import com.hemochain.entity.Usuario;

@SpringBootTest
@Transactional
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void cadastrarUsuarioComSucesso() {
        Usuario novo = new Usuario("Ana Souza", "ana@hemocentro.com", "senha123", PerfilUsuario.GESTOR_HEMOCENTRO);

        Usuario salvo = usuarioService.cadastrar(novo);

        assertNotNull(salvo.getId());
        assertNotEquals("senha123", salvo.getSenha(), "a senha nao pode ser salva em texto puro");
    }

    @Test
    void cadastrarComEmailDuplicadoDeveFalhar() {
        usuarioService.cadastrar(new Usuario("Bruno", "bruno@hemocentro.com", "senha123", PerfilUsuario.OPERADOR_LOGISTICA));

        ResponseStatusException excecao = assertThrows(ResponseStatusException.class, () ->
                usuarioService.cadastrar(new Usuario("Bruno 2", "bruno@hemocentro.com", "outrasenha", PerfilUsuario.OPERADOR_LOGISTICA)));

        assertEquals(400, excecao.getStatusCode().value());
    }

    @Test
    void loginComCredenciaisCorretasFunciona() {
        usuarioService.cadastrar(new Usuario("Carla", "carla@hospital.com", "minhasenha", PerfilUsuario.HOSPITAL_SOLICITANTE));

        Usuario logado = usuarioService.login("carla@hospital.com", "minhasenha");

        assertEquals("Carla", logado.getNome());
        assertEquals(PerfilUsuario.HOSPITAL_SOLICITANTE, logado.getPerfil());
    }

    @Test
    void loginComSenhaIncorretaDeveFalhar() {
        usuarioService.cadastrar(new Usuario("Diego", "diego@hospital.com", "senhacerta", PerfilUsuario.HOSPITAL_SOLICITANTE));

        ResponseStatusException excecao = assertThrows(ResponseStatusException.class, () ->
                usuarioService.login("diego@hospital.com", "senhaerrada"));

        assertEquals(401, excecao.getStatusCode().value());
    }

    @Test
    void loginComEmailInexistenteDeveFalhar() {
        assertThrows(ResponseStatusException.class, () ->
                usuarioService.login("naoexiste@hospital.com", "qualquersenha"));
    }
}

package com.hemochain.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hemochain.entity.Usuario;
import com.hemochain.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario cadastrar(Usuario usuario) {
        usuario.setEmail(usuario.getEmail().toLowerCase().trim());
        usuario.setNome(usuario.getNome().trim());

        if (repository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este e-mail ja esta cadastrado.");
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return repository.save(usuario);
    }

    public Usuario login(String email, String senha) {
        Optional<Usuario> usuarioEncontrado = repository.findByEmail(email.toLowerCase().trim());

        if (usuarioEncontrado.isEmpty() || !passwordEncoder.matches(senha, usuarioEncontrado.get().getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }

        return usuarioEncontrado.get();
    }
}

package com.hemochain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// So o bean de hash de senha (BCrypt). Sem o starter completo de Spring Security,
// entao nao ha filtro de autenticacao nem tela de login sendo ativados por baixo dos panos.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

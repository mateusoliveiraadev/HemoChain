package com.hemochain.config;

import java.time.LocalDate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.hemochain.service.ValidadeService;

@Component
@EnableScheduling
public class VerificacaoValidadeAgendada {
    private final ValidadeService validade;

    public VerificacaoValidadeAgendada(ValidadeService validade) {
        this.validade = validade;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        verificar();
    }

    @Scheduled(cron = "${estoque.verificacao.cron:0 0 0 * * *}")
    public void verificar() {
        validade.verificar(LocalDate.now());
    }
}

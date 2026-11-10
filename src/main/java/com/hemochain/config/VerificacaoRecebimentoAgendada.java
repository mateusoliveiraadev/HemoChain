package com.hemochain.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.hemochain.service.AtendimentoHospitalarService;

@Component
public class VerificacaoRecebimentoAgendada {
    private final AtendimentoHospitalarService atendimento;
    public VerificacaoRecebimentoAgendada(AtendimentoHospitalarService atendimento) { this.atendimento = atendimento; }
    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() { verificar(); }
    @Scheduled(fixedDelayString = "${recebimento.verificacao-ms:60000}")
    public void verificar() { atendimento.verificarPendencias(); }
}

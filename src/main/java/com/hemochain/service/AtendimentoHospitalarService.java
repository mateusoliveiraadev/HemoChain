package com.hemochain.service;

import java.time.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hemochain.entity.*;
import com.hemochain.repository.*;

@Service
public class AtendimentoHospitalarService {
    private final RequisicaoRepository requisicoes;
    private final BolsaRepository bolsas;
    private final EntregaHospitalarRepository entregas;
    private final CompatibilidadeService compatibilidade;
    private final RastreabilidadeService rastreabilidade;
    private final Clock relogio;
    private final Duration prazo;

    public AtendimentoHospitalarService(RequisicaoRepository requisicoes, BolsaRepository bolsas,
            EntregaHospitalarRepository entregas, CompatibilidadeService compatibilidade,
            RastreabilidadeService rastreabilidade, Clock relogio,
            @Value("${recebimento.prazo:PT24H}") Duration prazo) {
        if (prazo.isZero() || prazo.isNegative()) throw new IllegalArgumentException("Prazo deve ser positivo.");
        this.requisicoes = requisicoes;
        this.bolsas = bolsas;
        this.entregas = entregas;
        this.compatibilidade = compatibilidade;
        this.rastreabilidade = rastreabilidade;
        this.relogio = relogio;
        this.prazo = prazo;
    }

    @Transactional
    public EntregaHospitalar registrarEntrega(Long requisicaoId, Long bolsaId) {
        RequisicaoHospitalar r = bloquear(requisicaoId);
        List<EntregaHospitalar> existentes = entregas.findByRequisicaoIdOrderByEntregueEmAscIdAsc(r.getId());
        if (existentes.size() >= r.getQuantidade()) conflito("Quantidade solicitada ja atendida.");
        BolsaHemocomponente b = bolsas.buscarParaAtualizacao(bolsaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Bolsa nao encontrada."));
        if (b.getStatus() != StatusBolsa.DISPONIVEL || !b.getValidade().isAfter(hoje())) {
            conflito("A entrega exige bolsa disponivel e valida.");
        }
        // A tabela ABO/Rh existente aplica-se somente a hemacias: nao generalizar para plasma/plaquetas.
        if (r.getTipoComponente() != TipoComponente.CONCENTRADO_HEMACIAS
                || b.getTipoComponente() != TipoComponente.CONCENTRADO_HEMACIAS) {
            conflito("Nesta entrega, a associacao hospitalar suporta somente concentrado de hemacias.");
        }
        if (!compatibilidade.podeDoarHemacias(b.getTipoSanguineo(), r.getTipoSanguineo())) {
            conflito("Bolsa incompativel com o receptor.");
        }
        Instant agora = agora();
        EntregaHospitalar entrega = entregas.save(new EntregaHospitalar(r, b, agora, agora.plus(prazo)));
        b.setStatus(StatusBolsa.ENTREGUE);
        rastreabilidade.registrar(b, TipoEventoBolsa.ENTREGA_REGISTRADA, entrega.getOrigemEntrega(),
                "Atestacao de entrega ocorrida; requisicao=" + r.getId() + "; hospital=" + r.getHospital().getId());
        atualizarStatus(r);
        return entrega;
    }

    @Transactional
    public EntregaHospitalar receber(Long requisicaoId, Long entregaId, Long hospitalId,
            CondicaoRecebimento condicao, String observacao) {
        RequisicaoHospitalar r = bloquear(requisicaoId);
        exigirHospital(r, hospitalId);
        EntregaHospitalar e = buscarEntrega(r, entregaId);
        if (e.getRecebidaEm() != null) conflito("Recebimento ja confirmado.");
        if (condicao == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a condicao.");
        BolsaHemocomponente b = bloquearBolsa(e);
        if (b.getStatus() != StatusBolsa.ENTREGUE) conflito("Bolsa nao esta entregue.");
        if (condicao == CondicaoRecebimento.NAO_CONFORME && (observacao == null || observacao.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descreva a nao conformidade.");
        }
        if (condicao == CondicaoRecebimento.INTEGRA && !b.getValidade().isAfter(hoje())) {
            conflito("Bolsa vencida: registre recebimento nao conforme.");
        }
        String origem = origemHospital(hospitalId);
        e.receber(condicao, observacao, agora(), origem);
        b.setStatus(condicao == CondicaoRecebimento.INTEGRA ? StatusBolsa.RECEBIDA : StatusBolsa.BLOQUEADA);
        rastreabilidade.registrar(b, condicao == CondicaoRecebimento.INTEGRA
                ? TipoEventoBolsa.RECEBIDA : TipoEventoBolsa.RECEBIDA_NAO_CONFORME, origem,
                "requisicao=" + r.getId() + "; condicao=" + condicao + "; observacao=" + (observacao == null ? "" : observacao));
        atualizarStatus(r);
        return e;
    }

    @Transactional
    public EntregaHospitalar utilizar(Long requisicaoId, Long entregaId, Long hospitalId) {
        RequisicaoHospitalar r = bloquear(requisicaoId);
        exigirHospital(r, hospitalId);
        EntregaHospitalar e = buscarEntrega(r, entregaId);
        BolsaHemocomponente b = bloquearBolsa(e);
        if (e.getRecebidaEm() == null || e.getCondicao() != CondicaoRecebimento.INTEGRA
                || e.getUtilizadaEm() != null || b.getStatus() != StatusBolsa.RECEBIDA
                || !b.getValidade().isAfter(hoje())) {
            conflito("Uso exige recebimento integro, bolsa valida e ainda nao utilizada.");
        }
        String origem = origemHospital(hospitalId);
        e.utilizar(agora(), origem);
        b.setStatus(StatusBolsa.UTILIZADA);
        rastreabilidade.registrar(b, TipoEventoBolsa.UTILIZADA, origem, "requisicao=" + r.getId());
        atualizarStatus(r);
        return e;
    }

    @Transactional(readOnly = true)
    public List<EntregaHospitalar> listar(Long requisicaoId, boolean gestor, Long hospitalId) {
        RequisicaoHospitalar r = requisicoes.findById(requisicaoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisicao nao encontrada."));
        if (!gestor) exigirHospital(r, hospitalId);
        return entregas.findByRequisicaoIdOrderByEntregueEmAscIdAsc(requisicaoId);
    }

    @Transactional
    public List<EntregaHospitalar> verificarPendencias() {
        Instant agora = agora();
        // Mesmo bloqueio do recebimento; revalida a condicao apos adquirir o lock.
        for (Long id : entregas.requisicoesAtrasadas(agora)) {
            RequisicaoHospitalar r = bloquear(id);
            for (EntregaHospitalar e : entregas.findByRequisicaoIdOrderByEntregueEmAscIdAsc(id)) {
                if (e.getRecebidaEm() == null && e.getPrazoConfirmacao().isBefore(agora) && e.getAlertaEm() == null) {
                    e.alertar(agora);
                    rastreabilidade.registrar(e.getBolsa(), TipoEventoBolsa.CONFIRMACAO_ATRASADA,
                            "SISTEMA:PENDENCIAS", "requisicao=" + id + "; prazo=" + e.getPrazoConfirmacao());
                }
            }
            atualizarStatus(r);
        }
        return entregas.findByRecebidaEmIsNullAndAlertaEmIsNotNullOrderByPrazoConfirmacaoAsc();
    }

    private void atualizarStatus(RequisicaoHospitalar r) {
        List<EntregaHospitalar> lista = entregas.findByRequisicaoIdOrderByEntregueEmAscIdAsc(r.getId());
        long recebidas = lista.stream().filter(e -> e.getRecebidaEm() != null).count();
        long usadas = lista.stream().filter(e -> e.getUtilizadaEm() != null).count();
        StatusRequisicao status;
        if (lista.stream().anyMatch(e -> e.getRecebidaEm() == null && e.getAlertaEm() != null)) {
            status = StatusRequisicao.RECEBIMENTO_PENDENTE;
        } else if (lista.stream().anyMatch(e -> e.getCondicao() == CondicaoRecebimento.NAO_CONFORME)) {
            status = StatusRequisicao.NAO_CONFORME;
        } else if (usadas == r.getQuantidade()) status = StatusRequisicao.UTILIZADA;
        else if (usadas > 0) status = StatusRequisicao.PARCIALMENTE_UTILIZADA;
        else if (recebidas == r.getQuantidade()) status = StatusRequisicao.RECEBIDA;
        else if (recebidas > 0) status = StatusRequisicao.PARCIALMENTE_RECEBIDA;
        else if (lista.size() == r.getQuantidade()) status = StatusRequisicao.ENTREGUE;
        else if (!lista.isEmpty()) status = StatusRequisicao.PARCIALMENTE_ENTREGUE;
        else status = StatusRequisicao.SOLICITADA;
        r.setStatus(status);
    }

    private RequisicaoHospitalar bloquear(Long id) {
        return requisicoes.buscarParaAtualizacao(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisicao nao encontrada."));
    }
    private EntregaHospitalar buscarEntrega(RequisicaoHospitalar r, Long id) {
        return entregas.findById(id).filter(e -> e.getRequisicao().getId().equals(r.getId())).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega nao encontrada nesta requisicao."));
    }
    private BolsaHemocomponente bloquearBolsa(EntregaHospitalar e) {
        return bolsas.buscarParaAtualizacao(e.getBolsa().getId()).orElseThrow();
    }
    public static void exigirHospital(RequisicaoHospitalar r, Long hospitalId) {
        if (hospitalId == null || !r.getHospital().getId().equals(hospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operacao restrita ao hospital da requisicao.");
        }
    }
    private static String origemHospital(Long id) { return "PERFIL:HOSPITAL_SOLICITANTE;HOSPITAL:" + id; }
    private LocalDate hoje() { return LocalDate.now(relogio); }
    private Instant agora() { return relogio.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
    private static void conflito(String mensagem) { throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem); }
}

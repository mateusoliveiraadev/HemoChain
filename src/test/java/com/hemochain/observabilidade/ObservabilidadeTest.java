package com.hemochain.observabilidade;

import com.hemochain.controller.ErroSimuladoController;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"observabilidade", "test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.application.name=hemochain", "management.server.port=0",
    "management.tracing.enabled=false", "jdbc.datasource-proxy.enabled=false"
})
class ObservabilidadeTest {
    @Autowired TestRestTemplate http;
    @Autowired MeterRegistry meters;
    @Autowired ApplicationContext context;

    @Test
    void medeTentativaRecusadaComProxyRealSemAfirmarCommit() {
        double antes = meters.find("hemochain.entrega.tentativas").counters().stream().mapToDouble(c -> c.count()).sum();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Perfil", "GESTOR_HEMOCENTRO");
        var response = http.postForEntity("/requisicoes/999999999/entregas",
                new HttpEntity<>(Map.of("bolsaId", 999999999L, "entregaRealizada", true), headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(meters.find("hemochain.entrega.tentativas").counters().stream().mapToDouble(c -> c.count()).sum()).isEqualTo(antes + 1);
        assertThat(meters.find("hemochain.entrega.tentativas").tag("result", "failure").counter()).isNotNull();
        var timer = meters.find("hemochain.entrega.registro").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isPositive();
        assertThat(timer.getId().getDescription()).contains("tentativas");
        assertThat(timer.getId().getTag("application")).isEqualTo("hemochain");
    }

    @Test
    void perfilObservabilidadeNaoHabilitaErroDeDemonstracaoNemActuatorNaApi() {
        assertThat(context.getBeansOfType(ErroSimuladoController.class)).isEmpty();
        assertThat(http.getForEntity("/observabilidade/erro-simulado", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.getForEntity("/actuator/prometheus", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

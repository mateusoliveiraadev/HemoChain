package com.hemochain.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Liga as anotacoes @Counted e @Observed usadas nos services. */
@Configuration
@Profile("observabilidade")
public class ObservabilidadeConfig {

    @Bean
    MeterFilter descricaoDuracaoEntrega() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                return id.getName().equals("hemochain.entrega.registro")
                        ? new Meter.Id(id.getName(), Tags.of(id.getTags()), id.getBaseUnit(),
                            "Duracao das tentativas de registrar entrega, incluindo erros; nao mede commit", id.getType())
                        : id;
            }
        };
    }

    @Bean
    CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    @Bean
    ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }
}

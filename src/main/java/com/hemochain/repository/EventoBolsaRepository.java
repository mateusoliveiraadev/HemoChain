package com.hemochain.repository;

import java.util.List;
import org.springframework.data.repository.Repository;
import com.hemochain.entity.EventoBolsa;

// Sem metodos de update/delete, inclusive no contrato do repositorio.
public interface EventoBolsaRepository extends Repository<EventoBolsa, Long> {
    List<EventoBolsa> findByBolsa_IdOrderByMomentoAscIdAsc(Long bolsaId);
}

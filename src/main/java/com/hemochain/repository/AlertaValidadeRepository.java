package com.hemochain.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemochain.entity.AlertaValidade;
import com.hemochain.entity.StatusBolsa;

public interface AlertaValidadeRepository extends JpaRepository<AlertaValidade, Long> {
    boolean existsByBolsaIdAndLimiarDias(Long bolsaId, int limiarDias);
    List<AlertaValidade> findByBolsaStatusOrderByDataCriacaoDesc(StatusBolsa status);
    void deleteByBolsaId(Long bolsaId);
}

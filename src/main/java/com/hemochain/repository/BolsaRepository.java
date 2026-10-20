package com.hemochain.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import com.hemochain.entity.BolsaHemocomponente;
import com.hemochain.entity.StatusBolsa;

public interface BolsaRepository extends JpaRepository<BolsaHemocomponente, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    List<BolsaHemocomponente> findAllByOrderByValidadeAscIdAsc();
    List<BolsaHemocomponente> findByStatusOrderByValidadeAscIdAsc(StatusBolsa status);

    // As verificacoes e edicoes usam o mesmo bloqueio para nao duplicar eventos.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BolsaHemocomponente b where b.status = :status order by b.id")
    List<BolsaHemocomponente> buscarParaVerificacao(StatusBolsa status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BolsaHemocomponente b where b.id = :id")
    Optional<BolsaHemocomponente> buscarParaAtualizacao(Long id);
}

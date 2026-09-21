package com.hemochain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemochain.entity.RequisicaoHospitalar;

public interface RequisicaoRepository extends JpaRepository<RequisicaoHospitalar, Long> {
    List<RequisicaoHospitalar> findAllByOrderByDataCriacaoDesc();
}

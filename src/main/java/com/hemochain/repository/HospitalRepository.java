package com.hemochain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hemochain.entity.Hospital;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
}

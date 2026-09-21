package com.hemochain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hemochain.entity.Hospital;
import com.hemochain.repository.HospitalRepository;

@Service
public class HospitalService {

    private final HospitalRepository repository;

    public HospitalService(HospitalRepository repository) {
        this.repository = repository;
    }

    public Hospital cadastrar(Hospital hospital) {
        return repository.save(hospital);
    }

    public List<Hospital> listarTodos() {
        return repository.findAll();
    }
}

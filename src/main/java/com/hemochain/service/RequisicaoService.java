package com.hemochain.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hemochain.entity.Hospital;
import com.hemochain.entity.RequisicaoHospitalar;
import com.hemochain.repository.HospitalRepository;
import com.hemochain.repository.RequisicaoRepository;

@Service
public class RequisicaoService {

    private final RequisicaoRepository repository;
    private final HospitalRepository hospitalRepository;

    public RequisicaoService(RequisicaoRepository repository, HospitalRepository hospitalRepository) {
        this.repository = repository;
        this.hospitalRepository = hospitalRepository;
    }

    public RequisicaoHospitalar cadastrar(RequisicaoHospitalar requisicao) {
        if (requisicao.getHospitalId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o hospital da requisicao.");
        }

        Hospital hospital = hospitalRepository.findById(requisicao.getHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital nao encontrado."));

        requisicao.setHospital(hospital);
        return repository.save(requisicao);
    }

    public List<RequisicaoHospitalar> listarTodas() {
        return repository.findAllByOrderByDataCriacaoDesc();
    }
}

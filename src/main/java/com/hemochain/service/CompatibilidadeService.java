package com.hemochain.service;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import com.hemochain.entity.TipoSanguineo;

/**
 * Compatibilidade ABO/Rh para concentrado de hemacias.
 * Regra DIDATICA, nao e protocolo clinico oficial.
 */
@Service
public class CompatibilidadeService {

    public boolean podeDoarHemacias(TipoSanguineo doador, TipoSanguineo receptor) {
        exigir(doador, "doador");
        exigir(receptor, "receptor");
        return aboCompativel(doador, receptor) && rhCompativel(doador, receptor);
    }

    public List<TipoSanguineo> doadoresCompativeis(TipoSanguineo receptor) {
        exigir(receptor, "receptor");
        return Arrays.stream(TipoSanguineo.values())
                .filter(doador -> podeDoarHemacias(doador, receptor))
                .toList();
    }

    // O doa para qualquer grupo; AB recebe de qualquer grupo; demais so para o mesmo grupo.
    private boolean aboCompativel(TipoSanguineo doador, TipoSanguineo receptor) {
        return "O".equals(doador.getAbo())
                || "AB".equals(receptor.getAbo())
                || doador.getAbo().equals(receptor.getAbo());
    }

    // Doador Rh+ so doa para receptor Rh+.
    private boolean rhCompativel(TipoSanguineo doador, TipoSanguineo receptor) {
        return !doador.isRhPositivo() || receptor.isRhPositivo();
    }

    private void exigir(TipoSanguineo tipo, String papel) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo sanguineo do " + papel + " e obrigatorio.");
        }
    }
}

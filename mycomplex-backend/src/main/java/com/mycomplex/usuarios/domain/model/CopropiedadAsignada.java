package com.mycomplex.usuarios.domain.model;

import java.util.UUID;

public record CopropiedadAsignada(
        UUID copropiedadId,
        String nombre,
        String ciudad,
        RolCopropiedad rol
) {
}
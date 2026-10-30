package com.mycomplex.users.application.validation;

public enum ResidentialAssignmentValidationError {
    INCOMPLETE("Cada asignación residencial debe estar completa."),
    DUPLICATE_UNIT("No se puede asignar dos veces la misma unidad."),
    MULTIPLE_PRIMARY("Solo una unidad puede ser principal.");

    private final String safeMessage;

    ResidentialAssignmentValidationError(String safeMessage) {
        this.safeMessage = safeMessage;
    }

    public String safeMessage() {
        return safeMessage;
    }
}

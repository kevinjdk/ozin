package com.ozin.domain.shared;

/** Se lanza cuando se intenta una transición de estado no permitida por la máquina de estados. */
public final class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(Enum<?> currentState, String action) {
        super("INVALID_STATE_TRANSITION",
                "No se puede %s una cita en estado %s".formatted(action, currentState));
    }
}

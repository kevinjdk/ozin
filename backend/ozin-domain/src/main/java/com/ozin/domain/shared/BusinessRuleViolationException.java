package com.ozin.domain.shared;

/**
 * Se lanza cuando una operación incumple una regla de negocio.
 * El código corresponde al identificador de {@code docs/requirements/business-rules.md} (por ejemplo {@code RN-07}).
 */
public final class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String ruleCode, String message) {
        super(ruleCode, message);
    }
}

package com.ozin.infrastructure.shared.adapter.in.rest;

import com.ozin.domain.customer.Customer;
import com.ozin.domain.shared.AccessDeniedException;
import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.shared.DomainException;
import com.ozin.domain.shared.DomainValidationException;
import com.ozin.domain.shared.InvalidCredentialsException;
import com.ozin.domain.shared.InvalidStateTransitionException;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.shared.TooManyAttemptsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Traduce las excepciones a respuestas RFC 9457 ({@code application/problem+json}).
 *
 * <p>Cada respuesta incluye un {@code code} estable para que el frontend reaccione sin
 * interpretar el texto. Los errores inesperados nunca exponen detalles internos.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Reglas que indican un conflicto con el estado actual; se pueden resolver eligiendo otro dato. */
    private static final Set<String> CONFLICT_RULES =
            Set.of("RN-01", Customer.EMAIL_ALREADY_REGISTERED);

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    ProblemDetail handleTooManyAttempts(TooManyAttemptsException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, ex);
    }

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleValidation(DomainValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidStateTransitionException ex) {
        return problem(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        HttpStatus status = CONFLICT_RULES.contains(ex.code()) ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY;
        return problem(status, ex);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentModification(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "El recurso fue modificado por otra operación. Vuelve a intentarlo.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos", ex);
        return problem(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "La operación entra en conflicto con los datos existentes");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrió un error inesperado");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos inválidos");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, DomainException ex) {
        return problem(status, ex.code(), ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}

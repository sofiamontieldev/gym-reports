package com.epam.gymreports.exception;

import com.epam.gymreports.exception.ReportNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class
    })
    ResponseEntity<ProblemDetail> badRequest(
            Exception exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(ReportNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(
            ReportNotFoundException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetail> databaseUnavailable(
            DataAccessException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MongoDB is not available",
                request);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail,
            HttpServletRequest request) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(status).body(problem);
    }
}
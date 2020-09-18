package com.streamer.extractor.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

import javax.security.sasl.AuthenticationException
import javax.xml.bind.ValidationException

@RestControllerAdvice
class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    ResponseEntity handleValidationException(ValidationException ve) {
        return new ResponseEntity(ve.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity handleAuthenticationException(AuthenticationException ve) {
        return new ResponseEntity(HttpStatus.UNAUTHORIZED)
    }

}

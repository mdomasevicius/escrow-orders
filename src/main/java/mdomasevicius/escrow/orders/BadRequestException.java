package mdomasevicius.escrow.orders;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
class BadRequestException extends RuntimeException {
}

package mdomasevicius.escrow.orders;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
class NotFoundException extends RuntimeException {
}

package mdomasevicius.escrow.orders;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URI;

@Data
@AllArgsConstructor
class Link {

    private String rel;
    private URI href;

}

package mdomasevicius.escrow.orders;

import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RequestMapping("/api/orders")
@RestController
class OrderResource {

    @GetMapping
    void search() {

    }

    @PostMapping
    void requestItem(
            @RequestHeader("User") String user,
            @RequestBody OrderRequest request
    ) {

    }

    @PutMapping("/{orderId}/payment")
    void pay() {

    }

    @PutMapping("/{orderId}/deliver")
    void deliver() {

    }

    @Data
    class OrderRequest {

        @NotBlank
        private String item;

        @NotNull
        private BigDecimal price;
    }
}

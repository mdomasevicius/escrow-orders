package mdomasevicius.escrow.orders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.*;
import static mdomasevicius.escrow.orders.Order.State.PAID;
import static mdomasevicius.escrow.orders.Order.State.PENDING;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;

@RequestMapping("/api/orders")
@RestController
class OrderResource {

    private final Orders orders;

    OrderResource(Orders orders) {
        this.orders = orders;
    }

    @GetMapping("/{id}")
    OrderResponse findOne(@PathVariable Long id) {
        return OrderResponse.toDto(orders.findOne(id));
    }


    @GetMapping
    Set<OrderResponse> list(@RequestHeader("User") String user) {
        return orders.findAll(user)
                .stream()
                .map(OrderResponse::toDto)
                .collect(toSet());
    }

    @PostMapping
    ResponseEntity createOrder(
            @RequestHeader("User") String buyer,
            @RequestBody OrderRequest request
    ) {
        Long id = orders.create(Order.builder()
                .buyer(buyer)
                .item(request.getItem())
                .price(request.getPrice())
                .created(now())
                .state(PENDING)
                .build());

        URI orderLocation = linkTo(methodOn(OrderResource.class).findOne(id)).toUri();
        return created(orderLocation).build();
    }

    @PutMapping("/{orderId}/payment")
    ResponseEntity pay(@PathVariable Long orderId) {
        orders.completePayment(orderId);
        return noContent().build();
    }

    @PutMapping("/{orderId}/deliver")
    ResponseEntity deliverItem(
            @RequestHeader("User") String seller,
            @PathVariable Long orderId
    ) {
        orders.completeOrder(orderId, seller);
        return noContent().build();
    }

    @Data
    static class OrderRequest {

        @NotBlank
        private String item;

        @NotNull
        private BigDecimal price;
    }

    @Data
    @Builder
    static class OrderResponse {
        private Long id;
        private String item;
        private BigDecimal price;
        private Order.State state;
        private LocalDateTime created;
        private String buyer;
        private String seller;

        @JsonProperty("_links")
        @JsonInclude(NON_EMPTY)
        private final List<Link> links = new ArrayList<>();

        private void addLink(Link link) {
            links.add(link);
        }

        static OrderResponse toDto(Order order) {
            OrderResponse response = OrderResponse.builder()
                    .id(order.getId())
                    .item(order.getItem())
                    .price(order.getPrice())
                    .state(order.getState())
                    .created(order.getCreated())
                    .buyer(order.getBuyer())
                    .seller(order.getSeller())
                    .build();

            if (order.getState() == PENDING) {
                response.addLink(
                        new Link(
                                "completePayment",
                                linkTo(methodOn(OrderResource.class).pay(order.getId())).toUri()));
            }

            if (order.getState() == PAID) {
                response.addLink
                        (new Link(
                                "deliverItem",
                                linkTo(methodOn(OrderResource.class).deliverItem(null, order.getId())).toUri()));
            }

            return response;
        }
    }
}

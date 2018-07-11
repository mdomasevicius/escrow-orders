package mdomasevicius.escrow.orders;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.SEQUENCE;
import static mdomasevicius.escrow.orders.Order.State.*;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
class Order {

    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;
    private String item;
    private BigDecimal price;
    private String buyer;
    private String seller;
    private State state;
    private LocalDateTime created;

    void paymentCompleted() {
        if (state == PENDING) {
            this.state = PAID;
        } else {
            throw new BadRequestException();
        }
    }

    void orderCompleted(String seller) {
        if (state == PAID) {
            this.state = COMPLETED;
            this.seller = seller;
        } else {
            throw new BadRequestException();
        }
    }

    enum State {
        PENDING, PAID, COMPLETED
    }
}

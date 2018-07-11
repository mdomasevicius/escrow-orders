package mdomasevicius.escrow.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
interface OrderRepo extends JpaRepository<Order, Long> {
    Set<Order> findByBuyer(String buyer);
}

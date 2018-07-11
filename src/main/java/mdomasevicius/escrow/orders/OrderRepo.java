package mdomasevicius.escrow.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface OrderRepo extends JpaRepository<Order, Long> { }

package mdomasevicius.escrow.orders;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
class Orders {

    private final OrderRepo repo;

    Orders(OrderRepo repo) {
        this.repo = repo;
    }

    @Transactional
    public Long create(Order order) {
        return repo.save(order).getId();
    }

    @Transactional(readOnly = true)
    public Order findOne(Long id) {
        return repo.findById(id)
                .orElseThrow(NotFoundException::new);
    }

    @Transactional
    public void completePayment(Long id) {
        repo.findById(id)
                .orElseThrow(NotFoundException::new)
                .paymentCompleted();
    }

    @Transactional
    public void completeOrder(Long id) {
        repo.findById(id)
                .orElseThrow(NotFoundException::new)
                .orderCompleted();
    }

    @Transactional(readOnly = true)
    public Set<Order> findAll(String user) {
        return repo.findByBuyer(user);
    }
}

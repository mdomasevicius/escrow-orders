package mdomasevicius.escrow.orders;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static mdomasevicius.escrow.orders.Order.State.PENDING;

@Service
@Slf4j
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
    public void completeOrder(Long id, String seller) {
        repo.findById(id)
                .orElseThrow(NotFoundException::new)
                .orderCompleted(seller);
    }

    @Transactional(readOnly = true)
    public Set<Order> findAll(String user) {
        return repo.findByBuyer(user);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${escrow.schedule.orderRemovalInMills:60000}")
    public void removeUnpaidOrders() {
        Set<Order> toDelete = repo.findAll()
                .stream()
                .filter(o -> o.getCreated().isBefore(now().minusHours(2)) && o.getState() == PENDING)
                .collect(Collectors.toSet());

        if (toDelete.size() > 0) {
            log.info("Found {} stale orders - removing.", toDelete.size());
        }

        repo.deleteAll(toDelete);
    }
}

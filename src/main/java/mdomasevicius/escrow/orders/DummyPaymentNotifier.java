package mdomasevicius.escrow.orders;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DummyPaymentNotifier implements PaymentNotifier {

    @Override
    public void paymentCompleted(Order order) {
        log.info("Payment for Order Id `{}` was completed. Notifying Seller `{}`!", order.getId(), order.getSeller());
    }

}

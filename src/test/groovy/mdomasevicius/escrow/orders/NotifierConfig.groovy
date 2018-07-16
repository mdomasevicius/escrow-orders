package mdomasevicius.escrow.orders

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class NotifierConfig {

    @Bean
    @Primary
    PaymentNotifier paymentNotifier() {
        return new TestPaymentNotifier()
    }


    static class TestPaymentNotifier implements PaymentNotifier {

        List<Order> paidOrders = []

        @Override
        void paymentCompleted(Order order) {
            paidOrders << order
        }
    }

}

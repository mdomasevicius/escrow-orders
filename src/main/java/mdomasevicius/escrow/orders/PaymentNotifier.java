package mdomasevicius.escrow.orders;

public interface PaymentNotifier {
    void paymentCompleted(Order order);
}

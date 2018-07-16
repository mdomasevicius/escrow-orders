package mdomasevicius.escrow.orders

import mdomasevicius.escrow.App
import mdomasevicius.escrow.orders.Order.State
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime

import static java.time.LocalDateTime.*
import static mdomasevicius.escrow.orders.NotifierConfig.TestPaymentNotifier
import static mdomasevicius.escrow.orders.Order.State.*
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.http.HttpStatus.*

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = App)
class OrderResourceSpec extends Specification {

    @Autowired
    RestClient rest

    @Autowired
    TestPaymentNotifier paymentNotifier

    @Autowired
    OrderRepo orderRepo

    @Autowired
    Orders orders

    def 'can create order'() {
        when:
            def response = rest.post(
                '/api/orders',
                [
                    item  : 'Magic Sword of Chuck Norris',
                    price : 199.99,
                    seller: 'Seller1'],
                'User1')
        then:
            response.statusCode == CREATED
            response.headers.Location

        when:
            def orderId = resolveCreatedResourceId(response)
            def orderResponse = rest.get("/api/orders/$orderId", 'User1')
        then:
            orderResponse.statusCode == OK
            orderResponse.body.item == 'Magic Sword of Chuck Norris'
            orderResponse.body.price == 199.99
            orderResponse.body.state == 'PENDING'
            orderResponse.body.buyer == 'User1'
            orderResponse.body.seller == 'Seller1'
            orderResponse.body._links.count { it.rel == 'completePayment' && it.href } == 1
    }

    def 'can only list items by specific user'() {
        given:
            10.times { createOrder('UserOne') }
            20.times { createOrder('UserTwo') }

        expect:
            rest.get('/api/orders', 'UserOne').body.size() == 10
            rest.get('/api/orders', 'UserTwo').body.size() == 20
    }

    def 'can complete order payment'() {
        given:
            def orderId = createOrderAndGetId()

        when:
            def response = rest.put("/api/orders/$orderId/payment")
        then:
            response.statusCode == NO_CONTENT

        when:
            def orderResponse = rest.get("/api/orders/$orderId")
        then:
            orderResponse.statusCode == OK
            orderResponse.body.state == 'PAID'
            orderResponse.body._links.count { it.rel == 'deliverItem' && it.href } == 1
    }

    def 'can deliver order item'() {
        given:
            def orderId = createPaidOrderAndGetId('Buyer123', 'Seller123')

        when:
            def response = rest.put("/api/orders/$orderId/deliver", 'Seller123')
        then:
            response.statusCode == NO_CONTENT

        when:
            def orderResponse = rest.get("/api/orders/$orderId", 'Buyer123')
        then:
            orderResponse.statusCode == OK
            orderResponse.body.state == 'COMPLETED'
    }

    def 'can not deliver item until order is unpaid'() {
        given:
            def orderId = createOrderAndGetId()
        when:
            def response = rest.put("/api/orders/$orderId/deliver")
        then:
            response.statusCode == BAD_REQUEST
    }

    def 'can not deliver item twice'() {
        given:
            def orderId = createPaidOrderAndGetId('User1337', 'Seller1337')
        expect:
            rest.put("/api/orders/$orderId/deliver", 'Seller1337').statusCode == NO_CONTENT
            rest.put("/api/orders/$orderId/deliver", 'Seller1337').statusCode == BAD_REQUEST
    }

    def 'can perform payment twice'() {
        given:
            def orderId = createOrderAndGetId('UserOne')
        expect:
            rest.put("/api/orders/$orderId/payment", 'UserOne').statusCode == NO_CONTENT
            rest.put("/api/orders/$orderId/payment", 'UserOne').statusCode == BAD_REQUEST
    }

    def 'can not perform payment or item delivery on completed order'() {
        given:
            def orderId = createCompletedOrder()
        expect:
            rest.put("/api/orders/$orderId/deliver").statusCode == BAD_REQUEST
            rest.put("/api/orders/$orderId/payment").statusCode == BAD_REQUEST
    }

    def 'can not single fetch other\'s resource'() {
        given:
            def orderA = createOrderAndGetId('UserA')
            createOrder('UserB')

        when:
            def forbiddenResponse = rest.get("/api/orders/$orderA", "UserB")
        then:
            forbiddenResponse.statusCode == FORBIDDEN

        when:
            def okResponse = rest.get("/api/orders/$orderA", "UserA")
        then:
            okResponse.statusCode == OK
    }

    def 'payment performer must be tracked'() {
        expect:
            rest.get("/api/orders/${createPaidOrderAndGetId('h3h3')}", 'h3h3')
                .body.paidBy == 'h3h3'
    }

    def 'seller must be notified when order is paid'() {
        when:
            def orderId = createPaidOrderAndGetId('User1337', 'Seller1337')
        then:
            paymentNotifier.paidOrders
                .find { it.id == orderId && it.buyer == 'User1337' && it.seller == 'Seller1337' }
    }

    @Unroll
    def 'when payload is #payload then validation must fail for fields #missingFields'() {
        given:
            def response = rest.post('/api/orders', payload)
        expect:
            response.statusCode == expectedStatusCode
            response.body.errors.count { it.field in missingFields } == missingFields.size()
        where:
            payload             || missingFields       | expectedStatusCode
            [item: 'item']      || ['seller', 'price'] | BAD_REQUEST
            [price: 19.99]      || ['seller', 'item']  | BAD_REQUEST
            [seller: 'Seller1'] || ['item', 'price']   | BAD_REQUEST
    }

    def 'unpaid deals are removed after 2 hours'() {
        given:
            persistNewOrder('Buyer1234', 'Expire', PENDING)
        when:
            orders.removeUnpaidOrders()
            def response = rest.get('/api/orders', 'Buyer1234')
        then:
            !response.body
    }

    def 'unpaid deals do not get removed earlier'() {
        given:
            persistNewOrder('b2b', 'Expire', PENDING, now().minusHours(1))
        when:
            orders.removeUnpaidOrders()
            def response = rest.get('/api/orders', 'b2b')
        then:
            response.body.count { it.name = 'Expire' && it.buyer == 'b2b' } == 1
    }

    def 'only pending deals can get removed'() {
        given:
            persistNewOrder('leeroy', 'Expire 1', PENDING)
            persistNewOrder('leeroy', 'Expire 2', COMPLETED)
            persistNewOrder('leeroy', 'Expire 3', PAID)
        when:
            orders.removeUnpaidOrders()
            def response = rest.get('/api/orders', 'leeroy')
        then:
            response.body.count { it.item in ['Expire 1', 'Expire 2', 'Expire 3'] } == 2
    }

    private Order persistNewOrder(String buyer, String item, State state, LocalDateTime createdAt = now().minusHours(2)) {
        return orderRepo.save(new Order(
            item: item,
            price: 1,
            buyer: buyer,
            seller: 'Seller1234',
            created: createdAt,
            state: state))
    }

    private void createOrder(String user = 'anonymous', String seller = 'seller') {
        rest.post('/api/orders', [item: 'random', price: 10, seller: seller], user)
    }

    private Long createOrderAndGetId(String user = 'anonymous', String seller = 'seller') {
        return resolveCreatedResourceId(rest.post('/api/orders', [item: 'random', price: 10, seller: seller], user))
    }

    private Long createPaidOrderAndGetId(String user = 'anonymous', String seller = 'seller') {
        def orderId = createOrderAndGetId(user, seller)
        assert rest.put("/api/orders/$orderId/payment", user).statusCode == NO_CONTENT
        return orderId
    }

    private Long createCompletedOrder(String user = 'anonymous', String seller = 'seller') {
        def orderId = createPaidOrderAndGetId(user, seller)
        assert rest.put("/api/orders/$orderId/deliver", seller).statusCode == NO_CONTENT
        return orderId
    }

    private static Long resolveCreatedResourceId(response) {
        def resourceId = response.headers.Location.find().split('/').last()
        assert resourceId
        return resourceId as Long
    }
}

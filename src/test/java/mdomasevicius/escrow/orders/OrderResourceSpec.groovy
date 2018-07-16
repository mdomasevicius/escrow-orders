package mdomasevicius.escrow.orders

import mdomasevicius.escrow.App
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.http.HttpStatus.*

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = App)
class OrderResourceSpec extends Specification {

    @Autowired
    RestClient rest

    def 'can create order'() {
        when:
            def response = rest.post(
                '/api/orders',
                [item: 'Magic Sword of Chuck Norris', price: 199.99],
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
            !orderResponse.body.seller
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
            def orderId = createPaidOrderAndGetId()

        when:
            def response = rest.put("/api/orders/$orderId/deliver", 'UserTwo')
        then:
            response.statusCode == NO_CONTENT

        when:
            def orderResponse = rest.get("/api/orders/$orderId")
        then:
            orderResponse.statusCode == OK
            orderResponse.body.state == 'COMPLETED'
            orderResponse.body.seller == 'UserTwo'
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
            def orderId = createPaidOrderAndGetId()
        expect:
            rest.put("/api/orders/$orderId/deliver").statusCode == NO_CONTENT
            rest.put("/api/orders/$orderId/deliver").statusCode == BAD_REQUEST
    }

    def 'can perform payment twice'() {
        given:
            def orderId = createOrderAndGetId('UserOne')
        expect:
            rest.put("/api/orders/$orderId/payment").statusCode == NO_CONTENT
            rest.put("/api/orders/$orderId/payment").statusCode == BAD_REQUEST
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

    private void createOrder(String user = 'anonymous') {
        rest.post('/api/orders', [item: 'random', price: 10], user)
    }

    private Long createOrderAndGetId(String user = 'anonymous') {
        return resolveCreatedResourceId(rest.post('/api/orders', [item: 'random', price: 10], user))
    }

    private Long createPaidOrderAndGetId(String user = 'anonymous') {
        def orderId = createOrderAndGetId(user)
        assert rest.put("/api/orders/$orderId/payment", user).statusCode == NO_CONTENT
        return orderId
    }

    private Long createCompletedOrder(String user = 'anonymous') {
        def orderId = createPaidOrderAndGetId(user)
        assert rest.put("/api/orders/$orderId/deliver", user).statusCode == NO_CONTENT
        return orderId
    }

    private static Long resolveCreatedResourceId(response) {
        def resourceId = response.headers.Location.find().split('/').last()
        assert resourceId
        return resourceId as Long
    }
}

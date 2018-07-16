package mdomasevicius.escrow.orders

import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.HttpMethod.PUT
import static org.springframework.util.CollectionUtils.toMultiValueMap

/**
 * This is just a wrapper for Spring {@link TestRestTemplate} since it's API is FUGLY
 */
@Component
class RestClient {

    private final TestRestTemplate rest

    RestClient(TestRestTemplate rest) {
        this.rest = rest
    }

    ResponseEntity<Map> post(String url, Object body, String user = 'anonymous') {
        return rest.postForEntity(url, new HttpEntity(body, toMultiValueMap([User: [user]])), LinkedHashMap)
    }

    ResponseEntity<Object> get(String url, String user = 'anonymous') {
        def entity = new RequestEntity(toMultiValueMap([User: [user]]), GET, new URI(url))
        return rest.exchange(entity, Object)
    }

    ResponseEntity<Map> put(String url, String user = 'anonymous') {
        def entity = new RequestEntity([:], toMultiValueMap([User: [user]]), PUT, new URI(url))
        return rest.exchange(entity, LinkedHashMap)
    }
}

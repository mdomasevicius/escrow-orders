[![Build Status](https://travis-ci.org/mdomasevicius/escrow-orders.svg?branch=master)](https://travis-ci.org/mdomasevicius/escrow-orders)

# Escrow Ordering

A simple app, which allows to:

* Create Order for an Item
* Pay for the Order
* Complete Order by delivering Item
* List Your Own Orders

For simplicity - security is emulated via header `User`.

## Pre-requisits

* OpenJDK version 1.8.0_171 (Oracle JDK same version should work)
* Docker version 17.12.1-ce, build 7390fc6
* docker-compose version 1.17.1

## How to launch

While in project root directory execute: `$ ./gradlew build`  
This will compile the code, run tests and produce deployable `JAR`.

To run the app type in: `$ docker-compose up --build` - this   
will build docker image and then start a container on port `8080`.

### IDE

You can also open this project in what is probably the only IDE worth attention on the market - [IDEA](https://www.jetbrains.com/idea/).
Just follow the steps on how to import `Gradle` projects and everything should be ok.
You will need to install `Lombok` plugin and tick `Enable annotation processing` in IDE settings,
IDEA will provide you with useful hint bubbles on how to do most of that.

Entry point is - `mdomasevicius.escrow.App`

## Flow snippets

### Create Order

`$ curl -d '{"item": "Speed Boots", "price": 199.65}' -H "Content-Type: application/json" -H "User: John" -X POST http://localhost:8080/api/orders`

### List Orders

`$ curl -H "Content-Type: application/json" -H "User: John" http://localhost:8080/api/orders`

### Complete Order Payment

`$ curl -H "Content-Type: application/json" -H "User: John" -X PUT http://localhost:8080/api/orders/1/payment`

### Deliver Item
`$ curl -H "Content-Type: application/json" -H "User: Wick" -X PUT http://localhost:8080/api/orders/1/deliver`

## Acknowledgments

* Groovy/Spock is used for testing due to how well it removes noise from the code
* Spring Boot was chosen for convenience. For such a small app it is overkill, however IOC,  
REST and Scheduling is provided out of the box to name a few. 
* Swagger for some exposure of REST docs. Not a fan personally since it has somewhat poor integration. Watch out for  
awkward response bodies, they are Spring framework actual body wrappers. URL: `http://localhost:8080/swagger-ui.html`
* Docker for minimal fuss app launching plus docker is all the rage now.
* Java + Lombok - this combinations seems to confuse people the least compared to going only Groovy or Kotlin.
* Embedded H2 DB is used along with Hibernate. Again this is overkill but Spring boot provides integration
out of the box with minimal fuss.
* You can check out test reports after running `gw clean build` in `<project_root>/build/reports/tests/test/index.html`

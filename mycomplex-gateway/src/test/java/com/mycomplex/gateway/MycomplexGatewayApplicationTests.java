package com.mycomplex.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth0.test/",
        "spring.security.oauth2.resourceserver.jwt.audiences=https://api.mycomplex.test",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth0.test/.well-known/jwks.json"
})
class MycomplexGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
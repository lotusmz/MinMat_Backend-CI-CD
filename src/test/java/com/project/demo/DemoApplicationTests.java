package com.project.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Disabled;

@Disabled("Deshabilitado temporalmente hasta estabilizar configuración de test")
@ActiveProfiles("test")
@SpringBootTest
class DemoApplicationTests {
    @Test void contextLoads() {}
}


package com.test.alo.split_bill_api.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloControllerTest {

    private final HelloController controller = new HelloController();

    @Test
    void testHello() {
        assertEquals("Hello, World!", controller.hello());
    }

    @Test
    void testHelloJson() {
        Map<String, String> response = controller.helloJson("Developer");
        assertEquals("Hello, Developer!", response.get("message"));
    }
}


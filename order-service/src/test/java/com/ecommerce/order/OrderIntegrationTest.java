package com.ecommerce.order;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "services.cart.url=http://localhost:${wiremock.server.port}/api/cart",
        "services.product.url=http://localhost:${wiremock.server.port}/api/products"
})
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
public class OrderIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Giả lập RabbitTemplate để không gửi thật lên message broker khi test
    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void testCreateOrder_Success_LuotNghiepVuLienServer() throws Exception {
        Long userId = 1L;
        Long productId = 100L;

        // 1. Giả lập (Mock) phản hồi từ cart-service
        String cartResponseJson = """
                [
                    {
                        "id": 1,
                        "userId": 1,
                        "productId": 100,
                        "quantity": 2
                    }
                ]
                """;
        stubFor(get(urlEqualTo("/api/cart/user/" + userId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(cartResponseJson)));

        // 2. Giả lập phản hồi từ product-service
        String productResponseJson = """
                {
                    "id": 100,
                    "name": "Laptop",
                    "description": "Gaming Laptop",
                    "price": 1500.0,
                    "stockQuantity": 10,
                    "createdAt": "2023-01-01T00:00:00"
                }
                """;
        stubFor(get(urlEqualTo("/api/products/" + productId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(productResponseJson)));

        // 3. Chuẩn bị request tạo đơn hàng
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OrderRequest> request = new HttpEntity<>(orderRequest, headers);

        // 4. Gửi HTTP Request tạo đơn hàng tới order-service
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders",
                request,
                OrderResponse.class
        );

        // 5. Kiểm tra kết quả
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        OrderResponse orderResponse = response.getBody();
        assertNotNull(orderResponse);
        assertNotNull(orderResponse.getId());
        assertEquals(userId, orderResponse.getUserId());
        assertEquals("COMPLETED", orderResponse.getStatus());
        
        // Tổng tiền = 2 * 1500 = 3000
        assertEquals(3000.0, orderResponse.getTotalAmount());
        assertEquals(1, orderResponse.getItems().size());
        assertEquals(productId, orderResponse.getItems().get(0).getProductId());
        
        // RabbitTemplate (đã mock) sẽ không lỗi, hệ thống test luồng thành công.
    }
}

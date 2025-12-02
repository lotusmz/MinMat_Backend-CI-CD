package com.project.demo.rest.order;

import com.project.demo.logic.entity.order.Order;
import com.project.demo.logic.entity.order.OrderRepository;
import com.project.demo.logic.entity.user.User;
import com.project.demo.logic.entity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderAPITest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderRestController orderRestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderRestController)
                .alwaysDo(print())
                .build();
    }

    @Test
    void getAllOrders_success() throws Exception {
        Order order1 = new Order();
        order1.setId(1L);
        order1.setDescription("Orden 1");
        order1.setTotal(100.0);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setDescription("Orden 2");
        order2.setTotal(200.0);

        List<Order> orders = Arrays.asList(order1, order2);
        Page<Order> page = new PageImpl<>(orders, PageRequest.of(0, 10), orders.size());

        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/orders")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.meta.totalElements").value(2))
                .andExpect(jsonPath("$.meta.pageNumber").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(10));

        verify(orderRepository, times(1)).findAll(any(Pageable.class));
    }


    //FALLO INTENCIONAL: página inválida
    @Test
    void getAllOrders_invalidPage_returnsServerError() throws Exception {

        MvcResult result = mockMvc.perform(get("/orders")
                        .param("page", "0")   // inválido
                        .param("size", "10"))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();

        assertEquals(
                500,
                status,
                "FALLO INTENCIONAL: Se esperaba que el controlador devolviera error 500 " +
                        "porque la página '0' es inválida. Pero devolvió " + status +
                        ". Respuesta: " + body
        );

        verify(orderRepository, never()).findAll(any(Pageable.class));
    }

    // (usuario existe y tiene órdenes)
    @Test
    void getAllOrdersByUser_success() throws Exception {
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setDescription("Orden U1");
        order1.setTotal(150.0);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setDescription("Orden U2");
        order2.setTotal(250.0);

        List<Order> orders = Arrays.asList(order1, order2);
        Page<Order> page = new PageImpl<>(orders, PageRequest.of(0, 10), orders.size());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(orderRepository.getOrderByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/orders/user/{userId}/orders", userId)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(2))
                .andExpect(jsonPath("$.meta.pageNumber").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(10));

        verify(userRepository, times(1)).findById(userId);
        verify(orderRepository, times(1)).getOrderByUserId(eq(userId), any(Pageable.class));
    }

    //FALLO INTENCIONAL: ID inválido
    @Test
    void getAllOrdersByUserID_invalidSize_returnsServerError() throws Exception {
        Long userId = 1L;

        MvcResult result = mockMvc.perform(get("/orders/user/{userId}/orders", userId)
                        .param("page", "1")
                        .param("size", "0"))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();

        assertEquals(
                500,
                status,
                "FALLO INTENCIONAL: Se esperaba un error 500 debido a que el tamaño '0' y ID no son válidos " +
                        "para paginación. El controlador devolvió " + status +
                        ". Respuesta: " + body
        );

        verify(orderRepository, never()).getOrderByUserId(anyLong(), any(Pageable.class));
    }

}

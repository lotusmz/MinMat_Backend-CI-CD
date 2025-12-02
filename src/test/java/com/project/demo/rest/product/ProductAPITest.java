package com.project.demo.rest.product;

import com.project.demo.logic.entity.product.Product;
import com.project.demo.logic.entity.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProductAPITest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductRestController productRestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(productRestController)
                .alwaysDo(print())
                .build();
    }

   // LISTAR PRODUCTOS CON page = 0 (dato erróneo) (Fallo Intencional)
    @Test
    void getAllProducts_invalidPage_returnsServerError() throws Exception {
        mockMvc.perform(get("/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is5xxServerError());

        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    // CREAR PRODUCTO (OK)
    @Test
    void addProduct_success() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Nuevo producto");
        product.setDescription("Desc");
        product.setPrice(new BigDecimal("19.99"));
        product.setStockQuantity(10);

        when(productRepository.save(any(Product.class))).thenReturn(product);

        String jsonBody = """
                {
                  "name": "Nuevo producto",
                  "description": "Desc",
                  "price": 19.99,
                  "stockQuantity": 10
                }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.name").value("Nuevo producto"))
                .andExpect(jsonPath("$.data.stockQuantity").value(10));

        verify(productRepository, times(1)).save(any(Product.class));
    }

  // OBTENER PRODUCTO POR ID QUE NO EXISTE (Fallo Intencional)
    @Test
    void getProductById_notFound_returnsServerError() throws Exception {
        Long idNoExistente = 99L;
        when(productRepository.findById(idNoExistente)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/{id}", idNoExistente))
                .andExpect(status().is5xxServerError());

        verify(productRepository, times(1)).findById(idNoExistente);
    }

    // ELIMINAR PRODUCTO (OK)
    @Test
    void deleteProduct_success() throws Exception {
        Long id = 1L;

        Product existing = new Product();
        existing.setId(id);
        existing.setName("Producto a eliminar");
        existing.setDescription("Desc");

        // El controlador hace un findById antes de eliminar
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));

        // verifica lo que realmente pasa en el controlador
        verify(productRepository, times(1)).findById(id);
        verify(productRepository, times(1)).deleteById(id);  // 👈 aquí estaba el problema
    }


}

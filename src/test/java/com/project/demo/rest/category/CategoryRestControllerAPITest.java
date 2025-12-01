package com.project.demo.rest.category;

import com.project.demo.logic.entity.category.Category;
import com.project.demo.logic.entity.category.CategoryRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CategoryRestControllerAPITest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryRestController categoryRestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryRestController)
                .alwaysDo(print())
                .build();
    }

    //LISTAR CATEGORÍAS
    @Test
    void getAllCategories_success() throws Exception {
        Category c1 = new Category();
        c1.setId(1L);
        c1.setName("Números");
        c1.setDescription("Categoría de números");

        List<Category> categorias = List.of(c1);
        Page<Category> page = new PageImpl<>(categorias, PageRequest.of(0, 10), categorias.size());

        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/categories")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Números"))
                .andExpect(jsonPath("$.meta.totalElements").value(1))
                .andExpect(jsonPath("$.meta.pageNumber").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(10));

        verify(categoryRepository, times(1)).findAll(any(Pageable.class));
    }


    //LISTAR CATEGORÍAS CON page = 0 (dato erróneo)
    @Test
    void getAllCategories_invalidPage_returnsServerError() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("page", "0")       // dato inválido
                        .param("size", "10"))
                .andExpect(status().is5xxServerError());

        verify(categoryRepository, never()).findAll(any(Pageable.class));
    }

    //CREAR CATEGORÍA
    @Test
    void addCategory_success() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setName("Números");
        category.setDescription("Categoría de números");

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        String jsonBody = """
                {
                  "name": "Números",
                  "description": "Categoría de números"
                }
                """;

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.data.name").value("Números"))
                .andExpect(jsonPath("$.data.description").value("Categoría de números"));

        verify(categoryRepository, times(1)).save(any(Category.class));
    }


    //OBTENER CATEGORÍA POR ID QUE NO EXISTE
    @Test
    void getCategoryById_notFound_returnsServerError() throws Exception {
        Long idNoExistente = 99L;
        when(categoryRepository.findById(idNoExistente)).thenReturn(Optional.empty());

        mockMvc.perform(get("/categories/{id}", idNoExistente))
                .andExpect(status().is5xxServerError()); // lanza RuntimeException

        verify(categoryRepository, times(1)).findById(idNoExistente);
    }
}

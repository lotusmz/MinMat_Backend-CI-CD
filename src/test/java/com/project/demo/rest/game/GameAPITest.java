package com.project.demo.rest.game;

import com.project.demo.logic.entity.game.Game;
import com.project.demo.logic.entity.game.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GameAPITest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameRestController gameRestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(gameRestController)
                .alwaysDo(print())
                .build();
    }

    // LISTAR JUEGOS
    @Test
    void getAllGames_success() throws Exception {
        Game g1 = new Game();
        g1.setId(1L);
        g1.setName("Juego 1");
        g1.setDescription("Desc 1");

        Game g2 = new Game();
        g2.setId(2L);
        g2.setName("Juego 2");
        g2.setDescription("Desc 2");

        when(gameRepository.findAll()).thenReturn(List.of(g1, g2));

        mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Juego 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Juego 2"));

        verify(gameRepository, times(1)).findAll();
    }

    // ACTUALIZAR UN JUEGO
    @Test
    void updateGame_success() throws Exception {
        Long gameId = 1L;

        Game existing = new Game();
        existing.setId(gameId);
        existing.setName("Viejo nombre");
        existing.setDescription("Vieja desc");

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        String jsonBody = """
                {
                  "name": "Nuevo nombre",
                  "description": "Nueva desc"
                }
                """;

        mockMvc.perform(put("/games/{id}", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo nombre"))
                .andExpect(jsonPath("$.description").value("Nueva desc"));

        verify(gameRepository, times(1)).findById(gameId);
        verify(gameRepository, times(1)).save(any(Game.class));
    }

    // ACTUALIZAR JUEGO NO EXISTENTE (Fallo Intencional)
    @Test
    void updateGame_notFound() throws Exception {
        Long gameId = 999L;

        // El repositorio responde vacío osea el juego no existe
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        String jsonBody = """
            {
              "name": "Intento de update",
              "description": "No debería actualizar"
            }
            """;

        MvcResult result = mockMvc.perform(put("/games/{id}", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        assertEquals(
                404,
                status,
                "FALLO INTENCIONAL: se esperaba HTTP 404 cuando el juego no existe, " +
                        "pero el controlador devolvió " + status + ". Respuesta: " + body
        );

        verify(gameRepository, times(1)).findById(gameId);
        verify(gameRepository, never()).save(any(Game.class));
    }

    // ACTUALIZAR JUEGO CON DATOS NO VALIDOS (Fallo Intencional)
    @Test
    void updateGame_invalidData() throws Exception {
        Long gameId = 1L;

        Game existing = new Game();
        existing.setId(gameId);
        existing.setName("Viejo nombre");
        existing.setDescription("Vieja desc");

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(existing));

        // Datos inválidos: nombre vacío
        String jsonBody = """
            {
              "name": "",
              "description": "Nueva desc válida"
            }
            """;

        MvcResult result = mockMvc.perform(put("/games/{id}", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();

        assertEquals(
                400,
                status,
                "FALLO INTENCIONAL: se esperaba HTTP 400 por datos inválidos en el update, " +
                        "pero el controlador devolvió " + status + ". Respuesta: " + body
        );

        verify(gameRepository, times(1)).findById(gameId);
        verify(gameRepository, never()).save(any(Game.class));
    }
}

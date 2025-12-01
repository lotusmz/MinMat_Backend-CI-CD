package com.project.demo.rest.mathleship;

import com.project.demo.logic.entity.mathleship.GridCell;
import com.project.demo.logic.entity.mathleship.GridCellRepository;
import com.project.demo.logic.entity.mathleship.Ship;
import com.project.demo.logic.entity.mathleship.ShipRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class MathleshipRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShipRepository shipRepository;

    @Mock
    private GridCellRepository gridCellRepository;

    @InjectMocks
    private MathleshipRestController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // -----------------------------------------------------------
    // TEST: /initialize
    // -----------------------------------------------------------
    @Test
    public void testInitializeBoard_ReturnsShips() throws Exception {
        // Arrange (Barcos simulados)
        Ship ship1 = new Ship();
        ship1.setId(1L);
        ship1.setSize(4);

        Ship ship2 = new Ship();
        ship2.setId(2L);
        ship2.setSize(3);

        when(shipRepository.save(any(Ship.class))).thenAnswer(inv -> {
            Ship s = inv.getArgument(0);
            s.setId(new Random().nextLong());
            return s;
        });

        // Act & Assert
        mockMvc.perform(get("/api/mathleship/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Must clear repositories + save new ships
        verify(shipRepository, atLeastOnce()).save(any(Ship.class));
        verify(gridCellRepository, atLeastOnce()).save(any(GridCell.class));
    }


    // -----------------------------------------------------------
    // TEST: /attack → HIT
    // -----------------------------------------------------------
    @Test
    public void testAttack_Hit() throws Exception {
        // Arrange: create ship with one cell at row 2 column 'B'
        GridCell cell = new GridCell(2, 'B', 1, 0);

        Ship ship = new Ship();
        ship.setId(1L);
        ship.setSize(1);
        ship.setCellsOccupied(new ArrayList<>(List.of(cell)));

        // Mock repository returns this ship
        when(shipRepository.findAll()).thenReturn(List.of(ship));

        // Act & Assert
        mockMvc.perform(post("/api/mathleship/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"row\":2,\"column\":\"B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHit").value(true))
                .andExpect(jsonPath("$.message").value("Hit!"));
    }


    // -----------------------------------------------------------
    // TEST: /attack → MISS
    // -----------------------------------------------------------
    @Test
    public void testAttack_Miss() throws Exception {
        // Arrange: ship at different location
        GridCell cell = new GridCell(4, 'D', 1, 0);

        Ship ship = new Ship();
        ship.setId(1L);
        ship.setCellsOccupied(List.of(cell));

        when(shipRepository.findAll()).thenReturn(List.of(ship));

        // Act & Assert
        mockMvc.perform(post("/api/mathleship/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"row\":1,\"column\":\"A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHit").value(false))
                .andExpect(jsonPath("$.message").value("Miss!"));
    }


    // -----------------------------------------------------------
    // TEST: /reset
    // -----------------------------------------------------------
    @Test
    public void testInitialize_CorrectShipSizes() throws Exception {
        String jsonResponse = mockMvc.perform(get("/api/mathleship/initialize"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> ships = mapper.readValue(jsonResponse, List.class);

        // Deben ser 4 barcos
        assertEquals(4, ships.size(), "El número de barcos debe ser exactamente 4");

        // Tamaños esperados
        List<Integer> expectedSizes = Arrays.asList(4, 3, 2, 1);

        List<Integer> actualSizes = ships.stream()
                .map(s -> (Integer) s.get("size"))
                .sorted(Collections.reverseOrder())
                .toList();

        // Comparar ambos
        assertEquals(expectedSizes, actualSizes,
                "Los tamaños de los barcos deben coincidir con [4,3,2,1]");
    }

    @Test
    public void testInitialize_NoOverlappingCells() throws Exception {
        // Ejecutar la inicialización real del tablero
        String jsonResponse = mockMvc.perform(get("/api/mathleship/initialize"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extraer todas las celdas de los barcos
        // (Usamos Jackson para parsear el JSON)
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> ships = mapper.readValue(jsonResponse, List.class);

        Set<String> usedCells = new HashSet<>();

        for (Map<String, Object> ship : ships) {
            List<Map<String, Object>> cells = (List<Map<String, Object>>) ship.get("cellsOccupied");

            for (Map<String, Object> cell : cells) {
                int row = (Integer) cell.get("row");
                String column = (String) cell.get("column");

                String key = row + "-" + column;

                // Si la celda ya existía en el set → hay solapamiento
                if (usedCells.contains(key)) {
                    fail("Se detectó un solapamiento en la celda: " + key);
                }

                usedCells.add(key);
            }
        }
    }
}


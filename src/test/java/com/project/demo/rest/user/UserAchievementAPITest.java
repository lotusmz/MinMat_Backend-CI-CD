package com.project.demo.rest.user;

import com.project.demo.logic.entity.achievement.Achievement;
import com.project.demo.logic.entity.achievement.AchievementRepository;
import com.project.demo.logic.entity.rol.Role;
import com.project.demo.logic.entity.rol.RoleEnum;
import com.project.demo.logic.entity.user.User;
import com.project.demo.logic.entity.user.UserRepository;
import com.project.demo.logic.entity.user_achievement.UserAchievement;
import com.project.demo.logic.entity.user_achievement.UserAchievementRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserAchievementAPITest {

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAchievementRestController userAchievementRestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(userAchievementRestController)
                .alwaysDo(print())
                .build();
    }

   //LISTAR TODOS LOS ACHIEVEMENTS CON page = 0 (dato erróneo) (Fallo Intencional)
    @Test
    void getAllAchievements_invalidPage_returnsServerError() throws Exception {
        mockMvc.perform(get("/achievements")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is5xxServerError());

        verify(achievementRepository, never()).findAll(any(Pageable.class));
    }

    //LISTAR ACHIEVEMENTS POR USUARIO
    @Test
    void getUserAchievementsByUserId_success() throws Exception {
        Long userId = 1L;

        // Usuario con rol para evitar NullPointer en getAuthorities()
        User user = new User();
        user.setId(userId);

        Role role = new Role();
        role.setId(1);
        role.setName(RoleEnum.USER);
        role.setDescription("Test role");
        user.setRole(role);

        Achievement achievement = new Achievement();
        achievement.setId(1L);
        achievement.setName("Logro 1");

        UserAchievement ua = new UserAchievement();
        ua.setId(1L);
        ua.setUser(user);
        ua.setAchievement(achievement);
        ua.setAchievedAt(Date.from(Instant.now()));

        Page<UserAchievement> page = new PageImpl<>(List.of(ua), PageRequest.of(0, 10), 1);

        when(userAchievementRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/achievements/{userId}", userId)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User achievements retrieved successfully"))
                .andExpect(jsonPath("$.data[0].achievement.id").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(1));

        verify(userAchievementRepository, times(1)).findByUserId(eq(userId), any(Pageable.class));
    }

    // AGREGAR ACHIEVEMENT A UN USUARIO
    @Test
    void addAchievementToUser_success() throws Exception {
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Role role = new Role();
        role.setId(1);
        role.setName(RoleEnum.USER);
        role.setDescription("Test role");
        user.setRole(role);

        Achievement achievement = new Achievement();
        achievement.setId(1L);
        achievement.setName("Logro 1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(achievement));
        when(userAchievementRepository.save(any(UserAchievement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String jsonBody = """
            {
              "achievement": { "id": 1 },
              "achievedAt": "2024-01-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/achievements/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Achievement added successfully"))
                .andExpect(jsonPath("$.data.achievement.id").value(1));

        verify(userRepository, times(1)).findById(userId);
        verify(achievementRepository, times(1)).findById(1L);
        verify(userAchievementRepository, times(1)).save(any(UserAchievement.class));
    }

    // AGREGAR ACHIEVEMENT CON DATOS ERRÓNEOS (Fallo Intencional)
    @Test
    void addAchievementToUser_invalidData_returnsBadRequest() throws Exception {
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Role role = new Role();
        role.setId(1);
        role.setName(RoleEnum.USER);
        role.setDescription("Test role");
        user.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String jsonBody = """
        {
          "achievement": {},
          "achievedAt": "2024-01-01T00:00:00Z"
        }
        """;

        MvcResult result = mockMvc.perform(post("/achievements/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body  = result.getResponse().getContentAsString();
        assertEquals(
                400,
                status,
                "FALLO INTENCIONAL: Los datos enviados son inválidos. " +
                        "Se esperaba HTTP 400 porque el JSON no contiene achievement.id, " +
                        "lo cual es obligatorio. " +
                        "Sin embargo, el controlador respondió " + status + ". " +
                        "Respuesta completa del servidor: " + body
        );

        verify(userRepository, times(1)).findById(userId);
        verify(achievementRepository, never()).findById(anyLong());
        verify(userAchievementRepository, never()).save(any(UserAchievement.class));
    }

}

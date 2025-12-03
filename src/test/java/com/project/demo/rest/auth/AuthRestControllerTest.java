package com.project.demo.rest.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.logic.entity.auth.AuthenticationService;
import com.project.demo.logic.entity.auth.JwtService;
import com.project.demo.logic.entity.rol.Role;
import com.project.demo.logic.entity.rol.RoleEnum;
import com.project.demo.logic.entity.rol.RoleRepository;
import com.project.demo.logic.entity.user.LoginResponse;
import com.project.demo.logic.entity.user.User;
import com.project.demo.logic.entity.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para AuthRestController
 */
@WebMvcTest(AuthRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mocks de todos los beans usados en el controller
    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    // ---------- /auth/login ----------

    @Test
    void authenticate_returnsTokenAndUser_whenCredentialsAreValid() throws Exception {
        User requestUser = new User();
        requestUser.setEmail("test@example.com");
        requestUser.setPassword("123456");

        User authenticatedUser = new User();
        authenticatedUser.setEmail("test@example.com");

        given(authenticationService.authenticate(any(User.class))).willReturn(authenticatedUser);
        given(jwtService.generateToken(authenticatedUser)).willReturn("fake-jwt-token");
        given(jwtService.getExpirationTime()).willReturn(3600L);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(authenticatedUser));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestUser))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("fake-jwt-token")))
                .andExpect(jsonPath("$.expiresIn", is(3600)))
                .andExpect(jsonPath("$.authUser.email", is("test@example.com")));
    }

    // ---------- /auth/signup ----------

    @Test
    void registerUser_returnsConflict_whenEmailAlreadyExists() throws Exception {
        User existing = new User();
        existing.setEmail("dup@example.com");

        given(userRepository.findByEmail("dup@example.com")).willReturn(Optional.of(existing));

        User request = new User();
        request.setEmail("dup@example.com");
        request.setPassword("123456");

        mockMvc.perform(
                        post("/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(content().string("Email already in use"));
    }

    @Test
    void registerUser_createsUser_whenEmailNotExists() throws Exception {
        User request = new User();
        request.setEmail("new@example.com");
        request.setPassword("123456");

        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("123456")).willReturn("encoded");
        given(roleRepository.findByName(RoleEnum.USER)).willReturn(Optional.of(new Role()));

        User saved = new User();
        saved.setId(1L);
        saved.setEmail("new@example.com");
        given(userRepository.save(any(User.class))).willReturn(saved);

        mockMvc.perform(
                        post("/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("new@example.com")));

        // opcional: verificar que se encriptó password
        verify(passwordEncoder).encode("123456");
    }

    // ---------- /auth/request-password-reset ----------

    @Test
    void requestPasswordReset_sendsEmail_whenUserExists() throws Exception {
        User user = new User();
        user.setEmail("reset@example.com");

        given(userRepository.findByEmail("reset@example.com")).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> body = Map.of("email", "reset@example.com");

        mockMvc.perform(
                        post("/auth/request-password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Correo enviado"));

        // verificamos que se mandó correo
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        // correo al email correcto
        assert sent.getTo()[0].equals("reset@example.com");
    }

    @Test
    void requestPasswordReset_returnsNotFound_whenUserDoesNotExist() throws Exception {
        given(userRepository.findByEmail("nope@example.com")).willReturn(Optional.empty());

        Map<String, String> body = Map.of("email", "nope@example.com");

        mockMvc.perform(
                        post("/auth/request-password-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("Correo no registrado"));
    }

    // ---------- /auth/reset-password ----------

    @Test
    void resetPassword_updatesPassword_whenTokenValid() throws Exception {
        User user = new User();
        user.setResetToken("token-123");

        given(userRepository.findByResetToken("token-123")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPass")).willReturn("encodedNew");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> body = Map.of(
                "token", "token-123",
                "newPassword", "newPass"
        );

        mockMvc.perform(
                        post("/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Contraseña actualizada"));

        verify(passwordEncoder).encode("newPass");
        // opcional: asegurar que se limpió el token
        assert user.getResetToken() == null;
    }

    @Test
    void resetPassword_returnsNotFound_whenTokenInvalid() throws Exception {
        given(userRepository.findByResetToken("bad-token")).willReturn(Optional.empty());

        Map<String, String> body = Map.of(
                "token", "bad-token",
                "newPassword", "whatever"
        );

        mockMvc.perform(
                        post("/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("Token inválido"));
    }

    // ---------- /auth/check-email ----------

    @Test
    void checkEmailExists_returnsConflict_whenEmailExists() throws Exception {
        given(userRepository.findByEmail("exists@example.com"))
                .willReturn(Optional.of(new User()));

        mockMvc.perform(
                        get("/auth/check-email")
                                .param("email", "exists@example.com")
                )
                .andExpect(status().isConflict())
                .andExpect(content().string("Email already in use"));
    }

    @Test
    void checkEmailExists_returnsOk_whenEmailAvailable() throws Exception {
        given(userRepository.findByEmail("free@example.com"))
                .willReturn(Optional.empty());

        mockMvc.perform(
                        get("/auth/check-email")
                                .param("email", "free@example.com")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Email available"));
    }
}

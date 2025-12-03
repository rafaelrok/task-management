package br.com.rafaelvieira.taskmanagement.web.controller;

import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final String VIEW_LOGIN = "auth/login";
    private static final String VIEW_REGISTER = "auth/register";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return VIEW_LOGIN;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return VIEW_REGISTER;
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("registerForm") RegisterForm form,
            Errors errors,
            RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return VIEW_REGISTER;
        }
        boolean invalid = false;
        if (userRepository.existsByUsername(form.getUsername())) {
            errors.rejectValue("username", "exists", "Username já em uso");
            invalid = true;
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            errors.rejectValue("email", "exists", "Email já em uso");
            invalid = true;
        }
        if (invalid) {
            return VIEW_REGISTER;
        }

        var user =
                User.builder()
                        .username(form.getUsername())
                        .email(form.getEmail())
                        .fullName(form.getFullName())
                        .password(passwordEncoder.encode(form.getPassword()))
                        .role(br.com.rafaelvieira.taskmanagement.domain.enums.Role.MEMBER)
                        .build();
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Cadastro realizado! Faça login.");
        return "redirect:/login";
    }

    @Data
    public static class RegisterForm {

        @NotBlank(message = "Username é obrigatório")
        private String username;

        @NotBlank(message = "Nome completo é obrigatório")
        private String fullName;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Formato de email inválido")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        private String password;
    }
}

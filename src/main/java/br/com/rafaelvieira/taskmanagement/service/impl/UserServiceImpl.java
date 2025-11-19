package br.com.rafaelvieira.taskmanagement.service.impl;

import br.com.rafaelvieira.taskmanagement.domain.model.Profile;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.repository.UserRepository;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import br.com.rafaelvieira.taskmanagement.web.dto.UserProfileForm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserService self;

    public UserServiceImpl(UserRepository userRepository, @Lazy UserService self) {
        this.userRepository = userRepository;
        this.self = self;
    }

    private static Path avatarsRoot() {
        return Path.of("src/main/resources/static/uploads/avatars");
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return null; // allow service to proceed without assignment in non-auth contexts/tests
        }
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileForm loadProfile() {
        User user = self.getCurrentUser(); // Call via self-injected proxy
        Profile p = user.getProfile();
        UserProfileForm form = new UserProfileForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setProfession(p.getProfession());
        form.setSkills(Optional.ofNullable(p.getSkills()).orElse(""));
        form.setSoftSkills(Optional.ofNullable(p.getSoftSkills()).orElse(""));
        form.setBio(p.getBio());
        form.setWebsiteUrl(p.getWebsiteUrl());
        form.setGithubUrl(p.getGithubUrl());
        form.setLinkedinUrl(p.getLinkedinUrl());
        form.setLocation(p.getLocation());
        form.setExperienceLevel(p.getExperienceLevel());
        form.setPrimaryStack(p.getPrimaryStack());
        form.setAvailability(p.getAvailability());
        form.setAvatarUrl(p.getAvatarUrl());
        // GitHub fields
        form.setGithubLogin(p.getGithubLogin());
        form.setGithubName(p.getGithubName());
        form.setGithubCompany(p.getGithubCompany());
        form.setTwitterUsername(p.getTwitterUsername());
        form.setHireable(p.getHireable());
        form.setPublicRepos(p.getPublicRepos());
        form.setPublicGists(p.getPublicGists());
        form.setFollowers(p.getFollowers());
        form.setFollowing(p.getFollowing());
        if (p.getGithubCreatedAt() != null) {
            form.setGithubCreatedAt(p.getGithubCreatedAt().toString());
        }
        if (p.getGithubUpdatedAt() != null) {
            form.setGithubUpdatedAt(p.getGithubUpdatedAt().toString());
        }
        return form;
    }

    @Override
    public void updateProfile(UserProfileForm form) {
        User user = self.getCurrentUser(); // Call via self-injected proxy

        if (form.getFullName() != null && !form.getFullName().isEmpty()) {
            user.setFullName(form.getFullName());
        }
        user.setEmail(form.getEmail());
        Profile p = user.getProfile();
        p.setProfession(form.getProfession());
        p.setSkills(form.getSkills());
        p.setSoftSkills(form.getSoftSkills());
        p.setBio(form.getBio());
        p.setWebsiteUrl(form.getWebsiteUrl());
        p.setGithubUrl(form.getGithubUrl());
        p.setLinkedinUrl(form.getLinkedinUrl());
        p.setLocation(form.getLocation());
        p.setExperienceLevel(form.getExperienceLevel());
        p.setPrimaryStack(form.getPrimaryStack());
        p.setAvailability(form.getAvailability());

        // Save GitHub data fields
        p.setGithubLogin(form.getGithubLogin());
        p.setGithubName(form.getGithubName());
        p.setGithubCompany(form.getGithubCompany());
        p.setTwitterUsername(form.getTwitterUsername());
        p.setHireable(form.getHireable());
        p.setPublicRepos(form.getPublicRepos());
        p.setPublicGists(form.getPublicGists());
        p.setFollowers(form.getFollowers());
        p.setFollowing(form.getFollowing());

        // Parse and save GitHub dates if provided
        if (form.getGithubCreatedAt() != null && !form.getGithubCreatedAt().isEmpty()) {
            p.setGithubCreatedAt(
                    java.time.LocalDateTime.parse(
                            form.getGithubCreatedAt(),
                            java.time.format.DateTimeFormatter.ISO_DATE_TIME));
        }
        if (form.getGithubUpdatedAt() != null && !form.getGithubUpdatedAt().isEmpty()) {
            p.setGithubUpdatedAt(
                    java.time.LocalDateTime.parse(
                            form.getGithubUpdatedAt(),
                            java.time.format.DateTimeFormatter.ISO_DATE_TIME));
        }

        boolean avatarUploaded = form.getAvatar() != null && !form.getAvatar().isEmpty();
        if (avatarUploaded) {
            try {
                Files.createDirectories(avatarsRoot());
                String filename =
                        StringUtils.cleanPath(
                                Objects.requireNonNull(form.getAvatar().getOriginalFilename()));
                String ext =
                        filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
                String newName =
                        "avatar-" + user.getId() + "-" + Instant.now().toEpochMilli() + ext;
                Path target = avatarsRoot().resolve(newName);
                Files.copy(
                        form.getAvatar().getInputStream(),
                        target,
                        StandardCopyOption.REPLACE_EXISTING);
                p.setAvatarUrl("/uploads/avatars/" + newName);
            } catch (IOException e) {
                throw new ResourceNotFoundException("Failed to store avatar image", e);
            }
        } else if (StringUtils.hasText(form.getAvatarUrl())) {
            p.setAvatarUrl(form.getAvatarUrl());
        }

        userRepository.save(user);
    }
}

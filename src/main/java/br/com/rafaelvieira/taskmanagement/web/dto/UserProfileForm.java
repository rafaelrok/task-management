package br.com.rafaelvieira.taskmanagement.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
public class UserProfileForm {

    // Getters and setters
    @Size(max = 120)
    private String fullName;

    @Email private String email;

    @Size(max = 120)
    private String profession;

    @Size(max = 255)
    private String websiteUrl;

    @Size(max = 255)
    private String githubUrl;

    @Size(max = 255)
    private String linkedinUrl;

    @Size(max = 2000)
    private String bio;

    // CSV string of skills (e.g., "Java, Spring, SQL")
    @Size(max = 1000)
    private String skills;

    @Size(max = 1000)
    private String softSkills;

    // Current avatar URL (read-only for view)
    @Size(max = 255)
    private String avatarUrl;

    // Avatar upload (optional)
    private MultipartFile avatar;

    @Size(max = 120)
    private String location;

    @Size(max = 50)
    private String experienceLevel;

    @Size(max = 255)
    private String primaryStack;

    @Size(max = 60)
    private String availability;

    // GitHub fields getters and setters
    // GitHub import fields
    @Size(max = 255)
    private String githubLogin;

    @Size(max = 255)
    private String githubName;

    @Size(max = 255)
    private String githubCompany;

    @Size(max = 100)
    private String twitterUsername;

    private Boolean hireable;
    private Integer publicRepos;
    private Integer publicGists;
    private Integer followers;
    private Integer following;
    private String githubCreatedAt;
    private String githubUpdatedAt;
}

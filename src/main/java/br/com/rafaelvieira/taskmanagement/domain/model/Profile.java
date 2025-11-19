package br.com.rafaelvieira.taskmanagement.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @Column(name = "avatar_url")
    private String avatarUrl;

    @Size(max = 120)
    private String profession;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Size(max = 255)
    @Column(name = "website_url")
    private String websiteUrl;

    @Size(max = 255)
    @Column(name = "github_url")
    private String githubUrl;

    @Size(max = 255)
    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Size(max = 120)
    private String location;

    @Size(max = 50)
    @Column(name = "experience_level")
    private String experienceLevel;

    @Size(max = 255)
    @Column(name = "primary_stack")
    private String primaryStack;

    @Size(max = 60)
    private String availability;

    // Skills CSV retained for now (later could be normalized)
    @Column(name = "skills")
    private String skills;

    @Size(max = 1000)
    @Column(name = "soft_skills")
    private String softSkills;

    // GitHub integration fields
    @Size(max = 255)
    @Column(name = "github_login")
    private String githubLogin;

    @Size(max = 255)
    @Column(name = "github_name")
    private String githubName;

    @Size(max = 255)
    @Column(name = "github_company")
    private String githubCompany;

    @Size(max = 100)
    @Column(name = "twitter_username")
    private String twitterUsername;

    @Size(max = 500)
    @Column(name = "github_html_url")
    private String githubHtmlUrl;

    @Column(name = "hireable")
    private Boolean hireable;

    @Column(name = "public_repos")
    private Integer publicRepos;

    @Column(name = "public_gists")
    private Integer publicGists;

    @Column(name = "followers")
    private Integer followers;

    @Column(name = "following")
    private Integer following;

    @Column(name = "github_created_at")
    private java.time.LocalDateTime githubCreatedAt;

    @Column(name = "github_updated_at")
    private java.time.LocalDateTime githubUpdatedAt;
}

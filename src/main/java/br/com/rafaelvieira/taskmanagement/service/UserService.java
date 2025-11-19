package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.web.dto.UserProfileForm;

public interface UserService {
    User getCurrentUser();

    UserProfileForm loadProfile();

    void updateProfile(UserProfileForm form);
}

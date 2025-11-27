package br.com.rafaelvieira.taskmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskManagementApplication {
    void main(String[] args) {
        // Fix FileCountLimitExceededException - Deve ser definido ANTES do Spring
        // inicializar
        System.setProperty("org.apache.tomcat.util.http.fileupload.impl.FileCountMax", "1000");

        SpringApplication.run(TaskManagementApplication.class, args);
    }
}

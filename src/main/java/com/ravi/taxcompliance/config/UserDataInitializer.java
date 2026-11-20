
package com.ravi.taxcompliance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ravi.taxcompliance.entity.AppUser;
import com.ravi.taxcompliance.repository.UserRepository;

//This initializer:
//1. Reads admin credentials from configuration.
//2. Checks whether the username already exists.
//3. Encrypts the password using BCrypt.
//4. Saves the user into PostgreSQL.
//5. Avoids creating duplicate accounts.

@Configuration
public class UserDataInitializer {

    @Bean
    public CommandLineRunner initializeAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String username,
            @Value("${app.admin.password:}") String password) {

        return args -> {

            if (password == null || password.isBlank()) {
                System.out.println(
                    "Admin initialization skipped: no password configured");
                return;
            }

            if (userRepository.findByUsername(username).isPresent()) {
                System.out.println("Admin user already exists");
                return;
            }

            AppUser user = new AppUser();

            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole("USER");
            user.setEnabled(true);

            userRepository.save(user);

            System.out.println("Admin user created successfully");
        };
    }
}

//UPDATE app_users
//SET enabled = false
//WHERE username = 'admin';
//
//UPDATE app_users
//SET enabled = true
//WHERE username = 'admin';

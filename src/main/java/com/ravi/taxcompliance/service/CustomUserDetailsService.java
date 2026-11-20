
package com.ravi.taxcompliance.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ravi.taxcompliance.entity.AppUser;
import com.ravi.taxcompliance.repository.UserRepository;


//Finds the username in PostgreSQL.
//Retrieves the stored BCrypt password.
//Loads the user's role.
//Checks whether the account is enabled.
//Rejects authentication if the username doesn't exist.

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        AppUser appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

        String role = appUser.getRole();

        if (role == null || role.isBlank()) {
            throw new UsernameNotFoundException(
                    "User role is not configured");
        }

        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities(new SimpleGrantedAuthority(role))
                .disabled(!appUser.isEnabled())
                .build();
    }
}

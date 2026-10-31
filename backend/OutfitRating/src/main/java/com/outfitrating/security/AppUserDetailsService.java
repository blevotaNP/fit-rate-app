package com.outfitrating.security;

import com.outfitrating.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    /** {@code login} may be either a username or an e-mail address. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return users.findByUsernameIgnoreCase(login)
                .or(() -> users.findByEmailIgnoreCase(login))
                .map(AppUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + login));
    }
}

package nl.ing.lovebird.sitemanagement.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> getUser(final UUID userId) {
        return userRepository.getUser(userId);
    }

    public User getUserOrThrow(final UUID userId) {
        return userRepository.getUser(userId).orElseThrow(() ->
                new IllegalArgumentException("User with id '%s' could not be found".formatted(userId)));
    }

    public void saveUser(final User user) {
        userRepository.upsertUser(user);
    }

    public void deleteUser(final UUID userId) {
        userRepository.getUser(userId)
                .ifPresent(user -> userRepository.deleteUser(user.getUserId()));
    }
}

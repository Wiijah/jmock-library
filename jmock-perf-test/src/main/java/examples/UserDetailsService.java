package examples;

public interface UserDetailsService {
    User lookup(Long userId);
}
package za.ac.richfield.cbdventures.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.UserPreference;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUser(User user);
}

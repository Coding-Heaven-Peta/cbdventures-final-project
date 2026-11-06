package za.ac.richfield.cbdventures.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.richfield.cbdventures.domain.BusinessHours;

import java.util.List;

public interface BusinessHoursRepository extends JpaRepository<BusinessHours, Long> {

    List<BusinessHours> findByBusinessIdOrderByDayOfWeekAsc(Long businessId);
}

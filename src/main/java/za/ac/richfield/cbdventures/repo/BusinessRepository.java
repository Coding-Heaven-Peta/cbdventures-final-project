package za.ac.richfield.cbdventures.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;

import java.util.List;

public interface BusinessRepository extends JpaRepository<Business, Long>, JpaSpecificationExecutor<Business> {

    List<Business> findByOwnerIdOrderByNameAsc(Long ownerId);

    List<Business> findByStatusOrderByCreatedAtDesc(BusinessStatus status);

    long countByStatus(BusinessStatus status);

    long countByCategoryId(Long categoryId);
}

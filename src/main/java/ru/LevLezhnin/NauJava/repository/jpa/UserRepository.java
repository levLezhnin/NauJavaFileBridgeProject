package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import ru.LevLezhnin.NauJava.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @EntityGraph(attributePaths = {"storageQuota"})
    Optional<User> findWithDetailsById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByUsernameLikeIgnoreCase(String username, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateById(Long id);
}

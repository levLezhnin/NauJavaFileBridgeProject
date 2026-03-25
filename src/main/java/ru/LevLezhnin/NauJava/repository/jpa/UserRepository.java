package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import ru.LevLezhnin.NauJava.model.User;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    List<User> findAllByOrderByIdAsc(Pageable pageable);
    List<User> findByUsernameLikeIgnoreCase(String username, Pageable pageable);
    Optional<User> findByEmail(String email);
}

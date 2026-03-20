package ru.LevLezhnin.NauJava.repository.custom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.LevLezhnin.NauJava.model.File;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.model.UserBan;
import ru.LevLezhnin.NauJava.model.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final EntityManager entityManager;

    @Autowired
    public UserRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);

        Root<User> userRoot = criteriaQuery.from(User.class);
        Predicate idPredicate = criteriaBuilder.equal(userRoot.get("id"), id);

        criteriaQuery.select(userRoot).where(idPredicate);

        return Optional.ofNullable(entityManager.createQuery(criteriaQuery).getSingleResultOrNull());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll(Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);

        Root<User> userRoot = criteriaQuery.from(User.class);
        Order order = criteriaBuilder.asc(userRoot.get("id"));

        criteriaQuery.select(userRoot).orderBy(order);

        int firstResultIndex = page * size;

        return entityManager.createQuery(criteriaQuery).setFirstResult(firstResultIndex).setMaxResults(size).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByUsername(String username, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);

        Root<User> userRoot = criteriaQuery.from(User.class);
        Predicate usernameLikePredicate = criteriaBuilder.like(userRoot.get("username"), username);
        Order order = criteriaBuilder.asc(userRoot.get("id"));

        criteriaQuery.select(userRoot).where(usernameLikePredicate).orderBy(order);

        int firstResultIndex = page * size;

        return entityManager.createQuery(criteriaQuery).setFirstResult(firstResultIndex).setMaxResults(size).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);

        Root<User> userRoot = criteriaQuery.from(User.class);
        Predicate emailPredicate = criteriaBuilder.equal(userRoot.get("email"), email);

        criteriaQuery.select(userRoot).where(emailPredicate);

        return Optional.ofNullable(entityManager.createQuery(criteriaQuery).getSingleResultOrNull());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        User user = entityManager.find(User.class, id);

        if (user == null) {
            return;
        }

        for (File file : user.getActiveFiles()) {
            file.setAuthor(null);
        }
        user.getActiveFiles().clear();
        entityManager.flush();

        if (user.getRole() == UserRole.ADMIN) {
            for (UserBan providedBan : user.getProvidedBans()) {
                providedBan.setAdmin(null);
            }
            user.getProvidedBans().clear();
            entityManager.flush();
        }

        for (UserBan ban : user.getBanHistory()) {
            ban.setBannedUser(null);
        }
        user.getBanHistory().clear();
        entityManager.flush();

        entityManager.remove(user);
    }
}

package com.voxa.api.repository;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.projection.AuthenticationProjection;
import com.voxa.api.model.projection.UserProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmailOrUsername(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    @Query("""
    SELECT
        u.email AS email,
        u.username AS username,
        p.name AS name,
        p.birthday AS birthday,
        p.gender AS gender,
        p.bio AS bio,
        p.ppUrl AS ppUrl,
        p.postCount AS postCount,
        p.followerCount AS followerCount,
        p.followingCount AS followingCount
    FROM User u LEFT JOIN u.profile p WHERE u.id = :id
    """)
    Optional<UserProjection> findCurrentUser(String id);

    @Query("""
    SELECT u.id AS id FROM User u WHERE u.id = :id
    """)
    Optional<AuthenticationProjection> findForAuthentication(String id);
}

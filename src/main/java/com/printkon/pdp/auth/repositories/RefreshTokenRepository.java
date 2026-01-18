package com.printkon.pdp.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.printkon.pdp.auth.models.RefreshToken;
import com.printkon.pdp.user.models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

	/**
	 * Finds a token by its hashed ID. The ID stored in the database is a SHA-256
	 * hash of the actual token sent to the client.
	 */
	@Override
	Optional<RefreshToken> findById(String hashedId);

	/**
	 * Finds all non-revoked tokens for a user. Used for revoking an entire token
	 * family.
	 */
	List<RefreshToken> findByUserAndRevokedIsFalse(User user);

	/**
	 * Atomically revokes a refresh token by its hashed ID. This is a critical step
	 * to prevent race conditions during token rotation. It ensures a token can only
	 * be revoked if it's currently active.
	 *
	 * @param oldId The hashed ID of the token to revoke.
	 * @param newId The hashed ID of the new token that is replacing the old one.
	 * @return The number of rows affected (1 if successful, 0 if the token was
	 *         already revoked).
	 */
	@Modifying
	@Query("UPDATE RefreshToken t SET t.revoked = true, t.replacedBy = :newId WHERE t.id = :oldId AND t.revoked = false")
	int findAndRevokeById(@Param("oldId") String oldId, @Param("newId") String newId);
}
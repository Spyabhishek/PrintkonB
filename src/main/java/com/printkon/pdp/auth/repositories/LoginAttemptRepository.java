package com.printkon.pdp.auth.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.auth.models.LoginAttempt;
import com.printkon.pdp.auth.models.LoginAttemptId;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, LoginAttemptId> {
	Optional<LoginAttempt> findByIdentifierAndRemoteAddr(String identifier, String remoteAddr);
}
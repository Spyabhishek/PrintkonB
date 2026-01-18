package com.printkon.pdp.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.auth.models.JwtBlacklist;

public interface JwtBlacklistRepository extends JpaRepository<JwtBlacklist, String> {
}
package com.printkon.pdp.user.repositories;

import com.printkon.pdp.user.models.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

	List<Address> findByUserId(Long userId);

	Optional<Address> findByIdAndUserId(Long id, Long userId);

	List<Address> findByUserIdAndIsDefaultTrue(Long userId);

	@Modifying
	@Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.isDefault = true")
	void clearDefaultAddresses(@Param("userId") Long userId);

	boolean existsByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);

	@Modifying
	@Query("DELETE FROM Address a WHERE a.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Long userId);

	List<Address> findByCityContainingIgnoreCase(String city);

	List<Address> findByStateContainingIgnoreCase(String state);

	List<Address> findByCountryContainingIgnoreCase(String country);

	List<Address> findByZip(String zip);
}
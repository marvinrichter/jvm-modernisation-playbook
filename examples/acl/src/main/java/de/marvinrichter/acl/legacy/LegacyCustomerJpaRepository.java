package de.marvinrichter.acl.legacy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LegacyCustomerJpaRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByCustId(String custId);
}

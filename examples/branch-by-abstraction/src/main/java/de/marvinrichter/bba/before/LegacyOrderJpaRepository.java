package de.marvinrichter.bba.before;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LegacyOrderJpaRepository extends JpaRepository<LegacyOrderEntity, UUID> {
}

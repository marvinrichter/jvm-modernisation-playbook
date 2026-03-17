package de.marvinrichter.stranglerfig.newservice.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface NewOrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
}

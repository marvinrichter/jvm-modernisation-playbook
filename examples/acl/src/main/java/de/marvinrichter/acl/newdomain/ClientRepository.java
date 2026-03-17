package de.marvinrichter.acl.newdomain;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port — the new domain's view of persistence.
 * Implemented by {@code LegacyClientRepositoryAdapter} during migration.
 */
public interface ClientRepository {
    Optional<Client> findById(UUID id);
    Client save(Client client);
}

package de.marvinrichter.acl.translation;

import de.marvinrichter.acl.legacy.LegacyCustomerJpaRepository;
import de.marvinrichter.acl.newdomain.Client;
import de.marvinrichter.acl.newdomain.ClientRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter that implements the new domain's {@link ClientRepository} port
 * by delegating to the legacy JPA repository and translating via the ACL.
 *
 * <p>The new domain never sees a {@code Customer} object — translation
 * happens here, at the boundary.
 */
@Repository
public class LegacyClientRepositoryAdapter implements ClientRepository {

    private final LegacyCustomerJpaRepository legacyRepo;
    private final CustomerToClientTranslator translator;

    public LegacyClientRepositoryAdapter(LegacyCustomerJpaRepository legacyRepo,
                                          CustomerToClientTranslator translator) {
        this.legacyRepo = legacyRepo;
        this.translator = translator;
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return legacyRepo.findByCustId(id.toString())
                .map(translator::translate);
    }

    @Override
    public Client save(Client client) {
        var customer = translator.reverseTranslate(client);
        legacyRepo.save(customer);
        return client;
    }
}

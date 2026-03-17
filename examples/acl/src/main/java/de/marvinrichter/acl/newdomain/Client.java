package de.marvinrichter.acl.newdomain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * New domain model — clean names, proper types, self-documenting.
 * This class has no knowledge of the legacy {@code Customer} model.
 */
public record Client(
        UUID id,
        String name,
        Address address,
        ClientStatus status,
        LocalDate joinedDate
) {}

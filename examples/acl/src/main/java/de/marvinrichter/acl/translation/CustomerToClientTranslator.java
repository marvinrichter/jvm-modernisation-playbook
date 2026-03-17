package de.marvinrichter.acl.translation;

import de.marvinrichter.acl.legacy.Customer;
import de.marvinrichter.acl.newdomain.Address;
import de.marvinrichter.acl.newdomain.Client;
import de.marvinrichter.acl.newdomain.ClientStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * The Anti-Corruption Layer translator.
 *
 * <p>Translates between the legacy {@link Customer} model and the new {@link Client}
 * domain model. This class is the single place where legacy naming and type
 * conventions are known about.
 *
 * <p>Design rules:
 * <ul>
 *   <li>Pure function — no side effects, no IO, no Spring transaction</li>
 *   <li>Both directions supported (forward and reverse translation)</li>
 *   <li>Explicit about format assumptions (date format, status codes)</li>
 * </ul>
 */
@Component
public class CustomerToClientTranslator {

    private static final DateTimeFormatter LEGACY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public Client translate(Customer customer) {
        return new Client(
                UUID.fromString(customer.custId),
                customer.custNm,
                new Address(customer.custAddr1, customer.custAddr2),
                ClientStatus.fromLegacyCode(customer.status),
                LocalDate.parse(customer.createDt, LEGACY_DATE_FORMAT));
    }

    public Customer reverseTranslate(Client client) {
        return new Customer(
                client.id().toString(),
                client.name(),
                client.address().line1(),
                client.address().line2(),
                client.status().toLegacyCode(),
                client.joinedDate().format(LEGACY_DATE_FORMAT));
    }
}

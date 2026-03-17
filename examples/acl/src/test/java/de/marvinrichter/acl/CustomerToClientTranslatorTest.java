package de.marvinrichter.acl;

import de.marvinrichter.acl.legacy.Customer;
import de.marvinrichter.acl.newdomain.ClientStatus;
import de.marvinrichter.acl.translation.CustomerToClientTranslator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerToClientTranslatorTest {

    private final CustomerToClientTranslator translator = new CustomerToClientTranslator();

    @Test
    void translates_active_customer_to_client() {
        var id = UUID.randomUUID();
        var customer = new Customer(
                id.toString(), "Acme Corp", "123 Main St", null, 1, "20240101");

        var client = translator.translate(customer);

        assertThat(client.id()).isEqualTo(id);
        assertThat(client.name()).isEqualTo("Acme Corp");
        assertThat(client.address().line1()).isEqualTo("123 Main St");
        assertThat(client.address().line2()).isNull();
        assertThat(client.status()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(client.joinedDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void translates_inactive_customer() {
        var customer = new Customer(
                UUID.randomUUID().toString(), "Old Corp", "1 Old St", null, 0, "20200601");

        var client = translator.translate(customer);

        assertThat(client.status()).isEqualTo(ClientStatus.INACTIVE);
    }

    @Test
    void translates_suspended_customer() {
        var customer = new Customer(
                UUID.randomUUID().toString(), "Bad Corp", "2 Bad St", null, 2, "20230315");

        var client = translator.translate(customer);

        assertThat(client.status()).isEqualTo(ClientStatus.SUSPENDED);
    }

    @Test
    void throws_on_unknown_status_code() {
        var customer = new Customer(
                UUID.randomUUID().toString(), "X Corp", "3 X St", null, 99, "20240101");

        assertThatThrownBy(() -> translator.translate(customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void round_trips_correctly() {
        var id = UUID.randomUUID();
        var original = new Customer(
                id.toString(), "Round Trip Corp", "99 Loop Lane", "Suite 1", 1, "20250101");

        var client = translator.translate(original);
        var restored = translator.reverseTranslate(client);

        assertThat(restored.custId).isEqualTo(original.custId);
        assertThat(restored.custNm).isEqualTo(original.custNm);
        assertThat(restored.custAddr1).isEqualTo(original.custAddr1);
        assertThat(restored.custAddr2).isEqualTo(original.custAddr2);
        assertThat(restored.status).isEqualTo(original.status);
        assertThat(restored.createDt).isEqualTo(original.createDt);
    }
}

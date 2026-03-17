package de.marvinrichter.acl.adapter;

import de.marvinrichter.acl.newdomain.Client;
import de.marvinrichter.acl.newdomain.ClientRepository;
import de.marvinrichter.acl.newdomain.ClientStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound adapter — depends only on the new domain's {@link ClientRepository} port.
 * The controller has no knowledge of the legacy {@code Customer} model.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository clientRepository;

    public ClientController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable UUID id) {
        return clientRepository.findById(id)
                .map(ClientResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@RequestBody CreateClientRequest request) {
        var client = new Client(
                UUID.randomUUID(),
                request.name(),
                new de.marvinrichter.acl.newdomain.Address(request.address()),
                ClientStatus.ACTIVE,
                LocalDate.now());
        var saved = clientRepository.save(client);
        return ResponseEntity.status(201).body(ClientResponse.from(saved));
    }

    record CreateClientRequest(String name, String address) {}

    record ClientResponse(String id, String name, String address, String status, String joinedDate) {
        static ClientResponse from(Client client) {
            return new ClientResponse(
                    client.id().toString(),
                    client.name(),
                    client.address().line1(),
                    client.status().name(),
                    client.joinedDate().toString());
        }
    }
}

package nl.ing.lovebird.sitemanagement.lib.types;

import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.lib.types.ClientIdConverter;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIdConverterTest {

    @Test
    void shouldConvert() {
        ClientId clientId = new ClientIdConverter()
                .convert("841ca351-e6fe-4bed-ae92-dcc5ac812730");

        assertThat(clientId).isNotNull();
        assertThat(clientId.unwrap()).isEqualTo(UUID.fromString("841ca351-e6fe-4bed-ae92-dcc5ac812730"));
    }

}
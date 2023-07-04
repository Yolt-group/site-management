package nl.ing.lovebird.sitemanagement.lib.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIdTest {

    @Test
    public void shouldBeEqual() {
        ClientId c1 = new ClientId("841ca351-e6fe-4bed-ae92-dcc5ac812730");
        ClientId c2 = new ClientId("841ca351-e6fe-4bed-ae92-dcc5ac812730");
        assertThat(c1).isEqualTo(c2);
    }

    @Test
    public void shouldNotBeEqual() {
        ClientId c1 = new ClientId("841ca351-e6fe-4bed-ae92-dcc5ac812730");
        ClientId c2 = new ClientId("11111111-1111-1111-1111-111111111111");
        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    public void shouldSerializeAndDeserialize() throws JsonProcessingException {
        ClientId input = new ClientId("841ca351-e6fe-4bed-ae92-dcc5ac812730");

        ObjectMapper objectMapper = new ObjectMapper();
        String clientIdAsJsonString = objectMapper.writeValueAsString(input);

        ClientId deserialized = objectMapper.readValue(clientIdAsJsonString, ClientId.class);
        assertThat(deserialized).isEqualTo(input);
        assertThat(deserialized.unwrap().toString()).isEqualTo("841ca351-e6fe-4bed-ae92-dcc5ac812730");
    }

    @Test
    public void toStringShouldReturnUUIDAsString() {
        assertThat(new ClientId("841ca351-e6fe-4bed-ae92-dcc5ac812730").toString())
                .isEqualTo("841ca351-e6fe-4bed-ae92-dcc5ac812730");

        assertThat(new ClientId("841ca351-e6fe-4bed-ae92-dcc5ac812730").toString())
                .isNotEqualTo("11111111-1111-1111-1111-111111111111");
    }
}
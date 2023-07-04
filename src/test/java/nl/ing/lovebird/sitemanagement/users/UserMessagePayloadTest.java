package nl.ing.lovebird.sitemanagement.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class UserMessagePayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserialize_unfamiliarUserMessageType_mapsToUNKNOWN() throws IOException {
        var userMessagePayload = new UserMessage.Payload(UUID.randomUUID(), new ClientId(UUID.randomUUID()),
                UserMessage.Payload.UserMessageType.USER_LOGIN, true, null);

        var json = objectMapper.writeValueAsString(userMessagePayload);
        var jsonWithUnfamiliarType = json.replace(userMessagePayload.userMessageType().toString(), "SOMETHING_NEW");

        var deserializedUserMessagePayload = objectMapper.readValue(jsonWithUnfamiliarType, UserMessage.Payload.class);

        assertThat(deserializedUserMessagePayload.userMessageType()).isEqualTo(UserMessage.Payload.UserMessageType.UNKNOWN);
    }
}
package nl.ing.lovebird.sitemanagement.lib.types;

import com.datastax.driver.core.ProtocolVersion;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.lib.types.ClientIdTypeCodec;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIdTypeCodecTest {

    @Test
    void shouldParse() {
        ClientIdTypeCodec clientIdTypeCodec = ClientIdTypeCodec.instance;
        ClientId parsed = clientIdTypeCodec.parse("841ca351-e6fe-4bed-ae92-dcc5ac812730");

        assertThat(parsed).isEqualTo(new ClientId("841ca351-e6fe-4bed-ae92-dcc5ac812730"));
    }

    @Test
    void shouldFormat() {
        ClientIdTypeCodec clientIdTypeCodec = ClientIdTypeCodec.instance;
        String formatted = clientIdTypeCodec.format(new ClientId("841ca351-e6fe-4bed-ae92-dcc5ac812730"));
        assertThat(formatted).isEqualTo("841ca351-e6fe-4bed-ae92-dcc5ac812730");
    }

    @Test
    void shouldSerialize() {
        ClientIdTypeCodec clientIdTypeCodec = ClientIdTypeCodec.instance;
        ClientId clientId = new ClientId(new UUID(1,2));

        LongBuffer buffer = clientIdTypeCodec.serialize(clientId, ProtocolVersion.V3).asLongBuffer();
        assertThat(buffer.get(0)).isEqualTo(1);
        assertThat(buffer.get(1)).isEqualTo(2);
    }

    @Test
    void shouldDeserialize() {
        ClientIdTypeCodec clientIdTypeCodec = ClientIdTypeCodec.instance;

        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        buffer.rewind();

        ClientId clientId = clientIdTypeCodec.deserialize(buffer, ProtocolVersion.V3);
        assertThat(clientId.unwrap()).isEqualTo(uuid);
    }
}
package nl.ing.lovebird.sitemanagement.lib.types;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.InvalidTypeException;

import java.nio.ByteBuffer;
import java.util.UUID;

public class ClientIdTypeCodec extends TypeCodec<ClientId> {

    public static final ClientIdTypeCodec instance = new ClientIdTypeCodec();

    private ClientIdTypeCodec() {
        super(DataType.uuid(), ClientId.class);
    }

    @Override
    public ClientId parse(String value) {
        try {
            return value == null || value.isEmpty() || value.equalsIgnoreCase("NULL")
                    ? null
                    : new ClientId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new InvalidTypeException(
                    String.format("Cannot parse ClientId value from \"%s\"", value), e);
        }
    }

    @Override
    public String format(ClientId value) {
        if (value == null) return "NULL";
        return value.unwrap().toString();
    }

    @Override
    public ByteBuffer serialize(ClientId value, ProtocolVersion protocolVersion) {
        if (value == null) return null;
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(0, value.unwrap().getMostSignificantBits());
        bb.putLong(8, value.unwrap().getLeastSignificantBits());
        return bb;
    }

    @Override
    public ClientId deserialize(ByteBuffer bytes, ProtocolVersion protocolVersion) {
        return bytes == null || bytes.remaining() == 0
                ? null
                : new ClientId(new UUID(bytes.getLong(bytes.position()), bytes.getLong(bytes.position() + 8)));
    }
}
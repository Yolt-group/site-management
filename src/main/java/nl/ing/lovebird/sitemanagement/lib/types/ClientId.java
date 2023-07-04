package nl.ing.lovebird.sitemanagement.lib.types;

import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
public final class ClientId implements Identity<UUID> {
    private final UUID value;

    public ClientId(UUID uuid) {
        this.value = uuid;
    }

    public ClientId(String s) {
        this(UUID.fromString(s));
    }

    @Override
    public UUID unwrap() {
        return value;
    }

    public static ClientId random() {
        return new ClientId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

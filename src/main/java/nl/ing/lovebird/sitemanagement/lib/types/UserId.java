package nl.ing.lovebird.sitemanagement.lib.types;

import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
public final class UserId implements Identity<UUID> {
    private final UUID value;

    public UserId(UUID uuid) {
        this.value = uuid;
    }

    public UserId(String s) {
        this(UUID.fromString(s));
    }

    @Override
    public UUID unwrap() {
        return value;
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

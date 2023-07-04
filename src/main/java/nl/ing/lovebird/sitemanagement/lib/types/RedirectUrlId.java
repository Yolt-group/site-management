package nl.ing.lovebird.sitemanagement.lib.types;

import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
public final class RedirectUrlId implements Identity<UUID> {
    private final UUID value;

    public RedirectUrlId(UUID uuid) {
        this.value = uuid;
    }

    public RedirectUrlId(String s) {
        this(UUID.fromString(s));
    }

    @Override
    public UUID unwrap() {
        return value;
    }

    public static RedirectUrlId random() {
        return new RedirectUrlId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

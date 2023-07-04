package nl.ing.lovebird.sitemanagement.lib.types;

import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
public final class SiteId implements Identity<UUID> {
    private final UUID value;

    public SiteId(UUID uuid) {
        this.value = uuid;
    }

    public SiteId(String s) {
        this(UUID.fromString(s));
    }

    @Override
    public UUID unwrap() {
        return value;
    }

    public static SiteId random() {
        return new SiteId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

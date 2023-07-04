package nl.ing.lovebird.sitemanagement.lib.types;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class ClientIdConverter implements Converter<String, ClientId> {

    @Override
    public ClientId convert(@NonNull final String source) {
        return new ClientId(UUID.fromString(source));
    }
}

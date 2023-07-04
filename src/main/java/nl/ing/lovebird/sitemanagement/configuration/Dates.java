package nl.ing.lovebird.sitemanagement.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

class Dates {
    private static final DateTimeFormatter DATE_TIME_FORMAT_WITH_ZONE = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
            .withZone(ZoneOffset.UTC);

    static JsonSerializer<Date> serializer() {
        return new JsonSerializer<>() {
            @Override
            public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(DATE_TIME_FORMAT_WITH_ZONE.format(value.toInstant()));
            }
        };
    }

}

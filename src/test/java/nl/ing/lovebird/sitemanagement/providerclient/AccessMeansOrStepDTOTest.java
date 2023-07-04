package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import nl.ing.lovebird.sitemanagement.usersite.RedirectStep;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.JsonComponentModule;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessMeansOrStepDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .setDateFormat(new StdDateFormat().withColonInTimeZone(false))
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule())
            .registerModule(new JsonComponentModule());

    @Test
    void testDeserialization() throws IOException {
        final String userId = UUID.randomUUID().toString();
        final String accessMeans = "{\"userId\": \"" + userId + "\", \"accessMeansBlob\" : \"bla\", \"expireTime\":\"2018-01-01\", \"updated\":\"2018-01-01\"}";
        final String stepJson = "{\"type\":\"REDIRECT_URL\", \"redirectUrl\": \"http://somesite.com\", \"providerState\":\"SomeProviderState\"}";
        final String someResponse = "{\"accessMeans\" : " + accessMeans + ", \"step\": " + stepJson + "}";

        AccessMeansOrStepDTO accessMeansOrStepDTO = objectMapper.readValue(someResponse, AccessMeansOrStepDTO.class);
        assertThat(accessMeansOrStepDTO.getAccessMeans().getUserId().toString()).isEqualTo(userId);
        assertThat(accessMeansOrStepDTO.getStep() instanceof RedirectStep).isTrue();
        assertThat(((RedirectStep) accessMeansOrStepDTO.getStep()).getRedirectUrl()).isEqualTo("http://somesite.com");
        assertThat(accessMeansOrStepDTO.getStep().getProviderState()).isEqualTo("SomeProviderState");

    }
}
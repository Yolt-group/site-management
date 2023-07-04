package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ErrorCodeExtractor {
    private final ObjectMapper objectMapper;

    String getFunctionalErrorCode(HttpStatusCodeException httpStatusCodeException) {
        try {
            return Optional.of(objectMapper.readValue(httpStatusCodeException.getResponseBodyAsString(), ErrorDTO.class))
                    .map(ErrorDTO::getCode)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}

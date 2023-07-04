package nl.ing.lovebird.sitemanagement.nonlicensedclients;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlNotFoundException;
import nl.ing.lovebird.sitemanagement.exception.NoRedirectSubjectException;
import nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI;
import nl.ing.lovebird.sitemanagement.lib.documentation.External;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
public class ClientRedirectController {

    public static final String CORRELATION_ID_COOKIE = "client-redirect";

    private final ClientRedirectService clientRedirectService;

    private final URI developerPortalNotFoundUrl;
    private final URI developerPortalBadRequestUrl;
    private final Resource clientRedirectJavascriptPage;

    @Autowired
    public ClientRedirectController(
            ClientRedirectService clientRedirectService,
            @Value("${yolt.yts-group.not-found-url}") URI developerPortalNotFoundUrl,
            @Value("${yolt.yts-group.bad-request-url}") URI developerPortalBadRequestUrl,
            @Value("classpath:client-redirect.html") Resource clientRedirectJavascriptPage) {
        this.clientRedirectService = clientRedirectService;
        this.developerPortalNotFoundUrl = developerPortalNotFoundUrl;
        this.developerPortalBadRequestUrl = developerPortalBadRequestUrl;
        this.clientRedirectJavascriptPage = clientRedirectJavascriptPage;
    }

    @External
    @GetMapping(value = "/client-redirect", produces = {MediaType.TEXT_HTML_VALUE})
    public ResponseEntity<?> handleClientRedirect(
            @RequestParam(name = "state", required = false) String state,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        if (StringUtils.isBlank(state)) {
            final var correlationId = UUID.randomUUID().toString();
            log.info("No state, serving JavaScript to try with fragment. correlationId={}", correlationId);
            response.addCookie(buildCorrelationIdCookie(correlationId));

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .cacheControl(CacheControl.noCache())
                    .body(clientRedirectJavascriptPage);
        } else {
            final var redirectUrl = getRedirectUrl(state, request.getQueryString(), null);
            return redirect(redirectUrl);
        }
    }

    @External
    @PostMapping(path = "/client-redirect", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<?> handleRedirectClientWithFragment(
            @RequestParam("url") String url,
            @CookieValue(value = CORRELATION_ID_COOKIE, required = false) String correlationId) {

        final var maybeBankRedirectUrl = parseSubmittedUrl(url);
        final var state = OAuth2RedirectionURI.parse(url).getState();

        if (maybeBankRedirectUrl.isEmpty()) {
            log.warn("Client redirect url posted, correlationId={}, url=\"{}\". It is invalid.", correlationId, url);
            return redirect(developerPortalNotFoundUrl);
        } else if (StringUtils.isBlank(state)) {
            log.warn("Client redirect url posted, correlationId={}, url=\"{}\". It has no state.", correlationId, url);
            return redirect(developerPortalNotFoundUrl);
        } else {
            final var bankRedirectUrl = maybeBankRedirectUrl.get();
            final var clientRedirectURI = getRedirectUrl(state, bankRedirectUrl.getQuery(), bankRedirectUrl.getFragment());
            log.info("Client redirect url posted, correlationId={}, url={}.  Redirecting to \"{}\"", correlationId, url, clientRedirectURI); //NOSHERIFF
            return redirect(clientRedirectURI);
        }
    }

    private URI getRedirectUrl(String state, String query, String fragment) {
        try {
            // Get redirect URL for this state.
            final var clientRedirectBaseUrl = clientRedirectService.getRedirectUrl(state);

            // Copy over the querystring from current request.
            final var clientRedirectUrl = UriComponentsBuilder.fromHttpUrl(clientRedirectBaseUrl)
                    .query(query)
                    .fragment(fragment)
                    .build();

            // Successfully redirect the user.
            log.info("Redirecting state '{}', to client: {}", state, clientRedirectBaseUrl);
            return clientRedirectUrl.toUri();
        } catch (NoRedirectSubjectException e) {
            // Redirect, don't even know which for client.
            log.warn("Redirecting state '{}' to error: {}", state, developerPortalNotFoundUrl, e); //NOSHERIFF
            return developerPortalNotFoundUrl;
        } catch (ClientRedirectUrlNotFoundException e) {
            // Redirect, don't know to what URL of the client.
            log.warn("Redirecting state '{}' to error: {}", state, developerPortalBadRequestUrl, e); //NOSHERIFF
            return developerPortalBadRequestUrl;
        }
    }

    private static Cookie buildCorrelationIdCookie(String correlationId) {
        final var cookie = new Cookie(CORRELATION_ID_COOKIE, correlationId);
        cookie.setMaxAge(30);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }

    private static Optional<UriComponents> parseSubmittedUrl(String url) {
        try {
            return Optional.of(UriComponentsBuilder.fromHttpUrl(url).build());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static ResponseEntity<?> redirect(URI redirectUrl) {
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(redirectUrl)
                .build();
    }
}

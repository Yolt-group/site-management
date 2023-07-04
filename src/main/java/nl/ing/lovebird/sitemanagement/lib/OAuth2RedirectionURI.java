package nl.ing.lovebird.sitemanagement.lib;

import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Set;

/**
 * OAuth2 RFC https://tools.ietf.org/html/rfc6749
 */
@Slf4j
@UtilityClass
public class OAuth2RedirectionURI {

    /**
     * Permitted schemes for redirectURIs.  http is listed for testing purposes.
     */
    static final Set<String> permittedSchemes = Set.of("http", "https");

    /**
     * See section 4.1.2.1 https://tools.ietf.org/html/rfc6749#section-4.1.2.1
     */
    private static final Set<String> RFC6749ErrorResponseCodes = Set.of(
            /*
             * The request is missing a required parameter, includes an
             * invalid parameter value, includes a parameter more than
             * once, or is otherwise malformed.
             */
            "invalid_request",
            /*
             * The client is not authorized to request an authorization
             * code using this method.
             */
            "unauthorized_client",
            /*
             * The resource owner or authorization server denied the
             * request.
             */
            "access_denied",
            /*
             * The authorization server does not support obtaining an
             * authorization code using this method.
             */
            "unsupported_response_type",
            /*
             * The requested scope is invalid, unknown, or malformed.
             */
            "invalid_scope",
            /*
             * The authorization server encountered an unexpected
             * condition that prevented it from fulfilling the request.
             * (This error code is needed because a 500 Internal Server
             * Error HTTP status code cannot be returned to the client
             * via an HTTP redirect.)
             */
            "server_error",
            /*
             * The authorization server is currently unable to handle
             * the request due to a temporary overloading or maintenance
             * of the server.  (This error code is needed because a 503
             * Service Unavailable HTTP status code cannot be returned
             * to the client via an HTTP redirect.)
             */
            "temporarily_unavailable"
    );

    /**
     * This method matches a possible invalid (strictly incorrect, but technically correct) RFC6749 error code up
     * with a valid one. If the given <code>errorCode</code> is not a valid RFC6749 error
     * response (e.a. not contained in RFC6749ErrorResponseCodes) then it tries to "match" the given errorCode up with
     * an valid RFC6749 error code by replacing any hyphens by an underscore and replacing any upper case character
     * by a lower case character. If that does not yield a valid error code, the input is returned unmodified.
     *
     * @param errorCode the error code to match up;
     * @return the corrected errorCode or the original error_code if no matches could be made or null if the input was null.
     * if the given errorCode was null
     */
    @VisibleForTesting
    String matchUpWithRFC6749ErrorCodeConvention(final @Nullable String errorCode) {
        if (errorCode == null) {
            return null;
        }

        if (RFC6749ErrorResponseCodes.contains(errorCode)) {
            return errorCode;
        }
        String alternative = errorCode.replaceAll("-", "_").toLowerCase();
        if (RFC6749ErrorResponseCodes.contains(alternative)) {
            return alternative;
        } else {
            return errorCode;
        }
    }

    /**
     * See RFC6749 section 4.1.2 https://tools.ietf.org/html/rfc6749#section-4.1.2
     *
     * @return an object containing state and code if validation succeeds, an object that contains the error that occurred otherwise
     */
    public RedirectionURIValidationResult parse(String redirectionURI) {
        if (redirectionURI == null) {
            return RedirectionURIValidationResult.invalid("null_uri", null);
        }
        final UriComponents uri;
        try {
            uri = UriComponentsBuilder.fromUriString(redirectionURI).build();
        } catch (IllegalArgumentException e) {
            log.warn("Got invalid redirection URI: " + redirectionURI);
            return RedirectionURIValidationResult.invalid("invalid_uri", null);
        }
        if (uri.getScheme() == null || !permittedSchemes.contains(uri.getScheme())) {
            log.warn("Got redirection URI with invalid scheme: " + redirectionURI);
            return RedirectionURIValidationResult.invalid("invalid_uri", null);
        }

        final Map<String, String> params = mergeQueryParametersAndFragmentIdentifierParameters(uri);

        final String error = matchUpWithRFC6749ErrorCodeConvention(params.get("error"));
        if (!StringUtils.isBlank(error)) {
            if (!RFC6749ErrorResponseCodes.contains(error)) {
                log.warn("out-of-spec error in a redirectURI: " + redirectionURI);
                return RedirectionURIValidationResult.invalid("invalid_uri", params.get("state"));
            } else {
                return RedirectionURIValidationResult.invalid(error, params.get("state"));
            }
        }

        if (!params.containsKey("state")) {
            log.warn("Got redirection URI without state: " + redirectionURI);
            return RedirectionURIValidationResult.invalid("invalid_uri", null);
        }

        // both code and state *MUST* be present (https://tools.ietf.org/html/rfc6749#section-4.1.2)
        // we only check state since some banks provide something other than "code" (e.g. authorization_code, ...)
        return RedirectionURIValidationResult.valid(params.get("state"));
    }

    private static Map<String, String> mergeQueryParametersAndFragmentIdentifierParameters(UriComponents uri) {
        Map<String, String> parameters = uri.getQueryParams().toSingleValueMap();
        final String fragment = uri.getFragment();
        if (StringUtils.isBlank(fragment)) {
            return parameters;
        }
        // Fake an URI and let this standard component do the parsing/splitting for us.
        final MultiValueMap<String, String> fragmentParams = UriComponentsBuilder.fromUriString("https://www.example.com?" + fragment).build().getQueryParams();
        fragmentParams.toSingleValueMap().forEach(parameters::putIfAbsent);
        return parameters;
    }

    @Value
    @RequiredArgsConstructor
    public static class RedirectionURIValidationResult {
        boolean valid;
        String state;
        String error_code;

        public static RedirectionURIValidationResult valid(@NonNull String state) {
            return new RedirectionURIValidationResult(true, state, null);
        }

        public static RedirectionURIValidationResult invalid(@NonNull String error_code, @Nullable String state) {
            return new RedirectionURIValidationResult(false, state, error_code);
        }
    }
}

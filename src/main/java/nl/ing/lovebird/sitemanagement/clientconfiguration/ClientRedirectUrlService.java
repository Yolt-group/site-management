package nl.ing.lovebird.sitemanagement.clientconfiguration;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.ClientRedirectController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class ClientRedirectUrlService {

    // "Magic" redirectUrlId for the consent starter.
    // This redirectUrlId is available to all unlicensed-clients without it being configured explicitly as such in
    // the client configuration.  It is a "hard-coded" URL that both the consent-starter and site-management know about.
    // It exists to facilitate the usage of consent-starter for clients, normally after giving consent a user is sent
    // back to the client directly.  If a client uses the consent-starter the user must first be sent to the
    // consent-starter and only then to the clients' website.  This is the identifier of the URL of the consent-starter
    // that the user lands on after completing a consent flow.
    public static final UUID CONSENT_STARTER_REDIRECT_URL_ID = UUID.fromString("47539b3a-f249-4602-b00b-251f4e965e67");

    private final ClientRedirectUrlRepository clientRedirectUrlRepository;
    private final String ytsGroupRedirectUrl;
    private final String consentStarterRedirectUrl;
    private final Clock clock;

    public ClientRedirectUrlService(
            ClientRedirectUrlRepository clientRedirectUrlRepository,
            @NonNull @Value("${yolt.yts-group.redirect-url}") String ytsGroupRedirectUrl,
            @NonNull @Value("${yolt.consent-starter.redirect-url}") String consentStarterRedirectUrl,
            Clock clock
    ) {
        if (ytsGroupRedirectUrl.isBlank()) {
            throw new IllegalStateException("yolt.yts-group.redirect-url must be provided");
        }
        this.clientRedirectUrlRepository = clientRedirectUrlRepository;
        this.ytsGroupRedirectUrl = ytsGroupRedirectUrl;
        this.consentStarterRedirectUrl = consentStarterRedirectUrl;
        this.clock = clock;
    }

    public void delete(ClientId clientId, UUID redirectUrlId) {
        clientRedirectUrlRepository.delete(clientId, redirectUrlId);
    }

    public ClientRedirectUrl save(ClientId clientId, UUID redirectUrlId, String url) {
        if (redirectUrlId == null) {
            redirectUrlId = UUID.randomUUID();
        }

        Instant updated = Instant.now(clock);
        ClientRedirectUrl clientRedirectUrl = new ClientRedirectUrl(clientId, redirectUrlId, url, updated);
        clientRedirectUrlRepository.saveClientRedirectUrl(clientRedirectUrl);
        return clientRedirectUrl;
    }

    /**
     * Retrieves a client redirect url by the client id and the redirect url id.
     * <p>
     * When obtaining a redirect url to pass on to providers, use {@link #getBaseClientRedirectUrlOrThrow} which
     * supports both licensed and unlicensed clients.
     *
     * @return redirect url with the given id for the given client
     * @throws ClientRedirectUrlNotFoundException when the provided <code>redirectUrlId</code> could not be found
     * @see #getBaseClientRedirectUrlOrThrow
     */
    public String getRedirectUrlOrThrow(ClientId clientId, UUID redirectUrlId) {
        // This special case is (strictly speaking) only applicable to unlicensed clients.  We don't check that here
        // because we don't know at this point if the client is PSD2 licensed.
        if (CONSENT_STARTER_REDIRECT_URL_ID.equals(redirectUrlId)) {
            return consentStarterRedirectUrl;
        }

        return clientRedirectUrlRepository.get(clientId, redirectUrlId)
                .map(ClientRedirectUrl::getUrl)
                .orElseThrow(() -> new ClientRedirectUrlNotFoundException(clientId.unwrap(), redirectUrlId));
    }

    /**
     * When initiating a payment a client typically provides a redirectUrlId.  If the client is part of the
     * YTS Client Group we should always be using the same fixed redirectUrl when communicating with banks.
     *
     * @return our own {@link #ytsGroupRedirectUrl} if the client is part of the YTS Client Group or the client's own redirectUrl otherwise
     * @throws ClientRedirectUrlNotFoundException when the provided <code>clientProvidedRedirectUrlId</code> could not be found
     */
    public String getBaseClientRedirectUrlOrThrow(@NonNull ClientToken clientToken, @NonNull UUID clientProvidedRedirectUrlId) {
        if (!clientToken.isPSD2Licensed()) {
            return ytsGroupRedirectUrl;
        }

        return getRedirectUrlOrThrow(new ClientId(clientToken.getClientIdClaim()), clientProvidedRedirectUrlId);
    }

    /**
     * XXX: this functionality should be in the {@link nl.ing.lovebird.sitemanagement.nonlicensedclients} package instead.
     * <p>
     * Okay, this is a little wonky so bear with me.
     * <p>
     * If a client is a "YTS Group" client we have onboarded banks with our own YTS Group redirectUrl (that url is {@link #ytsGroupRedirectUrl} by the way).
     * <p>
     * Let's first define some terminology:
     * <p>
     * bankUrl = the url to which we send the user to log in at the bank and give their consent
     * ytsGroupUrl = {@link #ytsGroupRedirectUrl} ("https://client-redirect.yts.yolt.io")
     * clientBaseRedirectUrl = a redirectUrl that a client has registered with us ("https://www.example.com/oath/callback")
     * redirectUrlpostedBackFromSite = the URL to which a bank redirects an end-user after giving consent.
     * <p>
     * In a "normal" flow (for non-"YTS Group") clients, this happens:
     * 1. we send the user to bankUrl
     * 2. user gives consent and is redirected to redirectUrlpostedBackFromSite by the bank
     * 3. client posts redirectUrlpostedBackFromSite to site-management
     * 4. we pass this redirectUrlpostedBackFromSite on to providers for further processing (getting the token at the bank)
     * <p>
     * In a "YTS Group" flow, this happens instead:
     * 1. we send the user to bankUrl
     * 2. user gives consent and is redirected to ytsGroupUrl by the bank
     * 3. we immediately redirect the user to clientBaseRedirectUrl (with the state parameter added, see {@link ClientRedirectController}).
     * 4. the client posts redirectUrlpostedBackFromSite to site-management
     * <p>
     * The base-url of the redirectUrlpostedBackFromSite in this last scenario is clientBaseRedirectUrl but it should be ytsGroupUrl.
     */
    public static String changeRedirectBaseUrlOfUrlPostedBackFromSite(@NonNull String baseClientRedirectUrl, @NonNull String redirectUrlPostedBackFromSite) {
        var components = UriComponentsBuilder.fromUriString(redirectUrlPostedBackFromSite).build();
        var queryParameters = components.getQueryParams();
        var fragment = components.getFragment();


        return new DefaultUriBuilderFactory(baseClientRedirectUrl).builder()
                .replaceQueryParams(queryParameters)
                .fragment(fragment)
                .build()
                .toString();
    }
}

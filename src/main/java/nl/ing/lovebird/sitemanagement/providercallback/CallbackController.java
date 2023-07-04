package nl.ing.lovebird.sitemanagement.providercallback;

import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Size;

import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.callbacks;

@RestController
@RequiredArgsConstructor
@Validated
public class CallbackController {
    // This is a high number, but it makes sense because it is already checked at the edge at the callbacks proxy.
    // We put it here as an extra safety measure (when the callbacks check doesn't work for some reason) and to make security tools happy.
    // Make sure this value is equal or bigger than the  limit set in the callbacks service.
    static final int MAX_CALLBACK_SIZE = 20 * 1024 * 1024;

    private final ProviderCallbackAsyncService providerCallbackService;

    @Internal(callbacks)
    @PostMapping("/callbacks/{providerIdentifier}/{subpath}")
    public ResponseEntity handleCallbackWithSubPath(
            @PathVariable("providerIdentifier") String providerIdentifier,
            @PathVariable("subpath") String subpath,
            @RequestBody @Size(max = MAX_CALLBACK_SIZE) String body) {
        return handleCallback(providerIdentifier, subpath, body);
    }

    @Internal(callbacks)
    @PostMapping("/callbacks/{providerIdentifier}")
    public ResponseEntity handleCallbackWithoutSubPath(
            @PathVariable("providerIdentifier") String providerIdentifier,
            @RequestBody @Size(max = MAX_CALLBACK_SIZE) String body) {
        return handleCallback(providerIdentifier, null, body);
    }


    private ResponseEntity handleCallback(final String provider, @Nullable final String subpath, final String body) {
        providerCallbackService.processCallbackDataAsync(provider, subpath, body);
        return ResponseEntity.ok().build();
    }
}

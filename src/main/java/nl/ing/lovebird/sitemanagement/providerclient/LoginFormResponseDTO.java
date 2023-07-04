package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.Data;
import nl.ing.lovebird.providershared.form.Form;

@Data
public class LoginFormResponseDTO {
    /**
     * Raw JSON response from provider - dataprovider specific format; for submission to the provider.
     */
    private final String providerForm;

    /**
     * Internal Form - generic format; for conversion to DTO objects sent to Yolt app / clients.
     */
    private final Form yoltForm;
}

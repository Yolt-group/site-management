package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsDTO;
import org.springframework.lang.Nullable;

import java.util.UUID;

@Getter
@Setter
public class FormStep extends Step {

    /**
     * The Form in 'yolt format'. That is a normalized format that can be interpreted by our API consumers (e.g. the FE yolt app)
     * This is mandatory and will be used to present to the user.
     */
    private final Form form;

    /**
     * This describes how answers to a form should be encrypted.
     * If left empty, that means there is no encryption necessary.
     */
    private final EncryptionDetailsDTO encryptionDetails;

    /**
     * ONLY FOR SCRAPERS
     *
     * A form in *any* representation. For example the plain json that a scraper provides.
     * This value will never be interpreted by site-management. It is only handed back to providers when submitting back the form with
     * filled in values.
     * Why is this necessary?
     * Let's say we have a form of yodlee format calling 'A'.
     * The yodleeDataProvider is obliged to return this same form to some normalized format 'B' (i.e. the 'Form in this object).
     * During converstion A -> B we lose information since B can only be a subset because we have different formats of A (A1,A2,A3.. Yodlee/BI/saltedge and so on)
     * When the values are filled in, a dataprovider might need the original form because that contains information that was lost in the conversion
     * from A -> B.
     * That's why on submitting the values we return the form in both formats. It's up to the providers what to do with this.
     */
    private String serializedProviderForm;

    /**
     * A stateId. A 'form' is presented when the user adds or updates a user-site. The stateId should come from the 'usersite session'.
     * (i.e. the session where he/she adds the user-site)
     * This stateId should be send back on POST /user-sites, so we know what we were doing by getting the correct user site session.
     */
    private UUID stateId;

    /**
     * This constructor is used upon deserialization. This happens in 2 situations:
     * 1) we get it from providers, which only returns form, encryptiondetails, and optionally, providerstate.
     * 2) it is deserialized from the database. form, encryptiondetails, stateId *should* be there, providerstate and serializedProviderForm are optional.
     * It does not contain a stateId or serializedProviderForm.
     */
    @JsonCreator
    public FormStep(@NonNull @JsonProperty("form") Form form,
                    @Nullable @JsonProperty("providerState") String providerState,
                    @NonNull @JsonProperty("encryptionDetails") EncryptionDetailsDTO encryptionDetails,
                    @Nullable @JsonProperty("serializedProviderForm") String serializedProviderForm,
                    @Nullable @JsonProperty("stateId") UUID stateId) {
        super(providerState);
        this.form = form;
        this.encryptionDetails = encryptionDetails;
        this.serializedProviderForm = serializedProviderForm;
        this.stateId = stateId;
    }
}

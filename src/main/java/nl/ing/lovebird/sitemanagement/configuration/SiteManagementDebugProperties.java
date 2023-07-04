package nl.ing.lovebird.sitemanagement.configuration;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "yolt.site-management.debug")
public class SiteManagementDebugProperties {

    /**
     * On {@link nl.ing.lovebird.sitemanagement.forms.FormValidationException}, do the following:
     * - log the entire form
     * - log the keys sent by the client
     */
    boolean formValidationDetailedErrors;

}

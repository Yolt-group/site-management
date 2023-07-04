package nl.ing.lovebird.sitemanagement.usersite;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringEscapeUtils;

import java.time.Instant;
import java.util.UUID;

@Data
@Table(name = SiteLoginForm.TABLE_NAME)
@AllArgsConstructor
@NoArgsConstructor
public class SiteLoginForm {
    public static final String TABLE_NAME = "site_login_form";
    public static final String ID_COLUMN = "site_id";
    private static final String LOGIN_FORM_JSON_COLUMN = "login_form_json";
    private static final String ALT_LOGIN_FORM_JSON_COLUMN = "alt_login_form_json";
    private static final String LOGIN_FORM_COLUMN = "login_form";
    private static final String UPDATED_COLUMN = "updated";

    @PartitionKey
    @Column(name = ID_COLUMN)
    private UUID siteId;

    /**
     * Raw JSON response from provider - dataprovider specific format; for submission to the provider.
     */
    @Column(name = LOGIN_FORM_JSON_COLUMN)
    private String loginFormJson;

    /**
     * Alternative raw login form as overriden by Yolt instead of the form returned by the provider.
     */
    @Column(name = ALT_LOGIN_FORM_JSON_COLUMN)
    private String altLoginFormJson;

    /**
     * Serialized Form - generic format; for conversion to DTO objects sent to Yolt app / clients.
     */
    @Column(name = LOGIN_FORM_COLUMN)
    private String loginForm;

    @Column(name = UPDATED_COLUMN)
    private Instant updated;

    public String getLoginFormJson() {
        //These 5 backslashes are a bit weird here. We're not sure if this is a bug in Java or just a glitch
        //in time and space. However, it does give us the desired result.
        return StringEscapeUtils.unescapeHtml4(loginFormJson.replaceAll("&quot;", "\\\\\""));
    }
}

package nl.ing.lovebird.sitemanagement.login;

import nl.ing.lovebird.sitemanagement.usersite.SiteLoginForm;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static java.time.Clock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class SiteLoginFormTest {
    private static String M_AND_S_WITH_HTML_CHARACTER = "{\"conjunctionOp\":{\"conjuctionOp\":1},\"componentList\":[{\"valueIdentifier\":\"LOGIN\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_LOGIN\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"LOGIN\",\"displayName\":\"Enter your M&amp;S Money username\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"43228\",\"isOptionalMFA\":false,\"isMFA\":false},{\"valueIdentifier\":\"OP_PASSWORD\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_PASSWORD\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"OP_PASSWORD\",\"displayName\":\"Password\",\"isEditable\":true,\"isOptional\":true,\"isEscaped\":false,\"helpText\":\"43227\",\"isOptionalMFA\":false,\"isMFA\":false}],\"defaultHelpText\":\"10327\"}";
    private static String M_AND_S_WITHOUT_HTML_CHARACTER = "{\"conjunctionOp\":{\"conjuctionOp\":1},\"componentList\":[{\"valueIdentifier\":\"LOGIN\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_LOGIN\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"LOGIN\",\"displayName\":\"Enter your M&S Money username\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"43228\",\"isOptionalMFA\":false,\"isMFA\":false},{\"valueIdentifier\":\"OP_PASSWORD\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_PASSWORD\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"OP_PASSWORD\",\"displayName\":\"Password\",\"isEditable\":true,\"isOptional\":true,\"isEscaped\":false,\"helpText\":\"43227\",\"isOptionalMFA\":false,\"isMFA\":false}],\"defaultHelpText\":\"10327\"}";
    private static String M_AND_S_WITH_QUOTE_CHARACTER = "{\"conjunctionOp\":{\"conjuctionOp\":1},\"componentList\":[{\"valueIdentifier\":\"LOGIN\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_LOGIN\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"LOGIN\",\"displayName\":\"Username / Customer ID\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"If user is before &quot;1 November 2014&quot; give username otherwise customer id\",\"isOptionalMFA\":false,\"isMFA\":false},{\"valueIdentifier\":\"PASSWORD\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_PASSWORD\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"PASSWORD\",\"displayName\":\"Password\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"106318\",\"isOptionalMFA\":false,\"isMFA\":false},{\"valueIdentifier\":\"OP_PASSWORD1\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_PASSWORD\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"OP_PASSWORD1\",\"displayName\":\"Passcode\",\"isEditable\":true,\"isOptional\":true,\"isEscaped\":false,\"helpText\":\"167011\",\"isOptionalMFA\":false,\"isMFA\":false}],\"defaultHelpText\":\"10297\"}";

    @Test
    void loginFormContainingHTMLElementShouldBeUnescaped() {
        SiteLoginForm siteLoginForm = new SiteLoginForm(UUID.randomUUID(), M_AND_S_WITH_HTML_CHARACTER, null, null, Instant.now(systemUTC()));

        assertThat(M_AND_S_WITHOUT_HTML_CHARACTER).isEqualTo(siteLoginForm.getLoginFormJson());
    }

    @Test
    void loginFormContainingHTMLQuoteElementDoesNotResultInInvalidJson() {
        SiteLoginForm siteLoginForm = new SiteLoginForm(UUID.randomUUID(), M_AND_S_WITH_QUOTE_CHARACTER, null, null, Instant.now(systemUTC()));

        assertThat(siteLoginForm.getLoginFormJson()).isNotNull();
    }
}

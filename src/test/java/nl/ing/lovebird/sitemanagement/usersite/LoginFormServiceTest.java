package nl.ing.lovebird.sitemanagement.usersite;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.providershared.form.FormComponent;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.exception.KnownProviderRestClientException;
import nl.ing.lovebird.sitemanagement.exception.UnknownSiteFormException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerclient.LoginFormResponseDTO;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.Clock.fixed;
import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class LoginFormServiceTest {
    private static final String LLOYDS_TEST_JSON_FORM = "{\"conjunctionOp\":{\"conjuctionOp\":1},\"componentList\":[{\"valueIdentifier\":\"LOGIN\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_LOGIN\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"LOGIN\",\"displayName\":\"User ID\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"16934\",\"isOptionalMFA\":false,\"isMFA\":false},{\"valueIdentifier\":\"PASSWORD\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_PASSWORD\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"PASSWORD\",\"displayName\":\"Password\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"16935\",\"isOptionalMFA\":false,\"isMFA\":false},{\"valueIdentifier\":\"PASSWORD1\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_PASSWORD\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"PASSWORD1\",\"displayName\":\"memorable information\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"16933\",\"isOptionalMFA\":false,\"isMFA\":false}],\"defaultHelpText\":\"3059\"}";
    private static final String LLOYDS_TEST_NEW_JSON_FORM = "{\"conjunctionOp\":{\"conjuctionOp\":1},\"componentList\":[{\"valueIdentifier\":\"LOGIN\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_LOGIN\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"LOGIN\",\"displayName\":\"User ID\",\"isEditable\":false,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"16934\",\"isOptionalMFA\":false,\"isMFA\":false},{\"valueIdentifier\":\"PASSWORD\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_PASSWORD\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"PASSWORD\",\"displayName\":\"Password\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"16935\",\"isOptionalMFA\":false,\"isMFA\":false},{\"valueIdentifier\":\"PASSWORD1\",\"valueMask\":\"LOGIN_FIELD\",\"fieldType\":{\"typeName\":\"IF_PASSWORD\"},\"size\":20,\"maxlength\":40,\"fieldInfoType\":\"com.yodlee.common.FieldInfoSingle\",\"name\":\"PASSWORD1\",\"displayName\":\"memorable information\",\"isEditable\":true,\"isOptional\":false,\"isEscaped\":false,\"helpText\":\"16933\",\"isOptionalMFA\":false,\"isMFA\":false}],\"defaultHelpText\":\"3059\"}";
    private static final String SOME_JSON = "{\"key\":\"value\"}";

    private static final UUID SITE_ID1 = UUID.randomUUID();
    private static final Site SITE = createSite();
    private static final ClientId CLIENT_ID1 = ClientId.random();
    private static final Clock NOW = fixed(Instant.now(systemUTC()), ZoneId.systemDefault());
    private static final Clock TWO_DAYS_AGO = fixed(NOW.instant().minus(2, ChronoUnit.DAYS), ZoneId.systemDefault());
    private static final String SITE_NAME = "Santander";

    private final List<FormComponent> formComponents = new ArrayList<>();

    @Mock
    private SiteLoginFormRepository siteLoginFormRepository;

    @Mock
    private FormProviderRestClient formProviderRestClient;

    @Mock
    private Clock clock;

    @Mock
    private ClientUserToken clientUserToken;

    @Mock
    private SiteManagementMetrics siteManagementMetrics;

    private LoginFormService loginFormService;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        when(clock.instant()).thenReturn(NOW.instant());
        when(formProviderRestClient.fetchLoginForm(any(), any(), any(), any())).thenReturn(
                new LoginFormResponseDTO(LLOYDS_TEST_JSON_FORM, new Form(formComponents, null, null))
        );
        when(clientUserToken.getClientIdClaim()).thenReturn(CLIENT_ID1.unwrap());
        loginFormService = new LoginFormService(siteLoginFormRepository, formProviderRestClient, clock, siteManagementMetrics);
    }

    @Test
    @SneakyThrows
    public void getLoginForm_FreshFormInDb() {
        final SiteLoginForm formInDb = new SiteLoginForm();
        formInDb.setLoginFormJson(SOME_JSON);
        formInDb.setUpdated(NOW.instant());
        when(siteLoginFormRepository.selectSiteLogin(SITE_ID1)).thenReturn(formInDb);

        final SiteLoginForm siteForm = loginFormService.getLoginForm(SITE, clientUserToken);

        assertThat(siteForm).isEqualTo(formInDb);
        verify(siteLoginFormRepository).selectSiteLogin(SITE_ID1);
        verify(siteLoginFormRepository, never()).save(any());
        verify(formProviderRestClient, never()).fetchLoginForm(any(), any(), any(), any());
    }


    @Test
    @SneakyThrows
    public void getLoginForm_updatedIsNull() {
        final SiteLoginForm formInDb = new SiteLoginForm();
        formInDb.setLoginFormJson(SOME_JSON);
        formInDb.setUpdated(null);
        when(siteLoginFormRepository.selectSiteLogin(SITE_ID1)).thenReturn(formInDb);

        loginFormService.getLoginForm(SITE, clientUserToken);

        verify(siteLoginFormRepository).selectSiteLogin(SITE_ID1);
        verify(siteLoginFormRepository).save(any());
        verify(formProviderRestClient).fetchLoginForm(any(), any(), any(), any());
    }


    @Test
    @SneakyThrows
    public void getLoginForm_StaleFormInDbAndDifferentJsonFromProvider_ShouldSaveFormAndRemoveAltForm() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        root.addAppender(mockAppender);
        final SiteLoginForm formInDb = new SiteLoginForm();
        formInDb.setLoginFormJson(LLOYDS_TEST_JSON_FORM);
        formInDb.setUpdated(TWO_DAYS_AGO.instant());
        when(siteLoginFormRepository.selectSiteLogin(SITE_ID1)).thenReturn(formInDb);
        when(formProviderRestClient.fetchLoginForm(any(), any(), any(), any())
        ).thenReturn(
                new LoginFormResponseDTO(LLOYDS_TEST_NEW_JSON_FORM, new Form(formComponents, null, null))
        );
        ArgumentCaptor<SiteLoginForm> capturedLoginForm = ArgumentCaptor.forClass(SiteLoginForm.class);

        final SiteLoginForm siteForm = loginFormService.getLoginForm(SITE, clientUserToken);

        verify(siteLoginFormRepository).save(capturedLoginForm.capture());
        SiteLoginForm savedLoginForm = capturedLoginForm.getValue();
        assertThat(siteForm).isEqualTo(savedLoginForm);
        assertThat(savedLoginForm.getSiteId()).isEqualTo(SITE_ID1);
        assertThat(savedLoginForm.getUpdated()).isEqualTo(NOW.instant());
        assertThat(savedLoginForm.getAltLoginFormJson()).isNull();
        assertThat(savedLoginForm.getLoginFormJson()).isEqualTo(LLOYDS_TEST_NEW_JSON_FORM);
        verify(mockAppender).doAppend(argThat(o -> {
            LoggingEvent event = (LoggingEvent) o;
            return event.getFormattedMessage().startsWith("Alternative to YODLEE form for site " + SITE_ID1) && event.getLevel() == Level.WARN;
        }));
    }

    @Test
    @SneakyThrows
    public void testGetLoginForm_StaleFormInDbAndSameJsonFromProvider_ShouldOnlyUpdateDate() {
        final SiteLoginForm formInDb = new SiteLoginForm();
        formInDb.setSiteId(SITE_ID1);
        formInDb.setUpdated(Instant.now(TWO_DAYS_AGO));
        formInDb.setAltLoginFormJson(SOME_JSON);
        formInDb.setLoginFormJson(LLOYDS_TEST_JSON_FORM);
        formInDb.setLoginForm(SOME_JSON);
        when(siteLoginFormRepository.selectSiteLogin(SITE_ID1)).thenReturn(formInDb);
        when(formProviderRestClient.fetchLoginForm(any(), any(), any(), any())
        ).thenReturn(
                new LoginFormResponseDTO(LLOYDS_TEST_JSON_FORM, new Form(formComponents, null, null))
        );
        ArgumentCaptor<SiteLoginForm> capturedLoginForm = ArgumentCaptor.forClass(SiteLoginForm.class);

        final SiteLoginForm siteForm = loginFormService.getLoginForm(SITE, clientUserToken);

        verify(siteLoginFormRepository).save(capturedLoginForm.capture());
        SiteLoginForm savedLoginForm = capturedLoginForm.getValue();
        assertThat(siteForm).isEqualTo(savedLoginForm);
        assertThat(savedLoginForm.getSiteId()).isEqualTo(SITE_ID1);
        assertThat(savedLoginForm.getUpdated()).isEqualTo(NOW.instant());
        assertThat(savedLoginForm.getAltLoginFormJson()).isEqualTo(SOME_JSON);
        assertThat(savedLoginForm.getLoginFormJson()).isEqualTo(LLOYDS_TEST_JSON_FORM);
    }


    @Test
    @SneakyThrows
    public void testGetLoginForm_StaleFormInDbAndFetchFromProviderFail_ShouldNotTouchDb() {
        final SiteLoginForm formInDb = new SiteLoginForm();
        formInDb.setLoginFormJson(SOME_JSON);
        formInDb.setUpdated(Instant.now(TWO_DAYS_AGO));
        when(siteLoginFormRepository.selectSiteLogin(SITE_ID1)).thenReturn(formInDb);
        when(formProviderRestClient.fetchLoginForm(any(), any(), any(), any())).thenThrow(new KnownProviderRestClientException(""));

        final SiteLoginForm siteForm = loginFormService.getLoginForm(SITE, clientUserToken);

        verify(siteLoginFormRepository, never()).save(any());
        assertThat(siteForm).isEqualTo(formInDb);
    }

    @Test
    void testGetLoginForm_NotFoundInDbAndFormFromProvider_ShouldSaveNewForm() {
        when(siteLoginFormRepository.selectSiteLogin(SITE_ID1)).thenReturn(null);
        ArgumentCaptor<SiteLoginForm> capturedLoginForm = ArgumentCaptor.forClass(SiteLoginForm.class);

        final SiteLoginForm siteForm = loginFormService.getLoginForm(SITE, clientUserToken);

        verify(siteLoginFormRepository).save(capturedLoginForm.capture());
        SiteLoginForm savedLoginForm = capturedLoginForm.getValue();
        assertThat(siteForm).isEqualTo(savedLoginForm);
        assertThat(savedLoginForm.getSiteId()).isEqualTo(SITE_ID1);
        assertThat(savedLoginForm.getUpdated()).isEqualTo(NOW.instant());
        assertThat(savedLoginForm.getAltLoginFormJson()).isNull();
        assertThat(savedLoginForm.getLoginFormJson()).isEqualTo(LLOYDS_TEST_JSON_FORM);
    }

    @Test
    @SneakyThrows
    public void testGetLoginForm_NotFoundInDbAndFetchFromProviderFail_ShouldThrowException() {
        assertThatThrownBy(() -> {
            when(siteLoginFormRepository.selectSiteLogin(SITE_ID1)).thenReturn(null);
            when(formProviderRestClient.fetchLoginForm(any(), any(), any(), any())).thenThrow(new KnownProviderRestClientException(""));
            loginFormService.getLoginForm(SITE, clientUserToken);
        }).isInstanceOf(UnknownSiteFormException.class);
    }

    private static Site createSite() {
        return SiteCreatorUtil.createTestSite(SITE_ID1.toString().toString(), SITE_NAME, "YODLEE", List.of(), List.of(), Map.of());
    }

}

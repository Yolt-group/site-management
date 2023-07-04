package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.providershared.form.FormField;
import nl.ing.lovebird.providershared.form.SectionFormComponent;
import nl.ing.lovebird.providershared.form.TextField;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserSitePersistedFormAnswersServiceTest {

    UserSiteService userSiteService = Mockito.mock(UserSiteService.class);
    static ObjectMapper objectMapper = new ObjectMapper();

    UserSitePersistedFormAnswersService userSitePersistedFormAnswersService = new UserSitePersistedFormAnswersService(userSiteService, new Jackson2ObjectMapperBuilder());

    @Test
    void given_simpleFormWithNoFieldsToPersist_when_persistFormFieldAnswers_then_NoAnswerIsStored() throws JsonProcessingException {
        TextField textField = new TextField("q", "question", 8, 8, true, "^$", false);
        Form form = new Form(List.of(textField), null, Collections.emptyMap());
        FormStep formStep = new FormStep(form, null, EncryptionDetailsDTO.NONE, null, null);

        Map<String, String> answers = Map.of("q", "a");

        userSitePersistedFormAnswersService.persistFormFieldAnswers(
                objectMapper.writeValueAsString(formStep),
                mock(PostgresUserSite.class),
                answers
        );

        verify(userSiteService, times(0)).updateUserSitePersistedFields(any(), any());
    }

    @Test
    void given_simpleFormWithFieldToPersist_when_persistFormFieldAnswers_then_AnswerIsStored() throws JsonProcessingException {
        TextField textField = new TextField("q", "question", 8, 8, true, "^$", true);
        Form form = new Form(List.of(textField), null, Collections.emptyMap());
        FormStep formStep = new FormStep(form, null, EncryptionDetailsDTO.NONE, null, null);

        Map<String, String> answers = Map.of("q", "a");

        userSitePersistedFormAnswersService.persistFormFieldAnswers(
                objectMapper.writeValueAsString(formStep),
                mock(PostgresUserSite.class),
                answers
        );

        ArgumentCaptor<Map<String, String>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(userSiteService).updateUserSitePersistedFields(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue()).isEqualTo(answers);
    }

    /**
     * This tests that {@link UserSitePersistedFormAnswersService#flatten} works
     */
    @Test
    void given_nestedFormWithMultipleFieldsToPersist_when_persistFormFieldAnswers_then_AnswerIsStored() throws JsonProcessingException {
        TextField textFieldA = new TextField("q1", "question", 8, 8, true, "^$", true);
        TextField textFieldB = new TextField("q2", "question", 8, 8, true, "^$", true);
        SectionFormComponent section = new SectionFormComponent("section", "hoi", false);
        section.addComponent(textFieldB);
        Form form = new Form(List.of(textFieldA, section), null, Collections.emptyMap());
        FormStep formStep = new FormStep(form, null, EncryptionDetailsDTO.NONE, null, null);

        Map<String, String> answers = Map.of("q1", "a1", "q2", "a2");

        userSitePersistedFormAnswersService.persistFormFieldAnswers(
                objectMapper.writeValueAsString(formStep),
                mock(PostgresUserSite.class),
                answers
        );

        ArgumentCaptor<Map<String, String>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(userSiteService).updateUserSitePersistedFields(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue()).isEqualTo(answers);
    }

    @Test
    void given_formWithFieldAndPersistedAnswer_when_canCompleteFormStepWithoutUserIntervention_then_true() {
        TextField textField = new TextField("q", "question", 8, 8, true, "^$", true);
        Form form = new Form(List.of(textField), null, Collections.emptyMap());
        FormStep formStep = new FormStep(form, null, EncryptionDetailsDTO.NONE, null, null);

        PostgresUserSite userSite = new PostgresUserSite();
        userSite.setPersistedFormStepAnswers(Map.of("q", "a"));

        assertThat(UserSitePersistedFormAnswersService.canCompleteFormStepWithoutUserIntervention(
                formStep,
                userSite
        )).isTrue();
    }

    @Test
    void given_formWithMultipleFieldsAndNotAllPersistedAnswer_when_canCompleteFormStepWithoutUserIntervention_then_false() {
        TextField textField = new TextField("q1", "question1", 8, 8, true, "^$", true);
        TextField textField2 = new TextField("q2", "question2", 8, 8, true, "^$", true);
        Form form = new Form(List.of(textField, textField2), null, Collections.emptyMap());
        FormStep formStep = new FormStep(form, null, EncryptionDetailsDTO.NONE, null, null);

        PostgresUserSite userSite = new PostgresUserSite();
        userSite.setPersistedFormStepAnswers(Map.of("q1", "a"));

        assertThat(UserSitePersistedFormAnswersService.canCompleteFormStepWithoutUserIntervention(
                formStep,
                userSite
        )).isFalse();
    }

    @Test
    void given_formWithFieldAndNoPersistedAnswers_when_canCompleteFormStepWithoutUserIntervention_then_false() {
        TextField textField = new TextField("q", "question", 8, 8, false, "^$", true);
        Form form = new Form(List.of(textField), null, Collections.emptyMap());
        FormStep formStep = new FormStep(form, null, EncryptionDetailsDTO.NONE, null, null);

        PostgresUserSite userSite = new PostgresUserSite();
        userSite.setPersistedFormStepAnswers(Collections.emptyMap());

        assertThat(UserSitePersistedFormAnswersService.canCompleteFormStepWithoutUserIntervention(
                formStep,
                userSite
        )).isFalse();
    }

    @Test
    void given_formWithRequiredFieldAndNullPersistedAnswers_when_canCompleteFormStepWithoutUserIntervention_then_false() {
        TextField textField = new TextField("q", "question", 8, 8, false, "^$", true);
        Form form = new Form(List.of(textField), null, Collections.emptyMap());
        FormStep formStep = new FormStep(form, null, EncryptionDetailsDTO.NONE, null, null);

        PostgresUserSite userSite = new PostgresUserSite();
        userSite.setPersistedFormStepAnswers(null);

        assertThat(UserSitePersistedFormAnswersService.canCompleteFormStepWithoutUserIntervention(
                formStep,
                userSite
        )).isFalse();
    }

    /**
     * Test that {@link UserSitePersistedFormAnswersService#flatten} handles a slightly "exotic" form well.
     */
    @Test
    void given_nestedForm_when_flatten_then_listOfFieldsIsReturned() {
        TextField textField1 = new TextField("q1", "question1", 8, 8, true, "^$", true);
        TextField textField2 = new TextField("q2", "question2", 8, 8, true, "^$", true);
        SectionFormComponent section1 = new SectionFormComponent("section1", "hoi", false);
        SectionFormComponent section2 = new SectionFormComponent("section2", "hoi", false);

        section1.addComponent(section2);
        section2.addComponent(textField2);

        List<FormField> result = UserSitePersistedFormAnswersService.flatten(List.of(textField1, section1));
        assertThat(result).isEqualTo(List.of(textField1, textField2));
    }

}
package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.providershared.form.FormComponent;
import nl.ing.lovebird.providershared.form.FormContainer;
import nl.ing.lovebird.providershared.form.FormField;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static nl.ing.lovebird.providershared.form.FormComponent.ComponentType.CONTAINER;
import static nl.ing.lovebird.providershared.form.FormComponent.ComponentType.FIELD;

@Slf4j
@Component
public class UserSitePersistedFormAnswersService {

    private final UserSiteService userSiteService;
    private final ObjectMapper objectMapper;

    public UserSitePersistedFormAnswersService(UserSiteService userSiteService, Jackson2ObjectMapperBuilder objectMapperBuilder) {
        this.userSiteService = userSiteService;
        this.objectMapper = objectMapperBuilder
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public void persistFormFieldAnswers(@NonNull String serializedFormStep, PostgresUserSite userSite, @NonNull Map<String, String> filledInFormValues) {
        final Set<String> keysToPersist;
        try {
            final FormStep formStep = objectMapper.readValue(serializedFormStep, FormStep.class);
            final List<FormField> formFields = flatten(formStep.getForm().getFormComponents());
            keysToPersist = formFields.stream()
                    .filter(FormComponent::isPersist)
                    .map(FormField::getId)
                    .collect(Collectors.toSet());
        } catch (JsonProcessingException e) {
            log.error("persistFormFieldAnswers: couldn't deserialize FormStep in ConsentSession", e);
            return;
        }

        // None of the answers have to be stored for later re-use.
        if (keysToPersist.isEmpty()) {
            return;
        }

        // Create map of FormField#id -> Answer
        final Map<String, String> fieldsToPersist = filledInFormValues.entrySet().stream()
                .filter(e -> keysToPersist.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (keysToPersist.size() != fieldsToPersist.size()) {
            log.warn("keysToPersist is {}, however: not all answers are available, saving answers for these keys: {}.", keysToPersist, fieldsToPersist.keySet()); //NOSHERIFF
        }

        // Persist on UserSite.
        userSiteService.updateUserSitePersistedFields(userSite, fieldsToPersist);
    }

    /**
     * We can complete a FormStep on behalf of a user if we have a persisted answer for every FormField (regardless of whether it is optional or required)
     */
    public static boolean canCompleteFormStepWithoutUserIntervention(@NonNull FormStep formStep, PostgresUserSite userSite) {
        if (userSite.getPersistedFormStepAnswers() == null) {
            return false;
        }

        Set<String> fieldIds = listFormFieldKeys(formStep.getForm());

        return userSite.getPersistedFormStepAnswers().keySet().containsAll(fieldIds);
    }

    public static Set<String> listFormFieldKeys(@NonNull Form form) {
        return flatten(form.getFormComponents()).stream()
                .map(FormField::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Flatten a tree of {@link FormComponent}s into a list of its descendant {@link FormField}s.
     */
    static List<FormField> flatten(List<FormComponent> formComponents) {
        List<FormField> formFields = new ArrayList<>();
        for (FormComponent component : formComponents) {
            if (component.getComponentType() == CONTAINER) {
                List<FormField> childFields = flatten(((FormContainer) component).getChildComponents());
                formFields.addAll(childFields);
            } else if (component.getComponentType() == FIELD) {
                formFields.add((FormField) component);
            } else {
                throw new IllegalStateException("Unknown componentType.");
            }
        }
        return formFields;
    }

}

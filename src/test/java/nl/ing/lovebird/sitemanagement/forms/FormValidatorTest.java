package nl.ing.lovebird.sitemanagement.forms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import nl.ing.lovebird.providershared.form.*;
import nl.ing.lovebird.sitemanagement.configuration.SiteManagementDebugProperties;
import nl.ing.lovebird.sitemanagement.usersite.LoginDTO;
import nl.ing.lovebird.sitemanagement.usersite.FormLogin;
import nl.ing.lovebird.sitemanagement.usersite.Login;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.JsonComponentModule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class FormValidatorTest {


    private static final ObjectMapper objectMapper;
    private static final SiteManagementDebugProperties siteManagementDebugProperties = new SiteManagementDebugProperties();

    static {
        siteManagementDebugProperties.setFormValidationDetailedErrors(true);
        objectMapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .setDateFormat(new StdDateFormat().withColonInTimeZone(false))
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .registerModule(new ParameterNamesModule())
                .registerModule(new JsonComponentModule());
    }

    @Test
    void testYoltProviderFormStepPersonaIssue() throws JsonProcessingException, FormValidationException {
        // This happened on a real environment.
        // Added this test to fix:
        // {"code":"SM043","message":"Invalid form: You must at least fill in one of the following set of fields (a set is seperated by '|' ) : iban-field+text-area+password-field"}.
        // Those fields are all optional, so that message is invalid.
        Form form = objectMapper.readValue("{\"formComponents\":[{\"type\":\"SECTION\",\"id\":\"section-choice-components\",\"displayName\":\"Choice components\",\"optional\":false,\"childComponents\":[{\"type\":\"CHOICE\",\"id\":\"required-choice-form-component\",\"displayName\":\"Fill in exactly 1 of these fields.\",\"optional\":false,\"childComponents\":[{\"type\":\"TEXT\",\"id\":\"required-choice-form-component-a\",\"displayName\":\"A\",\"optional\":true,\"length\":1,\"maxLength\":1,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"},{\"type\":\"TEXT\",\"id\":\"required-choice-form-component-b\",\"displayName\":\"B\",\"optional\":true,\"length\":1,\"maxLength\":1,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"}],\"containerType\":\"CHOICE\",\"componentType\":\"CONTAINER\"},{\"type\":\"CHOICE\",\"id\":\"optional-choice-form-component\",\"displayName\":\"Fill in at most 1 of these fields.\",\"optional\":true,\"childComponents\":[{\"type\":\"TEXT\",\"id\":\"optional-choice-form-component-a\",\"displayName\":\"A\",\"optional\":true,\"length\":1,\"maxLength\":1,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"},{\"type\":\"TEXT\",\"id\":\"optional-choice-form-component-b\",\"displayName\":\"B\",\"optional\":true,\"length\":1,\"maxLength\":1,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"}],\"containerType\":\"CHOICE\",\"componentType\":\"CONTAINER\"}],\"containerType\":\"SECTION\",\"componentType\":\"CONTAINER\"},{\"type\":\"SECTION\",\"id\":\"section-multiline-components\",\"displayName\":\"Multiline components\",\"optional\":false,\"childComponents\":[{\"type\":\"MULTI\",\"id\":\"multi-form-component\",\"displayName\":\"Display A and B on single line.\",\"optional\":true,\"childComponents\":[{\"type\":\"TEXT\",\"id\":\"multi-form-component-a\",\"displayName\":\"A\",\"optional\":true,\"length\":1,\"maxLength\":1,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"},{\"type\":\"TEXT\",\"id\":\"multi-form-component-b\",\"displayName\":\"B\",\"optional\":true,\"length\":1,\"maxLength\":1,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"}],\"containerType\":\"MULTI\",\"componentType\":\"CONTAINER\"}],\"containerType\":\"SECTION\",\"componentType\":\"CONTAINER\"},{\"type\":\"SECTION\",\"id\":\"section-date-fields\",\"displayName\":\"Date fields\",\"optional\":false,\"childComponents\":[{\"type\":\"DATE\",\"id\":\"required-date-field\",\"displayName\":\"Date\",\"optional\":false,\"dateFormat\":\"yyyy-MM-dd\",\"fieldType\":\"DATE\",\"componentType\":\"FIELD\"},{\"type\":\"DATE\",\"id\":\"date-field-after-2000\",\"displayName\":\"Date after 2000-01-01\",\"optional\":true,\"minDate\":\"2000-01-01\",\"dateFormat\":\"yyyy-MM-dd\",\"fieldType\":\"DATE\",\"componentType\":\"FIELD\"},{\"type\":\"DATE\",\"id\":\"date-field-before-2000\",\"displayName\":\"Date before 2000-01-01\",\"optional\":true,\"maxDate\":\"2000-01-01\",\"dateFormat\":\"yyyy-MM-dd\",\"fieldType\":\"DATE\",\"componentType\":\"FIELD\"},{\"type\":\"DATE\",\"id\":\"date-field-dd-MM-yyyy\",\"displayName\":\"Date as dd-MM-yyyy\",\"optional\":true,\"dateFormat\":\"dd-MM-yyyy\",\"fieldType\":\"DATE\",\"componentType\":\"FIELD\"}],\"containerType\":\"SECTION\",\"componentType\":\"CONTAINER\"},{\"type\":\"SECTION\",\"id\":\"section-number-fields\",\"displayName\":\"Number fields\",\"optional\":false,\"childComponents\":[{\"type\":\"NUMBER\",\"id\":\"number-field-gt-100\",\"displayName\":\"Number > 100\",\"optional\":true,\"min\":100,\"max\":1000000,\"stepSize\":0.01,\"fieldType\":\"NUMBER\",\"componentType\":\"FIELD\"},{\"type\":\"NUMBER\",\"id\":\"number-field-lt-100\",\"displayName\":\"Number < 100\",\"optional\":true,\"min\":-1000000,\"max\":100,\"stepSize\":0.01,\"fieldType\":\"NUMBER\",\"componentType\":\"FIELD\"},{\"type\":\"NUMBER\",\"id\":\"number-field-gt-100\",\"displayName\":\"Whole number\",\"optional\":true,\"min\":-100000,\"max\":100000,\"stepSize\":1,\"fieldType\":\"NUMBER\",\"componentType\":\"FIELD\"},{\"type\":\"NUMBER\",\"id\":\"number-field-with-default\",\"displayName\":\"Default 10\",\"optional\":true,\"min\":1,\"max\":100,\"stepSize\":0.01,\"defaultValue\":10,\"fieldType\":\"NUMBER\",\"componentType\":\"FIELD\"}],\"containerType\":\"SECTION\",\"componentType\":\"CONTAINER\"},{\"type\":\"SECTION\",\"id\":\"section-radio-check-fields\",\"displayName\":\"Radiobuttons and checkboxes\",\"optional\":false,\"childComponents\":[{\"type\":\"RADIO\",\"id\":\"radio-field\",\"displayName\":\"Radio\",\"optional\":true,\"selectOptionValues\":[{\"displayName\":\"A\",\"value\":\"A\"},{\"displayName\":\"B\",\"value\":\"B\"}],\"fieldType\":\"RADIO\",\"componentType\":\"FIELD\"},{\"type\":\"RADIO\",\"id\":\"radio-field-with-default\",\"displayName\":\"Radio with default\",\"optional\":true,\"selectOptionValues\":[{\"displayName\":\"A\",\"value\":\"A\"},{\"displayName\":\"B\",\"value\":\"B\"}],\"defaultValue\":{\"value\":\"A\",\"displayName\":\"A\"},\"fieldType\":\"RADIO\",\"componentType\":\"FIELD\"},{\"type\":\"SELECT\",\"id\":\"select-field\",\"displayName\":\"Select\",\"optional\":true,\"selectOptionValues\":[{\"displayName\":\"A\",\"value\":\"A\"},{\"displayName\":\"B\",\"value\":\"B\"}],\"length\":5,\"maxLength\":5,\"fieldType\":\"SELECT\",\"componentType\":\"FIELD\"},{\"type\":\"SELECT\",\"id\":\"select-field-with-default\",\"displayName\":\"Select with default\",\"optional\":true,\"selectOptionValues\":[{\"displayName\":\"A\",\"value\":\"A\"},{\"displayName\":\"B\",\"value\":\"B\"}],\"length\":5,\"maxLength\":5,\"fieldType\":\"SELECT\",\"componentType\":\"FIELD\"}],\"containerType\":\"SECTION\",\"componentType\":\"CONTAINER\"},{\"type\":\"SECTION\",\"id\":\"text-fields\",\"displayName\":\"Text fields\",\"optional\":false,\"childComponents\":[{\"type\":\"TEXT\",\"id\":\"text-field\",\"displayName\":\"Text\",\"optional\":true,\"length\":5,\"maxLength\":5,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"},{\"type\":\"TEXT\",\"id\":\"text-field-only-lowercase-az\",\"displayName\":\"Only [a-z]\",\"optional\":true,\"length\":5,\"maxLength\":5,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"},{\"type\":\"TEXT\",\"id\":\"text-field-only-numbers\",\"displayName\":\"Only [a-z]\",\"optional\":true,\"length\":5,\"maxLength\":5,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"}],\"containerType\":\"SECTION\",\"componentType\":\"CONTAINER\"},{\"type\":\"SECTION\",\"id\":\"section-miscellaneous-fields\",\"displayName\":\"Miscellaneous fields\",\"optional\":false,\"childComponents\":[{\"type\":\"IBAN\",\"id\":\"iban-field\",\"displayName\":\"IBAN\",\"optional\":true,\"fieldType\":\"IBAN\",\"componentType\":\"FIELD\"},{\"type\":\"PASSWORD\",\"id\":\"password-field\",\"displayName\":\"Password\",\"optional\":true,\"length\":10,\"maxLength\":10,\"fieldType\":\"PASSWORD\",\"componentType\":\"FIELD\"},{\"type\":\"TEXT_AREA\",\"id\":\"text-area\",\"displayName\":\"Text area\",\"optional\":true,\"maxLength\":100,\"rows\":5,\"fieldType\":\"TEXT_AREA\",\"componentType\":\"FIELD\"}],\"containerType\":\"SECTION\",\"componentType\":\"CONTAINER\"}],\"explanationField\":{\"type\":\"EXPLANATION\",\"id\":\"explanation-field-id\",\"displayName\":\"Explanation field.\",\"explanation\":\"Please fill out this form.\",\"optional\":true,\"componentType\":\"FIELD\",\"fieldType\":\"EXPLANATION\"},\"stateId\":\"be027555-621e-4894-b502-1baccdfa2003\"}".replace("childComponents", "formComponents"), Form.class);

        LoginDTO loginDTO = objectMapper.readValue("{\"siteId\":\"333e1b97-1055-4b86-a112-bc1db801145f\",\"filledInFormValues\":[{\"fieldId\":\"required-choice-form-component\",\"value\":\"on\"},{\"fieldId\":\"required-choice-form-component-a\",\"value\":\"A\"},{\"fieldId\":\"optional-choice-form-component\",\"value\":\"on\"},{\"fieldId\":\"optional-choice-form-component-a\",\"value\":\"A\"},{\"fieldId\":\"multi-form-component-a\",\"value\":\"A\"},{\"fieldId\":\"multi-form-component-b\",\"value\":\"B\"},{\"fieldId\":\"required-date-field\",\"value\":\"0001-01-01\"},{\"fieldId\":\"date-field-after-2000\",\"value\":\"2011-01-01\"},{\"fieldId\":\"date-field-before-2000\",\"value\":\"0001-01-01\"},{\"fieldId\":\"number-field-with-default\",\"value\":\"10\"},{\"fieldId\":\"radio-field\",\"value\":\"B\"},{\"fieldId\":\"radio-field-with-default\",\"value\":\"A\"},{\"fieldId\":\"text-field\",\"value\":\"test\"}]}", LoginDTO.class);
        Login login = loginDTO.toLogin(UUID.randomUUID());
        FormLogin formLogin = (FormLogin) login;
        new FormValidator(false, siteManagementDebugProperties).validateValues(form, formLogin.getFilledInUserSiteFormValues());
    }

    @Test
    void testSuccessFullValidation() throws FormValidationException {
        List<FormComponent> allVariationsOfFormComponents = TestFormUtil.createAllVariationsOfFormComponents();

        FilledInUserSiteFormValues filledInUserSiteFormValues = new FilledInUserSiteFormValues();
        filledInUserSiteFormValues.add("user1", "bla");
        filledInUserSiteFormValues.add("password1", "bla");
        filledInUserSiteFormValues.add("multi-user1", "1");
        filledInUserSiteFormValues.add("multi-password1", "2");
        filledInUserSiteFormValues.add("multi-select1", "option1");
        filledInUserSiteFormValues.add("select1", "option2");
        filledInUserSiteFormValues.add("main-multi-user1", "bla");
        filledInUserSiteFormValues.add("main-multi-user2", "bla");
        filledInUserSiteFormValues.add("radioId", "option1");
        filledInUserSiteFormValues.add("age", "1990-03-23");
        filledInUserSiteFormValues.add("someNumber", "1");

        new FormValidator(false, siteManagementDebugProperties).validateValues(new Form(allVariationsOfFormComponents, null, null), filledInUserSiteFormValues);

    }

    @Test
    void testSuccessValidationRequirednessOnly() throws FormValidationException {
        List<FormComponent> allVariationsOfFormComponents = TestFormUtil.createAllVariationsOfFormComponents();

        FilledInUserSiteFormValues filledInUserSiteFormValues = new FilledInUserSiteFormValues();
        filledInUserSiteFormValues.add("user1", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("password1", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("multi-user1", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("multi-password1", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("multi-select1", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("select1", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("main-multi-user1", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("main-multi-user2", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("radioId", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("age", "SOMEVERYLONGENCRYPTEDVALUE");
        filledInUserSiteFormValues.add("someNumber", "SOMEVERYLONGENCRYPTEDVALUE");

        new FormValidator(true, siteManagementDebugProperties).validateValues(new Form(allVariationsOfFormComponents, null, null), filledInUserSiteFormValues);
    }

    @Test
    void testComplexFormWithChoices() throws FormValidationException {
        // Special case. *ONLY* 1 of the child components are allowed to be entered. (However, the chosen option could be a container..)
        // Consider the following tree:
        //                               CHOICE-A
        //			 FIELD-A*                               CHOICE-B
        //
        //						       CHOICE-C                MULTI                 SECTION
        //				           FIELD-B* FIELD-C*        FIELD-D/FIELD-E      FIELD-F     CHOICE-D
        //                                                                               FIELD-G*  FIELD-H*

        // The user is now allowed to *only* enter field A(optional), or B(optional), or C(optional), or D+E, or F+G(optional), or F+H(optional).

        final List<FormComponent> formComponents = new ArrayList<>();

        ChoiceFormComponent choiceA = new ChoiceFormComponent("choiceA", "choiceA", false);
        ChoiceFormComponent choiceB = new ChoiceFormComponent("choiceB", "choiceB", false);
        ChoiceFormComponent choiceC = new ChoiceFormComponent("choiceC", "choiceC", false);
        ChoiceFormComponent choiceD = new ChoiceFormComponent("choiceD", "choiceD", false);
        MultiFormComponent multi = new MultiFormComponent("multi", "multi", false);
        SectionFormComponent section = new SectionFormComponent("id", "section", true);

        choiceA.addComponent(new TextField("fieldA", "fieldA", 10, 100, true, false));
        choiceA.addComponent(choiceB);
        choiceB.addComponent(choiceC);
        choiceB.addComponent(multi);
        choiceB.addComponent(section);
        choiceC.addComponent(new TextField("fieldB", "fieldB", 10, 100, true, false));
        choiceC.addComponent(new TextField("fieldC", "fieldC", 10, 100, true, false));
        multi.addComponent(new TextField("fieldD", "fieldD", 100, 100, false, false));
        multi.addComponent(new TextField("fieldE", "fieldE", 100, 100, false, false));
        section.addComponent(new TextField("fieldF", "fieldF", 100, 100, false, false));
        section.addComponent(choiceD);
        choiceD.addComponent(new TextField("fieldG", "fieldG", 100, 100, true, false));
        choiceD.addComponent(new TextField("fieldH", "fieldH", 100, 100, true, false));

        formComponents.add(choiceA);
        Form form = new Form(formComponents, null, null);
        FormValidator formValidator = new FormValidator(false, siteManagementDebugProperties);

        formValidator.validateValues(form, filledInValues(entry("fieldA", "answer")));
        formValidator.validateValues(form, filledInValues(entry("fieldB", "answer")));
        formValidator.validateValues(form, filledInValues(entry("fieldC", "answer")));
        formValidator.validateValues(form, filledInValues(entry("fieldD", "answer"), entry("fieldE", "answer")));
        formValidator.validateValues(form, filledInValues(entry("fieldF", "answer"), entry("fieldG", "answer")));
        formValidator.validateValues(form, filledInValues(entry("fieldF", "answer"), entry("fieldH", "answer")));
        formValidator.validateValues(form, filledInValues(entry("fieldF", "answer"))); // G and H are optional, so might not be provided.

        try {
            formValidator.validateValues(form, filledInValues(entry("fieldA", "answer"), entry("fieldE", "answer")));
            fail("expected validation exception");
        } catch (FormValidationException e) {
            final String expectedMessageStart = "Invalid form: You must at least fill in one of the following set of fields (a set is seperated by '|' ) : ";
            assertThat(e.getMessage()).contains(expectedMessageStart);
            // Following assertion is pretty shitty.. but we're dealing with unordered sets, so they can apear in any order..
            // It basically assert that it says  " Invalid form: Missing one of the values for either: fieldA | fieldB | fieldC | fieldD,FieldE | ..
            Set<Set<String>> expectedSets = new HashSet<>();
            expectedSets.add(Set.of("fieldA"));
            expectedSets.add(Set.of("fieldB"));
            expectedSets.add(Set.of("fieldC"));
            expectedSets.add(Set.of("fieldD", "fieldE"));
            expectedSets.add(Set.of("fieldF", "fieldG"));
            expectedSets.add(Set.of("fieldF", "fieldH"));
            assertExceptionMessageEndsWithPossibleSets(e.getMessage().substring(expectedMessageStart.length()), " \\| ", "\\+", expectedSets);
        }

        try {
            formValidator.validateValues(form, filledInValues());
        } catch (FormValidationException e) {
            fail("This should not throw an exception, bececause it's ok to 'choose' A. Since that field is optional. No given value is also fine.");
        }

        try {
            formValidator.validateValues(form, filledInValues(entry("fieldA", "someAnswerThatIsOver100Characters.ThereforeMakingItFail" +
                    "TheValidationBecauseTheMaxlengthOfFieldAIsLimitedTo100Characters.")));
            fail("expected validation exception");
        } catch (FormValidationException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid form: The value of field with id fieldA exceeds the maximum length of 100"); // might be in different order.
        }
    }

    @Test
    void testTextField() {
        TextField textField = new TextField("id", "displayname", 10, 10, true, "[0-9]*", false);
        testField(textField, "1234567890", true);
        testField(textField, "12345678901", false, "Invalid form: The value of field with id id exceeds the maximum length of 10");
        testField(textField, "NotANumber", false, "Invalid form: The value of field with id id does not match regex [0-9]*");
    }

    @Test
    void testDateField() {
        DateField dateField = new DateField("id", "displayname", false, "yyyy-MM-dd", false);
        dateField.setMaxDate(LocalDate.of(2018, 12, 18));
        dateField.setMinDate(LocalDate.of(1990, 1, 1));
        testField(dateField, "1990-03-23", true);
        // invalid formats
        testField(dateField, "1990-23-03", false, "Invalid form: Invalid date format for field with id id. Expected format: yyyy-MM-dd");
        testField(dateField, "1990/03/03", false, "Invalid form: Invalid date format for field with id id. Expected format: yyyy-MM-dd");
        testField(dateField, "23-03-1990", false, "Invalid form: Invalid date format for field with id id. Expected format: yyyy-MM-dd");
        testField(dateField, "23-03-1990", false, "Invalid form: Invalid date format for field with id id. Expected format: yyyy-MM-dd");

        // invalid range,"test"
        testField(dateField, "1800-03-23", false, "Invalid form: Date of field with id id should be above 1990-01-01");
        testField(dateField, "2019-03-23", false, "Invalid form: Date of field with id id should be below 2018-12-18");
    }

    @Test
    void testPasswordField() {
        PasswordField passwordField = new PasswordField("id", "password: ", 10, 10, false, "[a-zA-Z]*");
        testField(passwordField, "myPassword", true);
        testField(passwordField, "myPasswordTooLong", false, "Invalid form: The value of field with id id exceeds the maximum length of 10");
        testField(passwordField, "112341", false, "Invalid form: The value of field with id id does not match regex [a-zA-Z]*");
    }

    @Test
    void testIbanField() {
        IbanField ibanField = new IbanField("id", "iban: ", false, false);
        testField(ibanField, "NL02ABNA0457180536", true);
        testField(ibanField, "NL02 ABNA 0457 1805 36", false, "Invalid form: The value of field with id id is not a valid IBAN.");
        testField(ibanField, "NL02ABNA0457180537", false, "Invalid form: The value of field with id id is not a valid IBAN.");
        testField(ibanField, "0457180537", false, "Invalid form: The value of field with id id is not a valid IBAN.");

    }

    @Test
    void testNumberField() {
        NumberField numberField = new NumberField("id", "number: ", false, new BigDecimal("-10"), new BigDecimal("100"), new BigDecimal("0.1"), false);
        testField(numberField, "-10", true);
        testField(numberField, "-10.0", true);
        testField(numberField, "-9.9", true);
        testField(numberField, "100", true);
        testField(numberField, "100.1", false, "Invalid form: The value of field with id id should be lower than 100");
        testField(numberField, "-10.1", false, "Invalid form: The value of field with id id should be higher than -10");
        testField(numberField, "1.15", false, "Invalid form: The number of decimals of field with id id is too high. Maximum stepsize: 0.1. Allowed nr of decimals: 1");
    }

    @Test
    void testRadioAndSelectFields() {
        SelectField selectField = new SelectField("id", "choose something", 1, 1, false, false);
        RadioField radioField = new RadioField("id", "choose something", false, false);

        SelectOptionValue value1 = new SelectOptionValue("value1", "displayName");
        SelectOptionValue value2 = new SelectOptionValue("value2", "displayName");
        selectField.addSelectOptionValue(value1);
        selectField.addSelectOptionValue(value2);
        radioField.addSelectOptionValue(value1);
        radioField.addSelectOptionValue(value2);
        testField(selectField, "value1", true);
        testField(radioField, "value1", true);
        testField(selectField, "value2", true);
        testField(radioField, "value2", true);

        testField(radioField, "someUnknownOption", false, "Invalid form: The value of field with id id does not match any of the given options value1,value2");
        testField(selectField, "someUnknownOption", false, "Invalid form: The value of field with id id does not match any of the given options value1,value2");

    }

    private void testField(FormField formField, String value, boolean expectSuccess) {
        testField(formField, value, expectSuccess, null);
    }

    private void testField(FormField formField, String value, boolean expectSuccess, String expectedMessage) {
        FormValidator formValidator = new FormValidator(false, siteManagementDebugProperties);
        FilledInUserSiteFormValues filledInUserSiteFormValues = new FilledInUserSiteFormValues();
        filledInUserSiteFormValues.add(formField.getId(), value);
        Form form = new Form(Collections.singletonList(formField), null, null);

        try {
            formValidator.validateValues(form, filledInUserSiteFormValues);
            if (!expectSuccess) {
                fail("expected an exception.");
            }
        } catch (FormValidationException e) {
            assertThat(e.getMessage()).contains(expectedMessage);
            if (expectSuccess) {
                fail("expected no exception");
            }
        }
    }

    private void assertExceptionMessageEndsWithPossibleSets(String message, String setSeperator, String fieldSeperator, Set<Set<String>> expectedSets) {
        String[] sets = message.split(setSeperator);
        Set<Set<String>> setsInMessage =
                Arrays.stream(sets)
                        .map(it -> Set.of(it.split(fieldSeperator)))
                        .collect(Collectors.toSet());

        // AssertJ can't cope with Set<Set<..>>
        for (Set<String> set : setsInMessage) {
            if (!expectedSets.remove(set)) {
                fail("found " + set + " but did not expect it");
            }
        }
        if (expectedSets.size() != 0) {
            fail("did not find " + expectedSets);
        }
    }

    private FilledInUserSiteFormValues filledInValues(Map.Entry<String, String>... values) {
        FilledInUserSiteFormValues filledInUserSiteFormValues = new FilledInUserSiteFormValues();
        for (Map.Entry<String, String> value : values) {
            filledInUserSiteFormValues.add(value.getKey(), value.getValue());
        }
        return filledInUserSiteFormValues;
    }

    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

}
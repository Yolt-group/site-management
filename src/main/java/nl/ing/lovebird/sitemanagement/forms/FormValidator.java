package nl.ing.lovebird.sitemanagement.forms;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providershared.form.*;
import nl.ing.lovebird.sitemanagement.configuration.SiteManagementDebugProperties;
import nl.ing.lovebird.sitemanagement.forms.FormExpressionTreeNode.EvaluationResult;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.IBANValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.ing.lovebird.sitemanagement.forms.FormExpressionTreeNode.EvaluationResult.INVALID;
import static nl.ing.lovebird.sitemanagement.forms.FormExpressionTreeNode.EvaluationResult.NOT_CHOSEN;

/**
 * Performs validation on the form. If there are only simple fields inside the form, it's simply iterating through the fields. However,
 * due to a {@link ChoiceFormComponent} in combination with optional fields, the validation is actually a lot harder.
 *
 * 'Choice' containers is a {@link FormContainer} where the user is allowed to only *choose* one of the child components.
 * That means that you are not allowed to provide 2 choices.
 * Also, because of a choice might only contain optional fields, that makes it even more difficult.
 * That's what the following example illustrates (this is also in the UT)
 * Consider the following tree:
 *                               CHOICE
 *           FIELD-A*                               CHOICE
 *
 *                             CHOICE                MULTI                 SECTION
 *                         FIELD-B*  FIELD-C*       FIELD-D/FIELD-E      FIELD-F     CHOICE
 *                                                                               FIELD-G*  FIELD-H*
 * Here '*' indicates an optional field.
 * The user is now allowed to *only* enter field A*, or B*, or C*, or D+E, or F+G*, or F+H*.
 * On top of that, in this situation no value is valid as well, because you could have implicitly chosen (but not provided because
 * it's optional) 'A' or even H through C->C->S->C->H.
 *
 * This recursively goes up in the tree to eval whether the filled in values are valid. (whether choices are correct and
 * required fields are there).
 * We map it into some TreeNode to recursively eval it. Unfortunately this cannot be some binary operation tree because we have to
 * deal with non-determent optional fields.. Consider the following example:
 *                               CHOICE
 *           FIELD-A*                               CHOICE
 *                                             FIELD-B*  FIELD-C*
 * Valid input in this form is:
 * - 'A'
 * - 'B'
 * - 'C'
 * - ''  (because everything is optional so some choice is implicit).
 * A choice could be seen as XOR (you have to choose for 1 option, exclusively.
 * However, A ^ ( B ^ C) does not provide us the right answer due to optional fields. Because one of the requirements is that the
 * input '' evaluates to True. This doesn't. --> ( F ^ (F ^ F)) = F ^ F = F
 * In other words the input '' and 'A' should satisfy:
 * True = True ^ (choice/expression right)
 * True = False ^ (choice/expression right)
 * The expression right is independent from 'A', so you can't solve this problem because this would require expression right to
 * be a *variable* in terms of A.
 * That's why we introduce a constant 'MAYBE'. That way this can be solved. Because Field B and C are optional we can eval
 * either choice as 'MAYBE'. If it's not provided, it 'MIGHT BE' chosen, but we can't eval that yet.
 * That way we can define the following Truth table:
 * True ^ MAYBE = True (valid) --> maybe was apparently false
 * False ^ MAYBE = True (valid) --> maybe was apparently true
 * Note that this is exactly the above example and satisfies the equations for '' = True and 'A' = True.
 * In other words, whether a 'choice' was actually chosen, we need information about parent elements in the tree. (so that's why
 * it can't be a "simple" expression tree.
 */
@Slf4j
public class FormValidator {

    private static final String FIELD_VALIDATION_MESSAGE_FIELD_INDICATION = "The value of field with id ";
    private final boolean onlyCheckRequiredFields;
    private final SiteManagementDebugProperties siteManagementDebugProperties;

    public FormValidator(boolean onlyCheckRequiredFields, SiteManagementDebugProperties siteManagementDebugProperties) {
        this.onlyCheckRequiredFields = onlyCheckRequiredFields;
        this.siteManagementDebugProperties = siteManagementDebugProperties;
    }

    public void validateValues(Form form, FilledInUserSiteFormValues filledInUserSiteFormValues) throws FormValidationException {
        try {
            for (FormComponent formComponent : form.getFormComponents()) {
                validate(formComponent, filledInUserSiteFormValues);
            }
        } catch (FormValidationException e) {
            if (siteManagementDebugProperties.isFormValidationDetailedErrors()) {
                String keySet = Optional.of(filledInUserSiteFormValues)
                        .map(FilledInUserSiteFormValues::getValueMap)
                        .map(Map::keySet)
                        .map(Objects::toString)
                        .orElse("<empty>");
                String iKnowWhatImDoingJavaGrep = form.toString();
                log.warn("FormValidationException. Form: {}. Keys: {}", iKnowWhatImDoingJavaGrep, keySet); //NOSHERIFF be careful here. Only log keys!
            }
            throw e;
        }
    }

    private void validate(FormComponent formComponent, FilledInUserSiteFormValues filledInUserSiteFormValues) throws FormValidationException {

        if (formComponent.getComponentType().equals(FormComponent.ComponentType.FIELD)) {
            // easy, validate the field.
            validate((FormField) formComponent, filledInUserSiteFormValues);
        } else if (formComponent.getComponentType().equals(FormComponent.ComponentType.CONTAINER)) {
            // See javadoc class.
            FormContainer formContainer = (FormContainer) formComponent;
            FormExpressionTreeNode formExpressionTreeNode = mapToExpressionTree(formContainer);
            EvaluationResult result = formExpressionTreeNode.evaluate(filledInUserSiteFormValues);
            if (result.equals(INVALID) ||
                    // Nothing chosen while required.
                    (!Boolean.TRUE.equals(formContainer.isOptional()) && result.equals(NOT_CHOSEN))) {
                Set<Set<FormField>> permutations = determinePermutations(formContainer);
                String permutationAsString = permutations.stream()
                        .map(set -> set.stream().map(FormField::getId).collect(Collectors.joining("+")))
                        .collect(Collectors.joining(" | "));
                throw new FormValidationException("You must at least fill in one of the following set of fields " +
                        "(a set is seperated by '|' ) : " + permutationAsString);
            }
            // We now know that the provided values are allowed to be provided, and there's no values missing. Now simply validate those
            // provided fields.
            Set<FormField> formFieldsToValidate = flattenToFormFields(formComponent)
                    .filter(it -> filledInUserSiteFormValues.getValueMap().containsKey(it.getId()))
                    .collect(Collectors.toSet());

            for (FormField field : formFieldsToValidate) {
                validate(field, filledInUserSiteFormValues);
            }
        }
    }

    private static Stream<FormField> flattenToFormFields(FormComponent formComponent) {
        if (formComponent.getComponentType().equals(FormComponent.ComponentType.FIELD)) {
            return Stream.of((FormField) formComponent);
        }
        return ((FormContainer) formComponent).getChildComponents().stream().flatMap(FormValidator::flattenToFormFields);
    }

    private FormExpressionTreeNode mapToExpressionTree(FormComponent formComponent) {
        if (formComponent.getComponentType().equals(FormComponent.ComponentType.CONTAINER)) {
            FormContainer formContainer = (FormContainer) formComponent;
            List<FormExpressionTreeNode> childNodes = ((FormContainer) formComponent).getChildComponents().stream()
                    .map(this::mapToExpressionTree)
                    .collect(Collectors.toList());
            if (formContainer.getContainerType().equals(FormContainer.ContainerType.CHOICE)) {
                return new ChoiceXORTreeNode(Boolean.TRUE.equals(formComponent.isOptional()), childNodes, "CHOICE-" + ((ChoiceFormComponent) formComponent).getId());
            } else if (formContainer.getContainerType().equals(FormContainer.ContainerType.MULTI)) {
                return new MultiAndTreeNode(Boolean.TRUE.equals(formComponent.isOptional()), childNodes, "MULTI-" + ((MultiFormComponent) formComponent).getId());
            } else if (formContainer.getContainerType().equals(FormContainer.ContainerType.SECTION)) {
                return new MultiAndTreeNode(Boolean.TRUE.equals(formComponent.isOptional()), childNodes, "SECTION-" + ((SectionFormComponent) formComponent).getId());
            } else {
                throw new NotImplementedException("Not implemented for component type " + formContainer.getContainerType());
            }
        } else {
            FormField formField = (FormField) formComponent;
            return new FieldLeafTreeNode(Boolean.TRUE.equals(formComponent.isOptional()), Collections.emptyList(), formField.getId());
        }
    }


    private void validate(FormField formField, FilledInUserSiteFormValues filledInUserSiteFormValues) throws FormValidationException {
        validateRequiredField(formField, filledInUserSiteFormValues);
        if (onlyCheckRequiredFields) {
            return;
        }
        String value = filledInUserSiteFormValues.get(formField.getId());
        if (StringUtils.isEmpty(value)) {
            // Non-required fields are allowed to be empty.
            return;
        }
        switch (formField.getFieldType()) {
            case TEXT:
                validate((TextField) formField, value);
                break;
            case PASSWORD:
                validate((PasswordField) formField, value);
                break;
            case SELECT:
                validate((SelectField) formField, value);
                break;
            case EXPLANATION:
                // No validation, no user input.
                break;
            case DATE:
                validate((DateField) formField, value);
                break;
            case NUMBER:
                validate((NumberField) formField, value);
                break;
            case IBAN:
                validate((IbanField) formField, value);
                break;
            case RADIO:
                validate((RadioField) formField, value);
                break;
            case TEXT_AREA:
                validate((TextAreaField) formField, value);
                break;
            default:
                throw new NotImplementedException("Not implemented. Please add a case-statement to validate the form field of type " +
                        formField.getFieldType());
        }
    }

    /**
     * Recursive method.
     * See explanation in callee.
     */
    private Set<Set<FormField>> determinePermutations(FormContainer formContainer) {
        Set<Set<FormField>> permutations = new HashSet<>();
        if (formContainer.getContainerType().equals(FormContainer.ContainerType.CHOICE)) {
            Set<Set<FormField>> sets = determineChoicePermutations((ChoiceFormComponent) formContainer);
            permutations.addAll(sets);
        } else {
            Set<Set<FormField>> sets = determineMultiOrSectionPermutations(formContainer);
            permutations.addAll(sets);
        }
        return permutations;
    }

    private Set<Set<FormField>> determineChoicePermutations(ChoiceFormComponent choiceFormComponent) {
        Set<Set<FormField>> permutations = new HashSet<>();

        // Choice.. That means the permutations/choices are simple all the children added to the list.
        for (FormComponent childComponent : choiceFormComponent.getChildComponents()) {
            if (childComponent.getComponentType().equals(FormComponent.ComponentType.CONTAINER)) {
                permutations.addAll(determinePermutations((FormContainer) childComponent));
            } else {
                permutations.add(Collections.singleton((FormField) childComponent));
            }
        }
        return permutations;
    }

    private Set<Set<FormField>> determineMultiOrSectionPermutations(FormContainer formContainer) {
        // Multi or section. That means it's an AND. The permutations for a multi/section only containing fields, is simply 1 fieldset:
        // All those fields. If there's a list of permutations coming from a child-container, we have to add the direct fields to these
        // permutations.
        Set<Set<FormField>> permutations = new HashSet<>();
        long nrOfChildContainers = formContainer.getChildComponents().stream().filter(it -> !it.getComponentType().equals(FormComponent.ComponentType.FIELD)).count();
        if (nrOfChildContainers == 0) {
            Set<FormField> singleSet = new HashSet<>();
            for (FormComponent childComponent : formContainer.getChildComponents()) {
                singleSet.add((FormField) childComponent);
            }
            permutations.add(singleSet);
            return permutations;
        }

        // There is at leas 1 other container.
        Set<Set<FormField>> childContainerPermutations = new HashSet<>();
        Set<FormField> childFields = new HashSet<>();
        for (FormComponent childComponent : formContainer.getChildComponents()) {
            if (childComponent.getComponentType().equals(FormComponent.ComponentType.CONTAINER)) {
                childContainerPermutations.addAll(determinePermutations((FormContainer) childComponent));
            } else {
                childFields.add((FormField) childComponent);
            }
        }
        for (Set<FormField> fieldsSet : childContainerPermutations) {
            HashSet<FormField> combined = new HashSet<>(fieldsSet);
            combined.addAll(childFields);
            permutations.add(combined);
        }
        return permutations;
    }

    private static void validate(TextField textField, String value) throws FormValidationException {
        if (value != null) {
            validateMaxLength(value, textField.getMaxLength(), textField.getId());
            validateRegex(value, textField.getRegex(), textField.getId());
        }
    }

    private static void validate(PasswordField passwordField, String value) throws FormValidationException {
        if (value != null) {
            validateMaxLength(value, passwordField.getMaxLength(), passwordField.getId());
            validateRegex(value, passwordField.getRegex(), passwordField.getId());
        }
    }

    private static void validate(DateField dateField, String value) throws FormValidationException {
        if (value != null) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateField.getDateFormat());
            try {
                LocalDate ld = LocalDate.parse(value, dateTimeFormatter);
                String result = ld.format(dateTimeFormatter);
                if (!result.equals(value)) {
                    throw new FormValidationException("Invalid date format for field with id " + dateField.getId() + ". Expected format: " +
                            dateField.getDateFormat());
                }

                if (dateField.getMinDate() != null && ld.isBefore(dateField.getMinDate())) {
                    throw new FormValidationException("Date of field with id " + dateField.getId() + " should be above " +
                            dateField.getMinDate());
                }

                if (dateField.getMaxDate() != null && ld.isAfter(dateField.getMaxDate())) {
                    throw new FormValidationException("Date of field with id " + dateField.getId() + " should be below " +
                            dateField.getMaxDate());
                }
            } catch (DateTimeParseException e) {
                log.info("could not parse date {} to format {}", value, dateField.getDateFormat()); //NOSHERIFF
                throw new FormValidationException("Invalid date format for field with id " + dateField.getId() + ". Expected format: " +
                        dateField.getDateFormat());
            }
        }
    }

    private static void validate(NumberField numberField, String value) throws FormValidationException {
        if (value != null) {
            try {
                BigDecimal valueBigDecimal = new BigDecimal(value);
                if (numberField.getMax() != null && valueBigDecimal.compareTo(numberField.getMax()) >= 1) {
                    throw new FormValidationException(FIELD_VALIDATION_MESSAGE_FIELD_INDICATION + numberField.getId() + " should be lower than " +
                            numberField.getMax());
                }

                if (numberField.getMin() != null && valueBigDecimal.compareTo(numberField.getMin()) <= -1) {
                    throw new FormValidationException(FIELD_VALIDATION_MESSAGE_FIELD_INDICATION + numberField.getId() + " should be higher than " +
                            numberField.getMin());
                }
                if (numberField.getStepSize() != null && valueBigDecimal.scale() > numberField.getStepSize().scale()) {
                    throw new FormValidationException("The number of decimals of field with id " + numberField.getId() + " is too high. Maximum " +
                            "stepsize: " + numberField.getStepSize() + ". Allowed nr of decimals: " + numberField.getStepSize().scale());
                }

            } catch (NumberFormatException e) {
                throw new FormValidationException(FIELD_VALIDATION_MESSAGE_FIELD_INDICATION + numberField.getId() + " should be a number.");
            }
        }
    }

    private static void validate(IbanField ibanField, String value) throws FormValidationException {
        if (value != null && !IBANValidator.getInstance().isValid(value)) {
            throw new FormValidationException(FIELD_VALIDATION_MESSAGE_FIELD_INDICATION + ibanField.getId() + " is not a valid IBAN.");
        }
    }

    private static void validate(RadioField radioField, String value) throws FormValidationException {
        if (value != null) {
            List<String> optionValuesStream = radioField.getSelectOptionValues().stream().map(SelectOptionValue::getValue).collect(Collectors.toList());
            if (!optionValuesStream.contains(value)) {
                throw new FormValidationException(FIELD_VALIDATION_MESSAGE_FIELD_INDICATION + radioField.getId() + " does not match any of the given " +
                        "options " + String.join(",", optionValuesStream));
            }
        }
    }

    private static void validate(SelectField selectField, String value) throws FormValidationException {
        if (value != null) {
            List<String> optionValuesStream = selectField.getSelectOptionValues().stream().map(SelectOptionValue::getValue).collect(Collectors.toList());
            if (!optionValuesStream.contains(value)) {
                throw new FormValidationException(FIELD_VALIDATION_MESSAGE_FIELD_INDICATION + selectField.getId() + " does not match any of the given " +
                        "options " + String.join(",", optionValuesStream));
            }
        }
    }

    private static void validate(TextAreaField textAreaField, String value) throws FormValidationException {
        validateMaxLength(value, textAreaField.getMaxLength(), textAreaField.getId());
    }

    private static void validateRegex(String value, String regex, String id) throws FormValidationException {
        if (regex != null && !value.matches(regex)) {
            throw new FormValidationException(FIELD_VALIDATION_MESSAGE_FIELD_INDICATION + id + " does not match regex " + regex);
        }
    }

    private static void validateMaxLength(String value, Integer maxLength, String id) throws FormValidationException {
        if (maxLength != null && maxLength < value.length()) {
            throw new FormValidationException(FIELD_VALIDATION_MESSAGE_FIELD_INDICATION + id + " exceeds the maximum length of "
                    + maxLength);
        }
    }

    private static void validateRequiredField(FormField formField, FilledInUserSiteFormValues filledInUserSiteFormValues) throws FormValidationException {
        if (!formField.isOptional() && !filledInUserSiteFormValues.getValueMap().containsKey(formField.getId())) {
            throw new FormValidationException("Expected required form field with id " + formField.getId());
        }
    }
}

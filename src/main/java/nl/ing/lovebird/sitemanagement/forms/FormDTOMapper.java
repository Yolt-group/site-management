package nl.ing.lovebird.sitemanagement.forms;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providershared.form.*;
import nl.ing.lovebird.sitemanagement.exception.UnknownComponentException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FormDTOMapper {

    private FormDTOMapper() {
    }

    public static List<FormComponentDTO> convertToFormComponentDTOs(final List<FormComponent> formComponents) {
        final List<FormComponentDTO> formComponentDTOs = new ArrayList<>();
        for (FormComponent formComponent : formComponents) {
            final FormComponentDTO formComponentDTO = convertFormComponent(null, formComponent);
            formComponentDTOs.add(formComponentDTO);
        }
        return formComponentDTOs;
    }

    public static FormComponentDTO convertToFormComponentDTO(final FormComponent formComponent) {
        return convertFormComponent(null, formComponent);
    }

    private static FormComponentDTO convertFormComponent(final FormContainerDTO parentContainer,
                                                         final FormComponent formComponent) {
        if (FormComponent.ComponentType.FIELD.equals(formComponent.getComponentType())) {
            final FormFieldDTO formFieldDTO = convertField((FormField) formComponent);
            if (parentContainer != null) {
                parentContainer.addChildComponent(formFieldDTO);
            }
            return formFieldDTO;
        } else if (FormComponent.ComponentType.CONTAINER.equals(formComponent.getComponentType())) {
            final FormContainerDTO formContainerDTO = convertContainer((FormContainer) formComponent);

            if (parentContainer != null) {
                parentContainer.addChildComponent(formContainerDTO);
            }
            return formContainerDTO;
        } else {
            final String errorMessage = String.format("Unknown component found: %s", formComponent.getComponentType());
            throw new UnknownComponentException(errorMessage);
        }
    }

    private static FormContainerDTO convertContainer(final FormContainer formContainer) {
        switch (formContainer.getContainerType()) {
            case CHOICE:
                ChoiceFormComponent component = (ChoiceFormComponent) formContainer;
                ChoiceFormComponentDTO componentDTO = new ChoiceFormComponentDTO(component.getId(), component.getDisplayName(), component.isOptional());
                component.getChildComponents().forEach(child -> convertFormComponent(componentDTO, child));
                return componentDTO;
            case MULTI:
                MultiFormComponent multiFormComponent = (MultiFormComponent) formContainer;
                MultiFormComponentDTO multiFormComponentDTO = new MultiFormComponentDTO(multiFormComponent.getId(), multiFormComponent.getDisplayName(), multiFormComponent.isOptional());
                multiFormComponent.getChildComponents().forEach(child -> convertFormComponent(multiFormComponentDTO, child));
                return multiFormComponentDTO;
            case SECTION:
                SectionFormComponent sectionFormComponent = (SectionFormComponent) formContainer;
                SectionFormComponentDTO sectionComponentDTO = new SectionFormComponentDTO(sectionFormComponent.getId(), sectionFormComponent.getDisplayName(), sectionFormComponent.isOptional());
                sectionFormComponent.getChildComponents().forEach(child -> convertFormComponent(sectionComponentDTO, child));
                return sectionComponentDTO;
            default:
                final String errorMessage = String.format("Unknown container found: %s", formContainer.getContainerType());
                throw new UnknownComponentException(errorMessage);
        }
    }

    private static FormFieldDTO convertField(final FormField field) {
        switch (field.getFieldType()) {
            case PASSWORD:
                final PasswordField passwordField = (PasswordField) field;
                return new PasswordFieldDTO(
                        passwordField.getId(),
                        passwordField.getDisplayName(),
                        passwordField.getLength(),
                        passwordField.getMaxLength(),
                        passwordField.isOptional());
            case TEXT:
                final TextField textField = (TextField) field;
                return new TextFieldDTO(
                        textField.getId(),
                        textField.getDisplayName(),
                        textField.getLength(),
                        textField.getMaxLength(),
                        textField.isOptional());
            case SELECT:
                final SelectField selectField = (SelectField) field;
                final SelectFieldDTO selectFieldDTO = new SelectFieldDTO(
                        selectField.getId(),
                        selectField.getDisplayName(),
                        selectField.getLength(),
                        selectField.getMaxLength(),
                        selectField.getOptional(),
                        selectField.getDefaultValue());
                for (SelectOptionValue optionValue : selectField.getSelectOptionValues()) {
                    selectFieldDTO.addSelectOptionValue(
                            new SelectOptionValueDTO(optionValue.getValue(), optionValue.getDisplayName())
                    );
                }
                return selectFieldDTO;
            case EXPLANATION:
                final ExplanationField explanationField = (ExplanationField) field;
                return new ExplanationFieldDTO(
                        explanationField.getId(),
                        explanationField.getDisplayName(),
                        explanationField.getExplanation());
            case DATE:
                final DateField dateField = (DateField) field;
                return new DateFieldDTO(dateField.getId(),
                        dateField.getDisplayName(),
                        dateField.getOptional(),
                        dateField.getMinDate(),
                        dateField.getMaxDate(),
                        dateField.getDefaultValue(),
                        dateField.getDateFormat());
            case NUMBER:
                final NumberField numberField = (NumberField) field;
                return new NumberFieldDTO(numberField.getId(),
                        numberField.getDisplayName(),
                        numberField.isOptional(),
                        numberField.getMin(),
                        numberField.getMax(),
                        numberField.getStepSize(),
                        numberField.getDefaultValue());
            case IBAN:
                final IbanField ibanField = (IbanField) field;
                return new IbanFieldDTO(ibanField.getId(),
                        ibanField.getDisplayName(),
                        ibanField.getOptional(),
                        ibanField.getDefaultValue());
            case RADIO:
                final RadioField radioField = (RadioField) field;
                RadioFieldDTO radioFieldDTO = new RadioFieldDTO(radioField.getId(),
                        radioField.getDisplayName(),
                        radioField.isOptional(),
                        radioField.getDefaultValue());
                for (SelectOptionValue optionValue : radioField.getSelectOptionValues()) {
                    radioFieldDTO.addSelectOptionValue(
                            new SelectOptionValueDTO(optionValue.getValue(), optionValue.getDisplayName())
                    );
                }
                return radioFieldDTO;
            case TEXT_AREA:
                TextAreaField textAreaField = (TextAreaField) field;
                return new TextAreaFieldDTO(textAreaField.getId(),
                        textAreaField.getDisplayName(),
                        textAreaField.getMaxLength(),
                        textAreaField.getOptional(),
                        textAreaField.getRows());
            case IMAGE:
                ImageField imageField = (ImageField) field;
                return new ImageFieldDTO(imageField.getId(),
                        imageField.getDisplayName(),
                        imageField.getBase64Image(),
                        imageField.getMimeType());
            case FLICKER_CODE:
                FlickerCodeField flickerCodeField = (FlickerCodeField) field;
                return new FlickerCodeFieldDTO(flickerCodeField.getId(),
                        flickerCodeField.getDisplayName(),
                        flickerCodeField.getCode());
            default:
                final String errorMessage = String.format("Unknown field found: %s", field.getFieldType());
                throw new UnknownComponentException(errorMessage);
        }
    }

}

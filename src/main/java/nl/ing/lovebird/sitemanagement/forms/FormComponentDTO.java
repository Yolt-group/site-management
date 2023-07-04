package nl.ing.lovebird.sitemanagement.forms;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FormFieldDTO.class, name = "FIELD"),
        @JsonSubTypes.Type(value = FormContainerDTO.class, name = "CONTAINER"),
        @JsonSubTypes.Type(value = TextFieldDTO.class, name = "TEXT"),
        @JsonSubTypes.Type(value = PasswordFieldDTO.class, name = "PASSWORD"),
        @JsonSubTypes.Type(value = SelectFieldDTO.class, name = "SELECT"),
        @JsonSubTypes.Type(value = ExplanationFieldDTO.class, name = "EXPLANATION"),
        @JsonSubTypes.Type(value = DateFieldDTO.class, name = "DATE"),
        @JsonSubTypes.Type(value = NumberFieldDTO.class, name = "NUMBER"),
        @JsonSubTypes.Type(value = IbanFieldDTO.class, name = "IBAN"),
        @JsonSubTypes.Type(value = RadioFieldDTO.class, name = "RADIO"),
        @JsonSubTypes.Type(value = TextAreaFieldDTO.class, name = "TEXT_AREA"),
        @JsonSubTypes.Type(value = ImageFieldDTO.class, name = "IMAGE"),
        @JsonSubTypes.Type(value = FlickerCodeFieldDTO.class, name = "FLICKER_CODE"),
        @JsonSubTypes.Type(value = ChoiceFormComponentDTO.class, name = "CHOICE"),
        @JsonSubTypes.Type(value = MultiFormComponentDTO.class, name = "MULTI"),
        @JsonSubTypes.Type(value = SectionFormComponentDTO.class, name = "SECTION")}
)
@Schema(name = "FormComponentDTO",
        description = "The type of form component",
        discriminatorProperty = "type",
        oneOf = {
                TextFieldDTO.class,
                PasswordFieldDTO.class,
                SelectFieldDTO.class,
                ExplanationFieldDTO.class,
                DateFieldDTO.class,
                NumberFieldDTO.class,
                IbanFieldDTO.class,
                RadioFieldDTO.class,
                TextAreaFieldDTO.class,
                ImageFieldDTO.class,
                FlickerCodeFieldDTO.class,
                ChoiceFormComponentDTO.class,
                MultiFormComponentDTO.class,
                SectionFormComponentDTO.class
        },
        discriminatorMapping = {
                @DiscriminatorMapping(value = "TEXT", schema = TextFieldDTO.class),
                @DiscriminatorMapping(value = "PASSWORD", schema = PasswordFieldDTO.class),
                @DiscriminatorMapping(value = "SELECT", schema = SelectFieldDTO.class),
                @DiscriminatorMapping(value = "EXPLANATION", schema = ExplanationFieldDTO.class),
                @DiscriminatorMapping(value = "DATE", schema = DateFieldDTO.class),
                @DiscriminatorMapping(value = "NUMBER", schema = NumberFieldDTO.class),
                @DiscriminatorMapping(value = "IBAN", schema = IbanFieldDTO.class),
                @DiscriminatorMapping(value = "RADIO", schema = RadioFieldDTO.class),
                @DiscriminatorMapping(value = "TEXT_AREA", schema = TextAreaFieldDTO.class),
                @DiscriminatorMapping(value = "IMAGE", schema = ImageFieldDTO.class),
                @DiscriminatorMapping(value = "FLICKER_CODE", schema = FlickerCodeFieldDTO.class),
                @DiscriminatorMapping(value = "CHOICE", schema = ChoiceFormComponentDTO.class),
                @DiscriminatorMapping(value = "MULTI", schema = MultiFormComponentDTO.class),
                @DiscriminatorMapping(value = "SECTION", schema = SectionFormComponentDTO.class)
        })
public interface FormComponentDTO {

    String getDisplayName();

    boolean isOptional();

    ComponentType getComponentType();

    enum ComponentType {
        CONTAINER, FIELD
    }

    @Schema(required = true)
    String getType();
}

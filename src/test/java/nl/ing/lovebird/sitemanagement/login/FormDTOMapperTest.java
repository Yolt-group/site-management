package nl.ing.lovebird.sitemanagement.login;

import nl.ing.lovebird.providershared.form.*;
import nl.ing.lovebird.sitemanagement.forms.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class FormDTOMapperTest {


    @Test
    void convertEmptyForm() {
        List<FormComponent> formComponents = new ArrayList<>();
        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(0);
    }

    @Test
    void checkStructureConvertForm() {
        List<FormComponent> formComponents = new ArrayList<>();
        formComponents.add(new TextField("id", "name", 1, 1, true, "[a-zA-Z]", false));
        ChoiceFormComponent choiceFormComponent = new ChoiceFormComponent();
        choiceFormComponent.addComponent(new TextField("id2.1", "name", 1, 1, true, "[a-zA-Z]", false));
        choiceFormComponent.addComponent(new MultiFormComponent());
        formComponents.add(choiceFormComponent);
        formComponents.add(new PasswordField("id4", "pw", 1, 1, true, "[a-zA-Z]"));
        formComponents.add(new PasswordField("id4", "pw", 1, 1, true, "[a-zA-Z]"));
        formComponents.add(new TextField("id5", "name", 1, 1, true, "[a-zA-Z]", false));

        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(5);

        assertThat(formComponentDTOs.get(0).getClass()).isEqualTo(TextFieldDTO.class);
        assertThat(formComponentDTOs.get(1).getClass()).isEqualTo(ChoiceFormComponentDTO.class);
        assertThat(formComponentDTOs.get(2).getClass()).isEqualTo(PasswordFieldDTO.class);
        assertThat(formComponentDTOs.get(3).getClass()).isEqualTo(PasswordFieldDTO.class);
        assertThat(formComponentDTOs.get(4).getClass()).isEqualTo(TextFieldDTO.class);

        assertThat(((ChoiceFormComponentDTO) formComponentDTOs.get(1)).getChildComponents().get(0)).isInstanceOf(TextFieldDTO.class);
        assertThat(((ChoiceFormComponentDTO) formComponentDTOs.get(1)).getChildComponents().get(1)).isInstanceOf(MultiFormComponentDTO.class);
    }

    @Test
    void checkPasswordFieldConversion() {
        List<FormComponent> formComponents = new ArrayList<>();
        PasswordField password = new PasswordField("id", "display", 10, 20, true, null);
        formComponents.add(password);

        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(1);
        assertThat(formComponentDTOs.get(0).getClass()).isEqualTo(PasswordFieldDTO.class);
        assertThat(formComponentDTOs.get(0)).isEqualTo(new PasswordFieldDTO("id", "display", 10, 20, true));
    }

    @Test
    void checkTextFieldConversion() {
        List<FormComponent> formComponents = new ArrayList<>();
        TextField textfield = new TextField("id", "display", 10, 20, true, null, false);
        formComponents.add(textfield);
        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(1);
        assertThat(formComponentDTOs.get(0).getClass()).isEqualTo(TextFieldDTO.class);
        assertThat(formComponentDTOs.get(0)).isEqualTo(new TextFieldDTO("id", "display", 10, 20, true));
    }

    @Test
    void checkImageFieldConversion() {
        List<FormComponent> formComponents = new ArrayList<>();
        ImageField imageField = new ImageField("id", "imageDisplay", "RGVmaW5pdGVseSBub3QgYSByZWFsIGltYWdl", "image/png");
        formComponents.add(imageField);
        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(1);
        assertThat(formComponentDTOs.get(0).getClass()).isEqualTo(ImageFieldDTO.class);
        assertThat(formComponentDTOs.get(0)).isEqualTo(new ImageFieldDTO("id", "imageDisplay", "RGVmaW5pdGVseSBub3QgYSByZWFsIGltYWdl", "image/png"));
    }

    @Test
    void checkFlickerCodeFieldConversion() {
        List<FormComponent> formComponents = new ArrayList<>();
        FlickerCodeField flickerCodeField = new FlickerCodeField("id", "flickerCodeDisplay", "0468C0110930898853522DE84499999310000005140043,33");
        formComponents.add(flickerCodeField);
        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(1);
        assertThat(formComponentDTOs.get(0).getClass()).isEqualTo(FlickerCodeFieldDTO.class);
        assertThat(formComponentDTOs.get(0)).isEqualTo(new FlickerCodeFieldDTO("id", "flickerCodeDisplay", "0468C0110930898853522DE84499999310000005140043,33"));
    }

    @Test
    void checkSelectFieldConversion() {
        List<FormComponent> formComponents = new ArrayList<>();
        formComponents.add(createSelectField());

        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(1);
        assertThat(formComponentDTOs.get(0).getClass()).isEqualTo(SelectFieldDTO.class);

        assertThat(formComponentDTOs.get(0)).isEqualTo(createSelectFieldDTO());
    }

    @Test
    void checkMultiFormConversion() {
        List<FormComponent> formComponents = new ArrayList<>();
        MultiFormComponent multiFormComponent = new MultiFormComponent();
        multiFormComponent.setDisplayName("MyMultiForm");
        multiFormComponent.addComponent(createSelectField());
        formComponents.add(multiFormComponent);

        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(1);
        assertThat(formComponentDTOs.get(0).getClass()).isEqualTo(MultiFormComponentDTO.class);
        MultiFormComponentDTO multiDTO = (MultiFormComponentDTO) formComponentDTOs.get(0);

        assertThat(multiDTO.getChildComponents().get(0)).isEqualTo(createSelectFieldDTO());
    }

    @Test
    void checkChoiceFormConversion() {
        List<FormComponent> formComponents = new ArrayList<>();
        ChoiceFormComponent choiceFormComponent = new ChoiceFormComponent();
        choiceFormComponent.addComponent(createSelectField());
        formComponents.add(choiceFormComponent);

        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(1);
        assertThat(formComponentDTOs.get(0).getClass()).isEqualTo(ChoiceFormComponentDTO.class);
        ChoiceFormComponentDTO ChoiceDTO = (ChoiceFormComponentDTO) formComponentDTOs.get(0);

        assertThat(ChoiceDTO.getChildComponents().get(0)).isEqualTo(createSelectFieldDTO());
    }

    @Test
    void checkSectionFormConversion() {
        List<FormComponent> formComponents = new ArrayList<>();
        SectionFormComponent sectionFormComponent = new SectionFormComponent("id", "displayname", true);
        sectionFormComponent.addComponent(createSelectField());
        formComponents.add(sectionFormComponent);

        final List<FormComponentDTO> formComponentDTOs = FormDTOMapper.convertToFormComponentDTOs(formComponents);

        assertThat(formComponentDTOs.size()).isEqualTo(1);
        assertThat(formComponentDTOs.get(0)).isInstanceOf(SectionFormComponentDTO.class);
        SectionFormComponentDTO sectionDTO = (SectionFormComponentDTO) formComponentDTOs.get(0);

        assertThat(sectionDTO.getChildComponents().get(0)).isEqualTo(createSelectFieldDTO());
    }

    @Test
    void checkSingleComponentConversion() {
        ExplanationField explanationField = new ExplanationField("ID", "short", "long explanation");

        final FormComponentDTO componentDTO = FormDTOMapper.convertToFormComponentDTO(explanationField);

        ExplanationFieldDTO expectedDTO = new ExplanationFieldDTO("ID", "short", "long explanation");
        assertThat(componentDTO).isEqualTo(expectedDTO);
    }

    private SelectFieldDTO createSelectFieldDTO() {
        SelectFieldDTO selectFieldDTO = new SelectFieldDTO("id", "display", 10, 20, true, null);
        SelectOptionValueDTO selectOptionValueDTO1 = new SelectOptionValueDTO();
        selectOptionValueDTO1.setDisplayName("option 1");
        selectOptionValueDTO1.setValue("value 1");
        selectFieldDTO.getSelectOptionValues().add(selectOptionValueDTO1);

        SelectOptionValueDTO selectOptionValueDTO2 = new SelectOptionValueDTO();
        selectOptionValueDTO2.setDisplayName("option 2");
        selectOptionValueDTO2.setValue("value 2");
        selectFieldDTO.getSelectOptionValues().add(selectOptionValueDTO2);
        return selectFieldDTO;
    }

    private SelectField createSelectField() {
        SelectField selectField = new SelectField("id", "display", 10, 20, true, false);

        SelectOptionValue selectOptionValue1 = new SelectOptionValue("value 1", "option 1");
        selectField.getSelectOptionValues().add(selectOptionValue1);

        SelectOptionValue selectOptionValue2 = new SelectOptionValue("value 2", "option 2");
        selectField.getSelectOptionValues().add(selectOptionValue2);
        return selectField;
    }
}
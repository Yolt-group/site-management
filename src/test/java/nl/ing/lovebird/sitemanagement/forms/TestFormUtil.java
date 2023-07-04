package nl.ing.lovebird.sitemanagement.forms;

import nl.ing.lovebird.providershared.form.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to 'hide' the creation of some forms(components) so it doesn't distract from reading the test cases.
 * Also to prevent copy-paste code just to create some forms.
 */
public class TestFormUtil {

    public static ArrayList<FormComponent> createTextFieldAndChoiceComponent() {
        final ArrayList<FormComponent> expectedFormComponents = new ArrayList<>();
        expectedFormComponents.add(new TextField("LOGIN", "Surname", 20, 40, false, null, false));
        ChoiceFormComponent choiceFormComponent = new ChoiceFormComponent();
        choiceFormComponent.addComponent(new TextField("OP_LOGIN1", "Membership number", 20, 12, true, null, false));
        choiceFormComponent.addComponent(new TextField("OP_LOGIN2", "Card number", 20, 16, true, null, false));
        MultiFormComponent multiFormComponent = new MultiFormComponent();
        multiFormComponent.setDisplayName("Sort code and Account number");
        multiFormComponent.setOptional(true);
        multiFormComponent.addComponent(new TextField("OP_LOGIN3", null, 20, 6, true, null, false));
        multiFormComponent.addComponent(new TextField("OP_LOGIN4", null, 20, 8, true, null, false));
        choiceFormComponent.addComponent(multiFormComponent);
        expectedFormComponents.add(choiceFormComponent);
        expectedFormComponents.add(new PasswordField("OP_PASSWORD", "Passcode", 20, 5, true, null));
        expectedFormComponents.add(new PasswordField("OP_PASSWORD1", "Memorable word", 20, 40, true, null));
        return expectedFormComponents;
    }

    /**
     * returns a list of formcomponents.
     * Should be a deserialized representation of testLoginForm_expectedForm.json
     */
    public static List<FormComponent> createAllVariationsOfFormComponents() {
        final List<FormComponent> formComponents = new ArrayList<>();
        formComponents.add(new TextField("user1", "user", 66, 6, false, null, false));
        formComponents.add(new PasswordField("password1", "password", 77, 7, false, null));

        final ChoiceFormComponent choiceFormComponent = new ChoiceFormComponent();
        choiceFormComponent.addComponent(new TextField("choice-user1", "choice-user", 44, 4, false, null, false));
        choiceFormComponent.addComponent(new PasswordField("choice-password1", "choice-password", 55, 5, false, null));
        formComponents.add(choiceFormComponent);

        final MultiFormComponent multiFormComponent = new MultiFormComponent();
        multiFormComponent.setDisplayName("multi");
        multiFormComponent.addComponent(new TextField("multi-user1", "multi-user", 11, 1, false, null, false));
        multiFormComponent.addComponent(new PasswordField("multi-password1", "multi-password", 22, 2, false, null));
        final SelectField selectField = new SelectField("multi-select1", "multi-select", 33, 3, false, false);
        SelectOptionValue option1 = new SelectOptionValue("option1", "option 1");
        SelectOptionValue option2 = new SelectOptionValue("option2", "option 2");
        SelectOptionValue option3 = new SelectOptionValue("option3", "option 3");
        selectField.addSelectOptionValue(option1);
        selectField.addSelectOptionValue(option2);
        selectField.addSelectOptionValue(option3);
        multiFormComponent.addComponent(selectField);
        choiceFormComponent.addComponent(multiFormComponent);

        final SelectField selectField2 = new SelectField("select1", "select", 88, 8, false, false);
        selectField2.addSelectOptionValue(option1);
        selectField2.addSelectOptionValue(option2);
        formComponents.add(selectField2);

        final MultiFormComponent multiFormComponent2 = new MultiFormComponent();
        multiFormComponent2.setDisplayName("main-multi");
        multiFormComponent2.addComponent(new TextField("main-multi-user1", "main-multi-user", 99, 9, false, null, false));
        multiFormComponent2.addComponent(new TextField("main-multi-user2", "main-multi-user2", 1010, 10, false, null, false));
        formComponents.add(multiFormComponent2);


        final SectionFormComponent section = new SectionFormComponent("section1", "diplayName", false);
        RadioField radioField = new RadioField("radioId", "option 1 or 2", false, false);
        radioField.addSelectOptionValue(option1);
        radioField.addSelectOptionValue(option2);
        DateField dateField = new DateField("age", "age", true, "yyyy-MM-dd", false);
        dateField.setMaxDate(LocalDate.of(2018, 12, 18));
        dateField.setMinDate(LocalDate.of(1990, 1, 1));
        dateField.setDefaultValue(LocalDate.of(1990, 3, 23));

        TextAreaField textAreaField = new TextAreaField("free.text", "freeText", 80, true, 2, false);
        NumberField numberField = new NumberField("someNumber", "some Number", true, new BigDecimal("0"), new BigDecimal("100"), new BigDecimal("0.1"), false);
        IbanField ibanField = new IbanField("iban", "your IBAN", true, false);

        section.addComponent(radioField);
        section.addComponent(dateField);
        section.addComponent(textAreaField);
        section.addComponent(numberField);
        section.addComponent(ibanField);
        formComponents.add(section);
        return formComponents;
    }

}

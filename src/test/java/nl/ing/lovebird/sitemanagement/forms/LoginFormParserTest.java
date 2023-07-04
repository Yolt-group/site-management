package nl.ing.lovebird.sitemanagement.forms;

import nl.ing.lovebird.providershared.form.*;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginFormParserTest {
    private final FormComponent EXPLANATION_FIELD = new ExplanationField("EXPLANATION", "Shortname",
            "<!DOCTYPE html><html><p>Some information about adding your bank to Yolt.</p> <div class=\"bright\">Lookup username and code in your &quot;online banking&quot; account.</div> </html>");

    private final Map<String, String> HIDDEN_COMPONENTS = new HashMap<>() {{
        put("HIDDEN1", "defaultValue");
        put("HIDDEN2", "false");
    }};

    @Test
    void parseAltLoginForm() throws Exception {
        final InputStream cardsJsonStream = LoginFormParserTest.class.getResourceAsStream("/data/alt_login_form.json");
        final String altLoginFormJson = new BufferedReader(new InputStreamReader(cardsJsonStream))
                .lines().collect(Collectors.joining("\n"));
        Form result = LoginFormParser.parseLoginForm(altLoginFormJson);

        assertThat(result.getFormComponents()).isEqualTo(alternativeFormComponents());
        assertThat(result.getExplanationField()).isEqualTo((EXPLANATION_FIELD));
        assertThat(result.getHiddenComponents()).isEqualTo((HIDDEN_COMPONENTS));
    }

    @Test
    void parseAltLoginFormV1() throws Exception {
        final InputStream cardsJsonStream = LoginFormParserTest.class.getResourceAsStream("/data/alt_login_form_v1.json");
        final String altLoginFormJson = new BufferedReader(new InputStreamReader(cardsJsonStream))
                .lines().collect(Collectors.joining("\n"));
        Form result = LoginFormParser.parseLoginForm(altLoginFormJson);

        assertThat(result.getFormComponents()).isEqualTo((alternativeFormComponents()));
        assertThat(result.getExplanationField()).isEqualTo((EXPLANATION_FIELD));
        assertThat(result.getHiddenComponents()).isEqualTo((HIDDEN_COMPONENTS));
    }

    private ArrayList<FormComponent> alternativeFormComponents() {
        return TestFormUtil.createTextFieldAndChoiceComponent();
    }

    @Test
    void parseForm_withoutExplanationAndHiddenComponents() throws Exception {
        final String basicForm = "{" +
                "  \"formComponents\": [" +
                "    \"java.util.ArrayList\", []" +
                "  ]}";

        Form result = LoginFormParser.parseLoginForm(basicForm);

        assertThat(result.getExplanationField()).isNull();
        assertThat(result.getHiddenComponents()).isNull();
    }

    @Test
    void roundtripNormalForm() throws IOException {
        // Containers
        ChoiceFormComponent choiceFormComponent = new ChoiceFormComponent();
        choiceFormComponent.addComponent(new TextField("id1", "name1", 8, 8, true, "^$", false));
        MultiFormComponent multiFormComponent = new MultiFormComponent();
        multiFormComponent.addComponent(new TextField("id2", "name2", 8, 8, true, "^$", false));

        // Fields
        TextField textField = new TextField("id3", "name3", 8, 8, true, "^$", false);
        PasswordField passwordField = new PasswordField("id4", "name4", 8, 8, true, "^$");
        SelectField selectField = new SelectField("id5", "name5", 8, 8, true, false);
        selectField.addSelectOptionValue(new SelectOptionValue("selectValue1", "displayName1"));
        selectField.addSelectOptionValue(new SelectOptionValue("selectValue2", "displayName2"));

        // Optional additional values
        ExplanationField expl = new ExplanationField("explId", "explName", "explString");
        Map<String, String> hidden = new HashMap<>();
        hidden.put("csrf", "csrfValue");

        // Create a form with all known types of components & containers
        List<FormComponent> formComponents = new ArrayList<>(List.of(choiceFormComponent, multiFormComponent, textField, passwordField, selectField));
        Form form = new Form(formComponents, expl, hidden);
        assertThat(form).isEqualTo(form); // Ensure equals/hashcode works across all fields

        // Serialize, deserialize and assert the resulting for equals the input
        String serialized = LoginFormParser.writeForm(form);
        Form deserialized = LoginFormParser.parseLoginForm(serialized);
        assertThat(deserialized).isEqualTo(form);

        // Guard against poorly implemented equals/hashcode
        String reserialized = LoginFormParser.writeForm(deserialized);
        assertThat(reserialized).isEqualTo(serialized);
    }

}

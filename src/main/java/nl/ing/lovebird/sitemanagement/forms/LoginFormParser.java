package nl.ing.lovebird.sitemanagement.forms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;
import nl.ing.lovebird.providershared.form.Form;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginFormParser {
    static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TypeResolverBuilder<?> typer = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, BasicPolymorphicTypeValidator.builder().build());
        typer = typer.init(JsonTypeInfo.Id.CLASS, new BackwardsCompatibleIdResolver());
        typer = typer.inclusion(JsonTypeInfo.As.WRAPPER_ARRAY);
        objectMapper.setDefaultTyping(typer);
        objectMapper.disable(MapperFeature.USE_ANNOTATIONS);
        objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }


    public static String writeForm(Form form) throws IOException {
        return objectMapper.writeValueAsString(form);
    }

    public static Form parseLoginForm(String json) throws IOException {
        return parse(json, Form.class);
    }

    public static <T> T parse(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }

    private static class BackwardsCompatibleIdResolver extends TypeIdResolverBase {

        final Map<String, String> mapping = new HashMap<>();

        BackwardsCompatibleIdResolver() {
            this(null, null);
        }

        BackwardsCompatibleIdResolver(JavaType baseType, TypeFactory typeFactory) {
            super(baseType, typeFactory);

            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.ChoiceFormComponent", "nl.ing.lovebird.providershared.form.ChoiceFormComponent");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.ExplanationField", "nl.ing.lovebird.providershared.form.ExplanationField");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.Field", "nl.ing.lovebird.providershared.form.Field");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.FilledInUserSiteFormValues", "nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.Form", "nl.ing.lovebird.providershared.form.Form");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.FormComponent", "nl.ing.lovebird.providershared.form.FormComponent");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.FormContainer", "nl.ing.lovebird.providershared.form.FormContainer");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.FormField", "nl.ing.lovebird.providershared.form.FormField");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.MultiFormComponent", "nl.ing.lovebird.providershared.form.MultiFormComponent");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.PasswordField", "nl.ing.lovebird.providershared.form.PasswordField");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.ProviderServiceMAFResponseDTO", "nl.ing.lovebird.providershared.form.ProviderServiceMAFResponseDTO");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.SelectField", "nl.ing.lovebird.providershared.form.SelectField");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.SelectOptionValue", "nl.ing.lovebird.providershared.form.SelectOptionValue");
            mapping.put("nl.ing.lovebird.sitemanagement.service.domain.TextField", "nl.ing.lovebird.providershared.form.TextField");
        }

        @Override
        public String idFromValue(Object value) {
            return value.getClass().getCanonicalName();
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            return idFromValue(value);
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return null;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
            return context.getTypeFactory().constructFromCanonical(mapping.getOrDefault(id, id));
        }
    }
}

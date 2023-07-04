package nl.ing.lovebird.sitemanagement.configuration;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providershared.form.SelectOptionValue;
import nl.ing.lovebird.sitemanagement.forms.*;
import nl.ing.lovebird.sitemanagement.health.dspipeline.UserContext;
import nl.ing.lovebird.sitemanagement.providercallback.CallbackConfiguration;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.usersite.FormLoginDTO;
import nl.ing.lovebird.sitemanagement.usersite.UrlLoginDTO;
import nl.ing.lovebird.sitemanagement.usersite.encryption.JWEFormStepEncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.usersite.encryption.NoFormStepEncryptionDetailsDTO;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;

@Slf4j
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties({CallbackConfiguration.class})
public class ApplicationConfiguration {

    @Value("${info.appName}")
    private String applicationName;

    @Value("${info.appVersion}")
    private String applicationVersion;

    public static final String ASYNC_EXECUTOR = "asyncExecutor";

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean(ASYNC_EXECUTOR)
    public ThreadPoolTaskExecutor asyncExecutor(TaskExecutorBuilder builder) {
        return builder
                .corePoolSize(10)
                .maxPoolSize(10)
                .queueCapacity(100)
                .threadNamePrefix(ASYNC_EXECUTOR + "-")
                .build();
    }

    public static final String BATCH_JOB_EXECUTOR = "batchExecutor";

    @Bean(BATCH_JOB_EXECUTOR)
    public ThreadPoolTaskExecutor batchExecutor(TaskExecutorBuilder builder) {
        return builder
                .corePoolSize(3)
                .maxPoolSize(3)
                .queueCapacity(0)
                .threadNamePrefix(BATCH_JOB_EXECUTOR + "-")
                .build();
    }

    public static final String INTERNAL_FLYWHEEL_PER_USER_EXECUTOR = "internalFlywheelPerUserExecutor";

    @Bean(INTERNAL_FLYWHEEL_PER_USER_EXECUTOR)
    public ThreadPoolTaskExecutor internalFlywheelPerUserExecutor(TaskExecutorBuilder builder) {
        /*
         * Note: we set core- and max pool to the same value. The default behaviour of {@link ThreadPoolTaskExecutor} is
         * to first fill up the entire queue and only when the queue is full to scale up the corePool thread count to
         * at most the maxPool thread count.  We want the following behaviour: first start up a thread and only if all
         * threads are busy put a task in the queue.  To achieve this we set corePoolSize and maxPoolSize to be equal
         * to the method argument poolSize and we set setAllowCoreThreadTimeOut to true so the system scales
         * down threads that aren't in use at any one time.
         */
        return builder
                .corePoolSize(3)
                .maxPoolSize(3)
                .queueCapacity(10_000)
                .threadNamePrefix(INTERNAL_FLYWHEEL_PER_USER_EXECUTOR + "-")
                .build();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer dateSerializerCustomizer() {
        return builder -> builder.serializerByType(Date.class, Dates.serializer());
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OpenApiCustomiser schemaCustomiser() {
        var types = List.of(
                UrlLoginDTO.class,
                FormLoginDTO.class,
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
                SectionFormComponentDTO.class,
                FormComponentDTO.class,
                SelectOptionValue.class,
                SelectOptionValueDTO.class,
                EncryptionDetailsDTO.class,
                JWEFormStepEncryptionDetailsDTO.class,
                NoFormStepEncryptionDetailsDTO.class
        );

        return openApi -> types.forEach(type -> {
            var schema = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(type)).schema;
            openApi.schema(schema.getName(), schema);
        });
    }

    @Bean
    public UserContextHeaderFromStringConverter userContextHeaderFromStringConverter() {
        return new UserContextHeaderFromStringConverter();
    }

    static class UserContextHeaderFromStringConverter implements Converter<String, UserContext> {
        @Override
        public UserContext convert(String source) {
            return UserContext.fromJson(source);
        }
    }

    @Bean
    public UserContextHeaderFromBytesConverter userContextHeaderFromBytesConverter() {
        return new UserContextHeaderFromBytesConverter();
    }

    static class UserContextHeaderFromBytesConverter implements Converter<byte[], UserContext> {
        @Override
        public UserContext convert(byte[] source) {
            return UserContext.fromJson(new String(source, StandardCharsets.UTF_8));
        }
    }
}

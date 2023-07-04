package nl.ing.lovebird.sitemanagement.lib.documentation;

import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class HandlerMappingDocumentationTest {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * Ensure that every request handler has an annotation that indicates if the endpoint is suitable for external
     * consumption (via client-proxy) or if it is only used internally by other microservices.
     */
    @Test
    void validateThatAllEndpointsHaveDocumentationAnnotations() {
        for (HandlerMethod handlerMethod : requestMappingHandlerMapping.getHandlerMethods().values()) {
            String packageName = handlerMethod.getMethod().getDeclaringClass().getPackageName();
            if (shouldSkipPackage(packageName)) {
                continue;
            }
            boolean isInternal = handlerMethod.hasMethodAnnotation(Internal.class);
            boolean isExternal = handlerMethod.hasMethodAnnotation(External.class);
            assertThat(isInternal || isExternal)
                    .withFailMessage("Request handler %s#%s is missing a documentation annotation.", handlerMethod.getMethod().getDeclaringClass().getName(), handlerMethod.getMethod().getName())
                    .isTrue();
        }
    }

    boolean shouldSkipPackage(String packageName) {
        return packageName.startsWith("org.springdoc") // skip swagger endpoints
                || packageName.startsWith("nl.ing.lovebird.rest.deleteuser") // skip lovebird-commons user delete endpoints
                || packageName.startsWith("org.springframework"); // skip default error controller
    }

}

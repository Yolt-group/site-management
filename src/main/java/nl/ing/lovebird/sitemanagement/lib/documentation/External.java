package nl.ing.lovebird.sitemanagement.lib.documentation;

import nl.ing.lovebird.springdoc.annotations.ExternalApi;

import java.lang.annotation.*;

/**
 * Annotation for documentation purposes only.
 * Endpoints annotated with this annotation are reachable through the client-gateway.
 *
 * To include an endpoint in the swagger documentation, annotated it with {@link ExternalApi}
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface External {
}

package nl.ing.lovebird.sitemanagement.lib.documentation;

import java.lang.annotation.*;

/**
 * Annotation for documentation purposes only.
 * Endpoints annotated with this annotation should **not** be reachable through the client-gateway.
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Internal {
    /**
     * Services that call this endpoint in the cluster.
     */
    Service[] value();

    enum Service {
        callbacks,
        batchTrigger,
        /**
         * yolt-assistance-portal, assistance-portal-yts, dev-portal
         */
        managementPortals,
        payments,
        providers,
        maintenance,
        clients,
        pis,
    }
}

package nl.ing.lovebird.sitemanagement.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.legacy.sitehealth.SiteMetric;

import java.time.LocalDateTime;

/**
 * This object contains bogus data and we should not explicitly tell our clients that this field is present.
 * As of YCO-1236 this is deprecated.
 */
@Data
@Deprecated
@Schema(description = "An object containing information about the health of the site")
public class SiteConnectionHealthStatus {

    public static final SiteConnectionHealthStatus SITE_CONNECTION_HEALH_STATUS_NOT_AVAILABLE =
            new SiteConnectionHealthStatus(MaintenanceStatus.NONE, null, null, SiteMetric.METRIC_NOT_AVAILABLE, SiteMetric.METRIC_NOT_AVAILABLE);

    /**
     * Status derived from the from and start times.
     */
    @Schema(required = true, description = "The current manual maintenance status of the site.")
    private final MaintenanceStatus manualMaintenanceStatus;
    /**
     * Optional maintenance start in UTC.
     */
    @Schema(required = false, description = "The start of manual maintenance for this site, in UTC.")
    private final LocalDateTime manualMaintenanceFrom;
    /**
     * Optional maintenance end in UTC.
     */
    @Schema(required = false, description = "The end of manual maintenance for this site, in UTC.")
    private final LocalDateTime manualMaintenanceTo;

    /**
     * The success ratio (1.00 - 0.00) of an add bank / update operation.
     * Note that this is intentionally a string, since "not available" might also be returned.
     * This is returned, for example, when the load was insufficient to calculate a ratio.
     */
    @Schema(required = true, description = "The success ratio (1.00 - 0.00) of an add bank / update operation. It might also be \"" + SiteMetric.METRIC_NOT_AVAILABLE + "\" due to a lack of load on this site.")
    private final String successRatioForCreateOrUpdate;
    /**
     * The success ratio (1.00 - 0.00) of a refresh operation.
     * Note that this is intentionally a string, since "not available" might also be returned.
     * This is returned, for example, when the load was insufficient to calculate a ratio.
     */
    @Schema(required = true, description = "The success ratio (1.00 - 0.00) of a refresh operation. It might also be \"" + SiteMetric.METRIC_NOT_AVAILABLE + "\" due to a lack of load on this site.")
    private final String successRatioRefresh;

    @Schema(required = true, description = "The total calculated health of this site.")
    public HealthStatus getHealthStatus() {
        return manualMaintenanceStatus.equals(MaintenanceStatus.MAINTENANCE) ? HealthStatus.DOWN : HealthStatus.UP;
    }

    public enum HealthStatus {
        UP,
        DOWN
    }

    public enum MaintenanceStatus {
        NONE,
        SCHEDULED,
        MAINTENANCE
    }
}

package nl.ing.lovebird.sitemanagement.legacy.sitehealth;

import lombok.Value;

@Value
@Deprecated
public class SiteMetric {

    /**
     * If there is not enough data, we communicate 'not available'. That means a '0' is actually a '0' ratio.
     */
    public static final String METRIC_NOT_AVAILABLE = "not available";
    public static final SiteMetric METRIC_FIXED_TO_1 = new SiteMetric("1.0", "1.0");

    /**
     * The success ratio (1.00 - 0.00) of an addbank / update operation.
     * Note that this is intentionally a string, since "not available" might also be returned.
     * This is returned, for example, when the load was insufficient to calculate a ratio.
     */
    String successRatioForCreateOrUpdate;
    /**
     * The success ratio (1.00 - 0.00) of a refresh operation.
     * Note that this is intentionally a string, since "not available" might also be returned.
     * This is returned, for example, when the load was insufficient to calculate a ratio.
     */
    String successRatioRefresh;
}

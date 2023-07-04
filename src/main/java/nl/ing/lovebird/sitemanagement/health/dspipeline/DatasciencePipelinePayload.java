package nl.ing.lovebird.sitemanagement.health.dspipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasciencePipelinePayload {
    private final UUID activityId;
    private final RefreshPeriod refreshPeriod;
    private final UserContext userContext;
    private final List<AccountContext> accountsContext;

    /**
     * Used to propagate trace information through data science while they do not yet propagate kafka message headers
     *
     * @deprecated
     */
    @Deprecated
    private Map<String, String> traceContext;

    public static final String MESSAGE_TYPE = "REFRESH_TRIGGERED";


    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class AccountContext {
        @NonNull
        public final UUID id;
        @NonNull
        public final AccountDTOv1.AccountType type;

        public final boolean hidden = false;
    }
}

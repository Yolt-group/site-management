package nl.ing.lovebird.sitemanagement.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository.UniqueRefreshesPerClientInfo;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticLoggingService {

    private final PostgresUserSiteRepository postgresUserSiteRepository;

    @Async(ApplicationConfiguration.BATCH_JOB_EXECUTOR)
    public void logNumberOfUniqueUserRefreshes(int daysInPast) {
        var numberOfRefreshesPerClient = postgresUserSiteRepository.getNumberOfRefreshesPerClient(daysInPast);
        var formattedRefreshes = numberOfRefreshesPerClient.stream()
                .map(UniqueRefreshesPerClientInfo::toString)
                .collect(Collectors.joining("\n"));
        log.info("**\nGot the following number of unique user refreshes per client for the past {} days.\n{}.\n **", daysInPast, formattedRefreshes); //NOSHERIFF
    }

    @Async(ApplicationConfiguration.BATCH_JOB_EXECUTOR)
    public void logUserSiteStatusesAndReasonsInUse() {
        log.info("logUserSiteConnectionStatusesAndFailureReasonsInUse " + postgresUserSiteRepository.userSiteConnectionStatusesAndFailureReasonsCounts()); //NOSHERIFF
        log.info("logUserSiteMigrationStatusesInUse " + postgresUserSiteRepository.userSiteMigrationStatussesCounts()); //NOSHERIFF
    }

}

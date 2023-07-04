package nl.ing.lovebird.sitemanagement.health.service.domain;

import nl.ing.lovebird.sitemanagement.health.repository.domain.UserSiteWithAccounts;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserHealthTest {

    @Test
    public void notMigrating_whenNoUserSites() {
        final List<UserSiteWithAccounts> userSites = Collections.emptyList();
        final UserHealth userHealth = new UserHealth(LovebirdHealthCode.UNKNOWN, userSites, Collections.emptyList());

        assertEquals(MigrationStatus.NONE, userHealth.getMigrationStatus());
    }

    @Test
    public void notMigrating_whenOneUserSiteNotMigrating() {
        final UserSiteWithAccounts userSite = mock(UserSiteWithAccounts.class);
        when(userSite.getMigrationStatus()).thenReturn(MigrationStatus.NONE);
        final List<UserSiteWithAccounts> userSites = asList(userSite);
        final UserHealth userHealth = new UserHealth(LovebirdHealthCode.UNKNOWN, userSites, Collections.emptyList());

        assertEquals(MigrationStatus.NONE, userHealth.getMigrationStatus());
    }

    @Test
    public void statusOfOneUserSite_whenOneUserSite() {
        final UserSiteWithAccounts userSite = mock(UserSiteWithAccounts.class);
        when(userSite.getMigrationStatus()).thenReturn(MigrationStatus.MIGRATION_NEEDED);
        final List<UserSiteWithAccounts> userSites = asList(userSite);
        final UserHealth userHealth = new UserHealth(LovebirdHealthCode.UNKNOWN, userSites, Collections.emptyList());

        assertEquals(MigrationStatus.MIGRATION_NEEDED, userHealth.getMigrationStatus());
    }

    @Test
    public void migratingTOComesBefore_MIGRATION_NEEDED() {
        final UserSiteWithAccounts userSite = mock(UserSiteWithAccounts.class);
        when(userSite.getMigrationStatus()).thenReturn(MigrationStatus.MIGRATION_NEEDED);

        final UserSiteWithAccounts userSite2 = mock(UserSiteWithAccounts.class);
        when(userSite2.getMigrationStatus()).thenReturn(MigrationStatus.MIGRATING_TO);
        final List<UserSiteWithAccounts> userSites = asList(userSite, userSite2);
        final UserHealth userHealth = new UserHealth(LovebirdHealthCode.UNKNOWN, userSites, Collections.emptyList());

        assertEquals(MigrationStatus.MIGRATING_TO, userHealth.getMigrationStatus());
    }

    @Test
    public void migratingTOComesBefore_MIGRATION_FROM() {
        final UserSiteWithAccounts userSite = mock(UserSiteWithAccounts.class);
        when(userSite.getMigrationStatus()).thenReturn(MigrationStatus.MIGRATING_FROM);

        final UserSiteWithAccounts userSite2 = mock(UserSiteWithAccounts.class);
        when(userSite2.getMigrationStatus()).thenReturn(MigrationStatus.MIGRATING_TO);
        final List<UserSiteWithAccounts> userSites = asList(userSite, userSite2);
        final UserHealth userHealth = new UserHealth(LovebirdHealthCode.UNKNOWN, userSites, Collections.emptyList());

        assertEquals(MigrationStatus.MIGRATING_TO, userHealth.getMigrationStatus());
    }

    @Test
    public void MIGRATION_NEEDEDComesBefore_NONE() {
        final UserSiteWithAccounts userSite = mock(UserSiteWithAccounts.class);
        when(userSite.getMigrationStatus()).thenReturn(MigrationStatus.MIGRATION_NEEDED);

        final UserSiteWithAccounts userSite2 = mock(UserSiteWithAccounts.class);
        when(userSite2.getMigrationStatus()).thenReturn(MigrationStatus.NONE);
        final List<UserSiteWithAccounts> userSites = asList(userSite, userSite2);
        final UserHealth userHealth = new UserHealth(LovebirdHealthCode.UNKNOWN, userSites, Collections.emptyList());

        assertEquals(MigrationStatus.MIGRATION_NEEDED, userHealth.getMigrationStatus());
    }
}

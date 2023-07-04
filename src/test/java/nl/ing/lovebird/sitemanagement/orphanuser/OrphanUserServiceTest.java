package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providercallback.UserExternalId;
import nl.ing.lovebird.sitemanagement.providercallback.UserExternalIdRepository;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class OrphanUserServiceTest {

    private static final ClientId CLIENT_ID = ClientId.random();
    private static final String PROVIDER = "YODLEE";
    private static final UUID BATCH_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EXTERNAL_ID = "123";

    @Mock
    private Clock clock;
    @Mock
    private OrphanUserBatchRepository orphanUserBatchRepository;
    @Mock
    private OrphanUserRepository orphanUserRepository;
    @Mock
    private OrphanUserExternalIdRepository orphanUserExternalIdRepository;
    @Mock
    private FormProviderRestClient formProviderRestClient;
    @Mock
    private UserExternalIdRepository userExternalIdRepository;
    @Mock
    private UserSiteService userSiteService;
    @Mock
    private OrphanUserAllowedExecutions allowedExecutions;
    @InjectMocks
    private OrphanUserService orphanUserService;

    private static ClientToken createClientToken(ClientId clientId) {
        ClientToken mock = mock(ClientToken.class);
        when(mock.getClientIdClaim()).thenReturn(clientId.unwrap());
        return mock;
    }

    @Test
    @SneakyThrows
    public void startPreparingBatch_whenBatchAlreadyExists_shouldThrowException() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        assertThatThrownBy(() -> {
            when(formProviderRestClient.fetchProviderExternalUserIds(PROVIDER, clientToken)).thenReturn(BATCH_ID);
            when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(new OrphanUserBatch()));

            orphanUserService.startPreparingBatch(clientToken, PROVIDER);
        }).isInstanceOf(OrphanUserBatchAlreadyExistsException.class);
    }

    @Test
    @SneakyThrows
    public void startPreparingBatch_whenBatchNotExists_shouldCreateBatch() {
        ClientToken clientToken = createClientToken(CLIENT_ID);

        when(formProviderRestClient.fetchProviderExternalUserIds(PROVIDER, clientToken)).thenReturn(BATCH_ID);
        when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.empty());
        ArgumentCaptor<OrphanUserBatch> argumentCaptor = ArgumentCaptor.forClass(OrphanUserBatch.class);

        orphanUserService.startPreparingBatch(mock(ClientToken.class), PROVIDER);

        verify(orphanUserBatchRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualToComparingOnlyGivenFields(
                new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_INITIATED),
                "provider", "status");
    }

    @Test
    void findAndSaveOrphanUsers_whenNoExternalIds_shouldDoNothing() {
        when(orphanUserExternalIdRepository.getForBatchAndProvider(eq(CLIENT_ID), eq(PROVIDER), eq(BATCH_ID), anyInt())).thenReturn(Collections.emptyList());
        orphanUserService.findAndSaveOrphanUsers(CLIENT_ID, PROVIDER, BATCH_ID);
        verifyNoMoreInteractions(orphanUserRepository, userExternalIdRepository, userSiteService);
    }

    @Test
    void findAndSaveOrphanUsers_whenUserNotOrphaned_shouldNotProcessThisUser() {
        OrphanUserExternalId orphanUserExternalId = new OrphanUserExternalId(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID);
        when(orphanUserExternalIdRepository.getForBatchAndProvider(eq(CLIENT_ID), eq(PROVIDER), eq(BATCH_ID), anyInt())).thenReturn(Collections.singletonList(orphanUserExternalId));
        UserExternalId userExternalId = new UserExternalId(USER_ID, PROVIDER, EXTERNAL_ID);
        when(userExternalIdRepository.findByProviderAndExternalUserId(PROVIDER, EXTERNAL_ID)).thenReturn(Optional.of(userExternalId));
        PostgresUserSite userSite = mock(PostgresUserSite.class);
        when(userSite.getProvider()).thenReturn(PROVIDER);
        when(userSiteService.getNonDeletedUserSites(USER_ID)).thenReturn(Collections.singletonList(userSite));

        orphanUserService.findAndSaveOrphanUsers(CLIENT_ID, PROVIDER, BATCH_ID);

        verify(orphanUserRepository, never()).save(any(OrphanUser.class));
    }

    @Test
    void findAndSaveOrphanUsers_whenUserIsOrphaned_shouldSaveThisUser() {
        OrphanUserExternalId orphanUserExternalId = new OrphanUserExternalId(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID);
        when(orphanUserExternalIdRepository.getForBatchAndProvider(eq(CLIENT_ID), eq(PROVIDER), eq(BATCH_ID), anyInt())).thenReturn(Collections.singletonList(orphanUserExternalId));
        UserExternalId userExternalId = new UserExternalId(USER_ID, PROVIDER, EXTERNAL_ID);
        when(userExternalIdRepository.findByProviderAndExternalUserId(PROVIDER, EXTERNAL_ID)).thenReturn(Optional.of(userExternalId));
        when(userSiteService.getNonDeletedUserSites(USER_ID)).thenReturn(Collections.emptyList());

        orphanUserService.findAndSaveOrphanUsers(CLIENT_ID, PROVIDER, BATCH_ID);

        OrphanUser expected = new OrphanUser(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL);
        ArgumentCaptor<OrphanUser> captor = ArgumentCaptor.forClass(OrphanUser.class);
        verify(orphanUserRepository).save(captor.capture());
        assertThat(captor.getValue()).isEqualToComparingOnlyGivenFields(expected,
                "provider", "orphanUserBatchId", "externalUserId", "status");
    }

    @Test
    void executeOrphanUserBatch_whenNoBatch_shouldThrowException() {
        assertThatThrownBy(() -> {
            when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.empty());

            orphanUserService.executeOrphanUserBatch(mock(ClientToken.class), PROVIDER, BATCH_ID);
        }).isInstanceOf(OrphanUserBatchNotFoundException.class);
    }

    @Test
    void executeOrphanUserBatch_whenIncorrectStatus_shouldThrowException() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        assertThatThrownBy(() -> {
            OrphanUserBatch orphanUserBatch = new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_INITIATED);
            when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(orphanUserBatch));

            orphanUserService.executeOrphanUserBatch(clientToken, PROVIDER, BATCH_ID);
        }).isInstanceOf(OrphanUserBatchInvalidStateException.class);
    }

    @Test
    void executeOrphanUserBatch_whenNoOrphanedUsers_shouldSetStatusEmpty() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        OrphanUserBatch orphanUserBatch = new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_PROCESSING_FINISHED);
        when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(orphanUserBatch));
        when(orphanUserRepository.listOrphanUsers(eq(CLIENT_ID), eq(PROVIDER), eq(BATCH_ID), anyInt())).thenReturn(Collections.emptyList());

        orphanUserService.executeOrphanUserBatch(clientToken, PROVIDER, BATCH_ID);

        verify(orphanUserBatchRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, OrphanUserBatch.Status.EXECUTE_EMPTY);
        verifyNoMoreInteractions(formProviderRestClient);
    }

    @Test
    void executeOrphanUserBatch_whenAllDeletesSuccess_shouldSetStatusSuccess() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        OrphanUserBatch orphanUserBatch = new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_PROCESSING_FINISHED);
        when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(orphanUserBatch));
        when(orphanUserRepository.listOrphanUsers(eq(CLIENT_ID), eq(PROVIDER), eq(BATCH_ID), anyInt())).thenReturn(Collections.singletonList(
                new OrphanUser(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL)
        ));

        orphanUserService.executeOrphanUserBatch(clientToken, PROVIDER, BATCH_ID);

        verify(orphanUserRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID, OrphanUser.Status.DELETED);
        verify(orphanUserBatchRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, OrphanUserBatch.Status.EXECUTE_FINISHED_SUCCESS);
    }

    @Test
    @SneakyThrows
    public void executeOrphanUserBatch_whenAllDeletesFailed_shouldSetStatusFailed() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        OrphanUserBatch orphanUserBatch = new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_PROCESSING_FINISHED);
        when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(orphanUserBatch));
        when(orphanUserRepository.listOrphanUsers(eq(CLIENT_ID), eq(PROVIDER), eq(BATCH_ID), anyInt())).thenReturn(Collections.singletonList(
                new OrphanUser(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL)
        ));
        doThrow(new RuntimeException()).when(formProviderRestClient).deleteOrphanUserAtProvider(eq(PROVIDER), eq(EXTERNAL_ID), any(ClientToken.class));

        orphanUserService.executeOrphanUserBatch(clientToken, PROVIDER, BATCH_ID);

        verify(orphanUserRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID, OrphanUser.Status.ERROR);
        verify(orphanUserBatchRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, OrphanUserBatch.Status.EXECUTE_FINISHED_WITH_ERRORS);
    }

    @Test
    @SneakyThrows
    public void executeOrphanUserBatch_whenSomeDeletesFailed_shouldSetStatusFailed() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        OrphanUserBatch orphanUserBatch = new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_PROCESSING_FINISHED);
        when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(orphanUserBatch));
        when(orphanUserRepository.listOrphanUsers(eq(CLIENT_ID), eq(PROVIDER), eq(BATCH_ID), anyInt())).thenReturn(List.of(
                new OrphanUser(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL),
                new OrphanUser(CLIENT_ID, "SALTEDGE", BATCH_ID, EXTERNAL_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL)
        ));
        doThrow(new RuntimeException()).when(formProviderRestClient).deleteOrphanUserAtProvider(eq("SALTEDGE"), eq(EXTERNAL_ID), any(ClientToken.class));

        orphanUserService.executeOrphanUserBatch(clientToken, PROVIDER, BATCH_ID);

        verify(orphanUserRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID, OrphanUser.Status.DELETED);
        verify(orphanUserRepository).updateStatus(CLIENT_ID, "SALTEDGE", BATCH_ID, EXTERNAL_ID, OrphanUser.Status.ERROR);
        verify(orphanUserBatchRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, OrphanUserBatch.Status.EXECUTE_FINISHED_WITH_ERRORS);
    }

    @Test
    void listOrphanUserBatches_shouldSortEntries() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        Instant now = Instant.now(systemUTC());
        Instant later = now.plusSeconds(10);
        when(allowedExecutions.isAllowed(PROVIDER, BATCH_ID)).thenReturn(true);
        when(orphanUserBatchRepository.list(CLIENT_ID, PROVIDER)).thenReturn(List.of(
                new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, now, now, OrphanUserBatch.Status.PREPARE_INITIATED),
                new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, later, later, OrphanUserBatch.Status.PREPARE_INITIATED)
        ));

        List<OrphanUserBatchDTO> dtos = orphanUserService.listOrphanUserBatches(clientToken, PROVIDER);

        assertThat(dtos).containsExactly(
                new OrphanUserBatchDTO(BATCH_ID, PROVIDER, later, later, OrphanUserBatch.Status.PREPARE_INITIATED.toString(), true),
                new OrphanUserBatchDTO(BATCH_ID, PROVIDER, now, now, OrphanUserBatch.Status.PREPARE_INITIATED.toString(), true)
        );
    }

    @Test
    void getOrphanUserBatch_whenNoBatch_shouldThrowException() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        assertThatThrownBy(() -> {
            when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.empty());

            orphanUserService.getOrphanUserBatch(clientToken, PROVIDER, BATCH_ID);
        }).isInstanceOf(OrphanUserBatchNotFoundException.class);
    }

    @Test
    void getOrphanUserBatch_whenBatchExists_shouldReturnIt() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        OrphanUserBatch orphanUserBatch = new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_INITIATED);
        when(allowedExecutions.isAllowed(PROVIDER, BATCH_ID)).thenReturn(false);
        when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(orphanUserBatch));

        OrphanUserBatchDTO dto = orphanUserService.getOrphanUserBatch(clientToken, PROVIDER, BATCH_ID);

        assertThat(dto).isEqualToComparingOnlyGivenFields(
                new OrphanUserBatchDTO(BATCH_ID, PROVIDER, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_INITIATED.toString(), false),
                "orphanUserBatchId", "provider", "status");
    }

    @Test
    void listOrphanUsers_shouldReturnProperResult() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        when(orphanUserRepository.listOrphanUsers(eq(CLIENT_ID), eq(PROVIDER), eq(BATCH_ID), anyInt())).thenReturn(
                Collections.singletonList(new OrphanUser(CLIENT_ID, PROVIDER, BATCH_ID, EXTERNAL_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL)));

        List<OrphanUserDTO> userDTOS = orphanUserService.listOrphanUsers(clientToken, PROVIDER, BATCH_ID);

        assertThat(userDTOS).hasSize(1);
        assertThat(userDTOS.get(0)).isEqualToComparingOnlyGivenFields(
                new OrphanUserDTO(BATCH_ID, PROVIDER, EXTERNAL_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL.toString()),
                "orphanUserBatchId", "provider", "externalUserId", "status");
    }

    @Test
    void deleteBatchData_shouldCallAllDeleteMethods() {
        ClientToken clientToken = createClientToken(CLIENT_ID);
        orphanUserService.deleteBatchData(clientToken, PROVIDER, BATCH_ID);

        verify(orphanUserBatchRepository).delete(CLIENT_ID, PROVIDER, BATCH_ID);
        verify(orphanUserRepository).delete(CLIENT_ID, PROVIDER, BATCH_ID);
        verify(orphanUserExternalIdRepository).delete(CLIENT_ID, PROVIDER, BATCH_ID);
    }
}

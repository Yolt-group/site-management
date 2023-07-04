package nl.ing.lovebird.sitemanagement.orphanuser;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static java.time.Clock.systemUTC;
import static nl.ing.lovebird.sitemanagement.lib.TestUtil.YOLT_APP_CLIENT_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrphanUserController.class)
@TestPropertySource(properties = "lovebird.providers.form.maxOrphanedUsersInResponse=10")
@AutoConfigureMockMvc
@Import({
        ExceptionHandlers.class
})
class OrphanUserControllerTest {

    private static final String USER_ID = "externalUserId";
    private static final UUID BATCH_ID = UUID.randomUUID();
    private static final String PROVIDER = "YODLEE";

    @MockBean
    private OrphanUserService orphanUserService;
    @MockBean
    private OrphanUserAllowedExecutions allowedExecutions;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestClientTokens testClientTokens;

    private ClientToken clientToken;

    @BeforeEach
    public void before() {
        Consumer<JwtClaims> mutator = claims -> {
            claims.setClaim("sub", YOLT_APP_CLIENT_ID.toString());
            claims.setClaim("isf", "assistance-portal-yts");
            claims.setClaim("psd2-licensed", true);
        };
        clientToken = testClientTokens.createClientToken(UUID.randomUUID(), UUID.randomUUID(), mutator);
    }

    @Test
    void testBatchPrepare() throws Exception {
        when(orphanUserService.startPreparingBatch(any(ClientToken.class), eq(PROVIDER))).thenReturn(BATCH_ID);

        String url = String.format("/orphan-users-batch/%s/prepare", PROVIDER);
        mockMvc.perform(post(url)
                        .header("client-token", clientToken.getSerialized()))
                .andExpect(status().isOk())
                .andExpect(content().string(BATCH_ID.toString()));

        verify(orphanUserService).startPreparingBatch(any(ClientToken.class), eq(PROVIDER));
    }

    @Test
    void testBatchExecute_whenExecutionNotAllowed() throws Exception {
        when(allowedExecutions.isAllowed(PROVIDER, BATCH_ID)).thenReturn(false);
        String url = String.format("/orphan-users-batch/%s/%s/execute", PROVIDER, BATCH_ID);

        mockMvc.perform(post(url)
                        .header("client-token", clientToken.getSerialized()))
                .andExpect(status().isForbidden());

        verify(orphanUserService, never()).executeOrphanUserBatch(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID));
    }

    @Test
    void testBatchExecute_whenExecutionAllowed() throws Exception {
        when(allowedExecutions.isAllowed(PROVIDER, BATCH_ID)).thenReturn(true);
        String url = String.format("/orphan-users-batch/%s/%s/execute", PROVIDER, BATCH_ID);

        mockMvc.perform(post(url)
                        .header("client-token", clientToken.getSerialized()))
                .andExpect(status().isOk());

        verify(orphanUserService).executeOrphanUserBatch(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID));
    }

    @Test
    void testBatchList() throws Exception {
        String url = String.format("/orphan-users-batch/%s", PROVIDER);
        OrphanUserBatchDTO batchDTO = new OrphanUserBatchDTO(BATCH_ID, PROVIDER, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_INITIATED.name(), false);
        when(orphanUserService.listOrphanUserBatches(any(ClientToken.class), eq(PROVIDER))).thenReturn(Collections.singletonList(batchDTO));

        mockMvc.perform(get(url)
                        .header("client-token", clientToken.getSerialized()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orphanUserBatchId", equalTo(BATCH_ID.toString())))
                .andExpect(jsonPath("$[0].provider", equalTo(PROVIDER)))
                .andExpect(jsonPath("$[0].status", equalTo(OrphanUserBatch.Status.PREPARE_INITIATED.name())))
                .andExpect(jsonPath("$[0].executionAllowed", equalTo(false)));

        verify(orphanUserService).listOrphanUserBatches(any(ClientToken.class), eq(PROVIDER));
    }

    @Test
    void testGetOrphanUserBatch() throws Exception {
        String url = String.format("/orphan-users-batch/%s/%s", PROVIDER, BATCH_ID);
        OrphanUserBatchDTO batchDTO = new OrphanUserBatchDTO(BATCH_ID, PROVIDER, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_INITIATED.name(), true);
        when(orphanUserService.getOrphanUserBatch(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID))).thenReturn(batchDTO);

        mockMvc.perform(get(url)
                        .header("client-token", clientToken.getSerialized()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orphanUserBatchId", equalTo(BATCH_ID.toString())))
                .andExpect(jsonPath("$.provider", equalTo(PROVIDER)))
                .andExpect(jsonPath("$.status", equalTo(OrphanUserBatch.Status.PREPARE_INITIATED.name())))
                .andExpect(jsonPath("$.executionAllowed", equalTo(true)));

        verify(orphanUserService).getOrphanUserBatch(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID));
    }

    @Test
    void testOrphanUserList() throws Exception {
        String url = String.format("/orphan-users-batch/%s/%s/orphan-users", PROVIDER, BATCH_ID);
        OrphanUserDTO userDTO = new OrphanUserDTO(BATCH_ID, PROVIDER, USER_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL.name());
        when(orphanUserService.listOrphanUsers(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID))).thenReturn(Collections.singletonList(userDTO));

        mockMvc.perform(get(url)
                        .header("client-token", clientToken.getSerialized()))
                .andExpect(status().isOk())
                //.andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.actualListSize", equalTo(1)))
                .andExpect(jsonPath("$.orphanUserList", hasSize(1)))
                .andExpect(jsonPath("$.orphanUserList[0].orphanUserBatchId", equalTo(BATCH_ID.toString())))
                .andExpect(jsonPath("$.orphanUserList[0].provider", equalTo(PROVIDER)))
                .andExpect(jsonPath("$.orphanUserList[0].externalUserId", equalTo(USER_ID)))
                .andExpect(jsonPath("$.orphanUserList[0].status", equalTo(OrphanUser.Status.INITIAL.name())));

        verify(orphanUserService).listOrphanUsers(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID));
    }

    @Test
    void testOrphanUserListExcessiveSize() throws Exception {
        String url = String.format("/orphan-users-batch/%s/%s/orphan-users", PROVIDER, BATCH_ID);
        OrphanUserDTO userDTO = new OrphanUserDTO(BATCH_ID, PROVIDER, USER_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUser.Status.INITIAL.name());
        List<OrphanUserDTO> dtos = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            dtos.add(userDTO);
        }
        when(orphanUserService.listOrphanUsers(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID))).thenReturn(dtos);

        mockMvc.perform(get(url)
                        .header("client-token", clientToken.getSerialized()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualListSize", equalTo(20)))
                .andExpect(jsonPath("$.orphanUserList", hasSize(10)));

        verify(orphanUserService).listOrphanUsers(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID));
    }

    @Test
    void testDeleteOrphanUserBatch() throws Exception {
        final String url = String.format("/orphan-users-batch/%s/%s", PROVIDER, BATCH_ID);

        mockMvc.perform(delete(url)
                        .header("client-token", clientToken.getSerialized()))
                .andExpect(status().isOk());

        verify(orphanUserService).deleteBatchData(any(ClientToken.class), eq(PROVIDER), eq(BATCH_ID));
    }


    @SpringBootApplication
    public static class LimitedApp {
        @Bean
        public Clock clock() {
            return systemUTC();
        }
    }
}

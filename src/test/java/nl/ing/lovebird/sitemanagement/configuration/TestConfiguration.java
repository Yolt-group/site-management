package nl.ing.lovebird.sitemanagement.configuration;

import com.yolt.securityutils.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import nl.ing.lovebird.sitemanagement.maintenanceclient.MaintenanceClient;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventService;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Slf4j
public class TestConfiguration {

    /**
     * Mock or spybeans in generic 'configuration'. This should only be classes that should always be mocked because they are close to
     * an external dependency. (it would be even better to point the client to an internal endpoint, and let wiremock serve a request,
     * however, that is too much refactoring to 'improve' at once).
     */
    @MockBean
    private MaintenanceClient maintenanceClient;
    @MockBean
    private FormProviderRestClient formProviderRestClient;
    @MockBean
    private ClientTokenRequesterService clientTokenRequesterService;
    @SpyBean
    private ProviderRestClient providerRestClient;
    @SpyBean
    private UserService userService;
    @SpyBean
    private UserSiteEventService userSiteEventService;
    @SpyBean
    private Clock clock;

    @Bean
    @Primary
    public VaultKeys vaultKeys() {
        VaultKeys vaultKeys = mock(VaultKeys.class);
        when(vaultKeys.getSymmetricKey(anyString())).thenReturn(SecretKey.from(Hex.decode("4060b34b5c66ea2de14c3cfd03c12ffce35697bc8fd1ac863bd7e27b3deb78ee")));

        return vaultKeys;
    }

    @Bean
    public CacheManager inspectableCacheManager() {
        return new InspectableCacheManager();
    }

    public static class InspectableCacheManager extends ConcurrentMapCacheManager {
        @Override
        protected Cache createConcurrentMapCache(String name) {
            Cache delegate = super.createConcurrentMapCache(name);
            return new InspectableCache(delegate);
        }
    }

    public static class InspectableCache implements Cache {
        @Getter
        long putOperations = 0;
        @Getter
        long getOperations = 0;

        Cache delegate;

        public InspectableCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            ++putOperations;
            return delegate.putIfAbsent(key, value);
        }

        @Override
        public void evict(Object key) {
            delegate.evict(key);
        }

        @Override
        public void put(Object key, Object value) {
            ++putOperations;
            delegate.put(key, value);
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            ++getOperations;
            return delegate.get(key, type);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ++getOperations;
            return delegate.get(key);
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            ++getOperations;
            return delegate.get(key, valueLoader);
        }

        @Override
        public void clear() {
            getOperations = putOperations = 0;
            delegate.clear();
        }
    }
}

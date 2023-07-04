package nl.ing.lovebird.sitemanagement.configuration;

import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.net.SocketTimeoutException;

@Configuration
public class HttpConfiguration {

    /**
     * Wiremock and Apache HTTP do not work well together due to Apache HTTP's connection pooling that only removes a connection from the
     * pool 2 seconds after it is no longer connected. This causes timeouts when running tests with Wiremock.
     * <p>
     * To ensure this does not happen, we take multiple measures (even though they might not all be needed)
     * - One connection in the pool, check after 10ms of inactivity whether it can be used
     * - Do not reuse connections
     * - Allow some retries in case of the suspicious exceptions
     * <p>
     * See also:
     * <p>
     * - https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins/55919043
     * - https://github.com/tomakehurst/wiremock/issues/97#issuecomment-605041024
     */
    private static HttpComponentsClientHttpRequestFactory getCustomRequestFactory() {
        var connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(1);
        connectionManager.setValidateAfterInactivity(10);
        var httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
                .setRetryHandler((exception, retries, context) -> {
                    if (retries > 3) {
                        return false;
                    }
                    return exception instanceof NoHttpResponseException || exception instanceof SocketTimeoutException;
                })
                .build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Bean
    @ConditionalOnProperty(value = "test.resttemplate.request-factory-customization", havingValue = "customizer", matchIfMissing = true)
    public RestTemplateCustomizer customHttpClientCustomizer() {
        return (restTemplate) -> restTemplate.setRequestFactory(getCustomRequestFactory());
    }

    @Bean
    @ConditionalOnProperty(value = "test.resttemplate.request-factory-customization", havingValue = "postprocess", matchIfMissing = false)
    public UseCustomApacheHttpClientHttpRequestFactory useCustomApacheHttpClientHttpRequestFactory() {
        return new UseCustomApacheHttpClientHttpRequestFactory();
    }

    /**
     * Bean post processor that replaces the HTTP client that RestTemplateBuilder uses in test scope.
     */
    public static class UseCustomApacheHttpClientHttpRequestFactory implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof RestTemplateBuilder) {
                var restTemplateBuilder = (RestTemplateBuilder) bean;
                return restTemplateBuilder.requestFactory(HttpConfiguration::getCustomRequestFactory);
            }
            return bean;
        }
    }
}

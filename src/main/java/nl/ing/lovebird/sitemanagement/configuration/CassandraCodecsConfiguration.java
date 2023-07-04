package nl.ing.lovebird.sitemanagement.configuration;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.extras.codecs.enums.EnumNameCodec;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.cassandra.codec.LocalDateTimeCodec;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.lib.types.ClientIdTypeCodec;
import nl.ing.lovebird.sitemanagement.orphanuser.OrphanUser;
import nl.ing.lovebird.sitemanagement.orphanuser.OrphanUserBatch;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.FailureReason;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSession;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * This config class makes sure all the generically used enums are only registered once, which prevents nasty overrides when not intended.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CassandraCodecsConfiguration {

    private final Cluster cluster;

    @PostConstruct
    public void onInit() {
        registerEnum(OrphanUser.Status.class);
        registerEnum(OrphanUserBatch.Status.class);
        registerEnum(ServiceType.class);
        registerEnum(LoginRequirement.class);
        registerEnum(CountryCode.class);
        registerEnum(StatusType.class);
        registerTypeCodec(new LocalDateTimeCodec());
        registerEnum(ConsentSession.Operation.class);
        registerEnum(ConnectionStatus.class);
        registerEnum(FailureReason.class);

        registerTypeCodec(InstantCodec.instance);
        registerTypeCodec(ClientIdTypeCodec.instance);
    }

    private <T> void registerTypeCodec(final TypeCodec<T> typeCodec) {
        this.cluster.getConfiguration().getCodecRegistry().register(typeCodec);
    }

    private <E extends Enum<E>> void registerEnum(Class<E> clazz) {
        this.cluster.getConfiguration().getCodecRegistry().register(new EnumNameCodec<>(clazz));
    }

}

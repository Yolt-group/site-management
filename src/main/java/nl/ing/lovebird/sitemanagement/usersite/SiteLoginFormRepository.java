package nl.ing.lovebird.sitemanagement.usersite;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository
class SiteLoginFormRepository extends CassandraRepository<SiteLoginForm> {

    @Autowired
    public SiteLoginFormRepository(final Session session) {
        super(session, SiteLoginForm.class);
    }

    public SiteLoginForm selectSiteLogin(final UUID siteId) {
        final Select selectSiteLoginFormByIdQuery = QueryBuilder.select().from(SiteLoginForm.TABLE_NAME);
        selectSiteLoginFormByIdQuery.where(eq(SiteLoginForm.ID_COLUMN, siteId));
        List<SiteLoginForm> siteLoginForms = select(selectSiteLoginFormByIdQuery);
        if (siteLoginForms.isEmpty()) {
            return null;
        }
        return siteLoginForms.get(0);
    }

    @Override
    protected void save(SiteLoginForm entity) {
        super.save(entity);
    }
}

package nl.ing.lovebird.sitemanagement.lib.spring.data;

import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * This {@link CrudRepository} extension disables default methods without bounds, e.a.
 * - findAll
 * - deleteAll
 *
 * @param <T>  the entity type
 * @param <ID> the primary key type
 */
@NoRepositoryBean
public interface RestrictedCrudRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    @Override
    default @NonNull Iterable<T> findAll() {
        throw new UnsupportedOperationException("This operation has been disabled.");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("This operation has been disabled.");
    }
}

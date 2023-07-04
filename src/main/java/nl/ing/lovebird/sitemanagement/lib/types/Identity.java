package nl.ing.lovebird.sitemanagement.lib.types;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Identity<T> {

    /**
     * @return the raw value of type T wrapped in this container
     */
    @JsonValue
    T unwrap();
}

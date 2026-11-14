package kz.astyq.zimran.alert.repository;

import kz.astyq.zimran.alert.model.entity.AlertPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertPreferenceRepository extends JpaRepository<AlertPreference, Long> {

    Optional<AlertPreference> findByScopeId(String scopeId);

    /**
     * Preferences that can match right now: not paused and enabled, in one of {@code cities}.
     */
    @Query("select p from AlertPreference p where p.city in :cities and p.paused = false and p.enabledAt is not null")
    List<AlertPreference> findActiveInCities(@Param("cities") Collection<String> cities);
}

package kz.astyq.zimran.ticketmaster.service;

import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.ticketmaster.model.dto.TmClassification;

import java.util.List;

public interface InterestMappingService {

    /**
     * Interests (in enum order) matched by any of the classifications; empty if none.
     */
    List<Interest> resolve(List<TmClassification> classifications);

    /**
     * Values for the Discovery API {@code classificationName} parameter that cover the interest:
     * the most specific name of each rule. Empty if the interest has no coverage.
     */
    List<String> classificationNamesFor(Interest interest);

    boolean hasCoverage(Interest interest);
}

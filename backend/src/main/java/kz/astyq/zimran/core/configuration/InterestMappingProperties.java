package kz.astyq.zimran.core.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import kz.astyq.zimran.event.model.enums.Interest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which Ticketmaster classifications count as which {@link Interest}
 * ({@code app.ticketmaster.interest-mapping}). An interest without rules has no coverage.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.ticketmaster")
public class InterestMappingProperties {

    private Map<Interest, List<@Valid ClassificationRule>> interestMapping = new LinkedHashMap<>();

    /**
     * Matches a classification when every non-null field equals (case-insensitively) the
     * classification's segment / genre / sub-genre name; null is a wildcard.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationRule {

        private String segment;
        private String genre;
        private String subGenre;

        @AssertTrue(message = "a classification rule needs at least one of segment, genre, subGenre")
        public boolean isConstrained() {
            return StringUtils.hasText(segment) || StringUtils.hasText(genre) || StringUtils.hasText(subGenre);
        }
    }

    public List<ClassificationRule> rulesFor(Interest interest) {
        List<ClassificationRule> rules = interestMapping.get(interest);
        return rules != null ? rules : new ArrayList<>();
    }
}

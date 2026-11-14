package kz.astyq.zimran.assistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.astyq.zimran.assistant.model.enums.AssistantChatField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatViewResponse {

    /** Who answered: always {@code ai} (the model reads the message and ranks the events). */
    public static final String ENGINE = "ai";

    @Schema(example = ENGINE)
    private String engine;
    private AssistantChatPreferencesViewResponse preferences;
    @Schema(description = "Still needed before events can be recommended: city, interests, dates")
    private List<AssistantChatField> pendingFields;
    @Schema(description = "Follow-up questions (fixed i18n texts, never model text); empty once a result is given")
    private List<String> clarifications;
    @Schema(description = "The recommendation; null while fields are pending")
    private AssistantChatPlanViewResponse result;
    @Schema(description = "Send back on the next turns; set once the chat has given a result")
    private String conversationId;
}

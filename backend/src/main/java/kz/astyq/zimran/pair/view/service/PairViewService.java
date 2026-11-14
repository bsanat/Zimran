package kz.astyq.zimran.pair.view.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.pair.model.dto.PairJoinCreateRequest;
import kz.astyq.zimran.pair.model.dto.PairJoinViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSessionCreateRequest;
import kz.astyq.zimran.pair.model.dto.PairSessionCreateViewResponse;
import kz.astyq.zimran.pair.model.dto.PairSessionViewResponse;
import kz.astyq.zimran.pair.model.dto.PairVoteCreateRequest;

public interface PairViewService {

    PairSessionCreateViewResponse create(PairSessionCreateRequest request, RequestScope scope);

    PairJoinViewResponse join(PairJoinCreateRequest request, RequestScope scope);

    PairSessionViewResponse get(String sessionId, String participantToken);

    PairSessionViewResponse vote(String sessionId, String participantToken, PairVoteCreateRequest request);

    void revoke(String sessionId, String participantToken);
}

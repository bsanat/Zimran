package kz.astyq.zimran.buddy.service.impl;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.buddy.model.domain.BuddyOverview;
import kz.astyq.zimran.buddy.model.domain.BuddyPostWithRequests;
import kz.astyq.zimran.buddy.model.domain.BuddyRequestWithPost;
import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;
import kz.astyq.zimran.buddy.repository.BuddyPostRepository;
import kz.astyq.zimran.buddy.repository.BuddyRequestRepository;
import kz.astyq.zimran.buddy.service.BuddyQueryService;
import kz.astyq.zimran.moderation.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuddyQueryServiceImpl implements BuddyQueryService {

    private final BuddyPostRepository buddyPostRepository;
    private final BuddyRequestRepository buddyRequestRepository;
    private final BlockService blockService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<BuddyPost> openPosts(String eventId, RequestScope viewer) {
        List<BuddyPost> posts = buddyPostRepository.findByEventAndStatusNotExpired(eventId, BuddyPostStatus.OPEN,
                clock.instant());
        if (viewer == null || posts.isEmpty()) {
            return posts;
        }
        Set<String> hidden = blockService.blockedEitherWay(viewer.getScopeId());
        return hidden.isEmpty() ? posts : posts.stream().filter(post -> !hidden.contains(post.getOwnerScopeId())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BuddyOverview overview(RequestScope scope) {
        List<BuddyPost> posts = buddyPostRepository.findTop50ByOwnerScopeIdOrderByCreatedAtDesc(scope.getScopeId());
        Map<Long, List<BuddyRequest>> incoming = posts.isEmpty() ? Map.of()
                : buddyRequestRepository.findAllByPostIdInOrderByCreatedAtAsc(posts.stream().map(BuddyPost::getId).toList())
                        .stream().collect(Collectors.groupingBy(BuddyRequest::getPostId));
        List<BuddyRequest> sent = buddyRequestRepository.findTop50BySenderScopeIdOrderByCreatedAtDesc(scope.getScopeId());
        Map<Long, BuddyPost> sentTo = sent.isEmpty() ? Map.of()
                : buddyPostRepository.findAllByIdIn(sent.stream().map(BuddyRequest::getPostId).distinct().toList())
                        .stream().collect(Collectors.toMap(BuddyPost::getId, Function.identity()));
        return BuddyOverview.builder()
                .posts(posts.stream().map(post -> BuddyPostWithRequests.builder()
                        .post(post)
                        .requests(incoming.getOrDefault(post.getId(), List.of()))
                        .build()).toList())
                .requests(sent.stream().filter(request -> sentTo.containsKey(request.getPostId()))
                        .map(request -> BuddyRequestWithPost.builder()
                                .request(request)
                                .post(sentTo.get(request.getPostId()))
                                .build()).toList())
                .build();
    }
}

package kz.astyq.zimran.buddy.service.impl;

import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;
import kz.astyq.zimran.buddy.repository.BuddyPostRepository;
import kz.astyq.zimran.buddy.service.BuddyExpiryService;
import kz.astyq.zimran.buddy.util.BuddyPostStatuses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class BuddyExpiryServiceImpl implements BuddyExpiryService {

    private final BuddyPostRepository buddyPostRepository;
    private final Clock clock;

    @Override
    @Transactional
    public int expireDue() {
        return buddyPostRepository.expireDue(BuddyPostStatus.EXPIRED, BuddyPostStatuses.ACTIVE, clock.instant());
    }
}

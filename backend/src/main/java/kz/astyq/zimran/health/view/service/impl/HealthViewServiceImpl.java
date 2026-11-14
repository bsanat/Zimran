package kz.astyq.zimran.health.view.service.impl;

import kz.astyq.zimran.health.model.dto.HealthViewResponse;
import kz.astyq.zimran.health.view.service.HealthViewService;
import org.springframework.stereotype.Service;

@Service
public class HealthViewServiceImpl implements HealthViewService {

    @Override
    public HealthViewResponse getHealth() {
        return HealthViewResponse.builder().ok(true).build();
    }
}

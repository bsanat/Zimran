package kz.astyq.zimran.meta.view.service.impl;

import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.meta.model.dto.CategoryViewResponse;
import kz.astyq.zimran.meta.model.dto.MetaViewResponse;
import kz.astyq.zimran.meta.util.MessageCode;
import kz.astyq.zimran.meta.view.service.MetaViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class MetaViewServiceImpl implements MetaViewService {

    private final MessageService messageService;

    @Override
    public MetaViewResponse getMeta() {
        return MetaViewResponse.builder()
                .categories(Arrays.stream(CategorySlug.values()).map(this::toViewResponse).toList())
                .build();
    }

    private CategoryViewResponse toViewResponse(CategorySlug slug) {
        return CategoryViewResponse.builder()
                .id(slug.getId())
                .label(messageService.getMessage(MessageCode.category(slug)))
                .interests(slug.interests().stream().map(Interest::getId).toList())
                .build();
    }
}

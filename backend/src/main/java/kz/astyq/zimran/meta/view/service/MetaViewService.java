package kz.astyq.zimran.meta.view.service;

import kz.astyq.zimran.meta.model.dto.MetaViewResponse;

/**
 * Public reference data.
 */
public interface MetaViewService {

    /**
     * Every category slug with its label in the request's language and its interests.
     */
    MetaViewResponse getMeta();
}

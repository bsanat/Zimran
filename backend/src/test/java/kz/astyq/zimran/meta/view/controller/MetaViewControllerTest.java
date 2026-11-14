package kz.astyq.zimran.meta.view.controller;

import kz.astyq.zimran.core.i18n.impl.MessageServiceImpl;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import kz.astyq.zimran.meta.view.service.impl.MetaViewServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetaViewController.class)
@Import({MetaViewServiceImpl.class, MessageServiceImpl.class})
class MetaViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/meta: every category slug with label and interests, public cache")
    void meta() throws Exception {
        mockMvc.perform(get("/api/meta"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(jsonPath("$.categories", hasSize(CategorySlug.values().length)))
                .andExpect(jsonPath("$.categories[0].id").value("music"))
                .andExpect(jsonPath("$.categories[0].label").value("Music"))
                .andExpect(jsonPath("$.categories[1].id").value("food_drinks"))
                .andExpect(jsonPath("$.categories[1].label").value("Food & Drinks"))
                .andExpect(jsonPath("$.categories[1].interests[0]").value("food"))
                .andExpect(jsonPath("$.categories[10].id").value("other"));
    }

    @Test
    @DisplayName("GET /api/meta: labels follow the request language")
    void russian() throws Exception {
        mockMvc.perform(get("/api/meta").header("Accept-Language", "ru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].label").value("Музыка"));
    }
}

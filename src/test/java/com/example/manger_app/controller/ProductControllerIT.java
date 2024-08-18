package com.example.manger_app.controller;

import com.example.manger_app.controller.payload.UpdateProductPayload;
import com.example.manger_app.entity.Product;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@WireMockTest(httpPort = 54321)
public class ProductControllerIT {
    @Autowired
    MockMvc mockMvc;

    @Test
    void getProductPage_ProductExists_ReturnsProductPage() throws Exception {
        var request = MockMvcRequestBuilders.get("/catalogue/products/1")
                .with(user("user").roles("MANAGER"));

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/catalogue-api/products/1"))
                .willReturn(WireMock.okJson("""
                        {
                            "id": 1,
                            "title": "Товар 1",
                            "details": "Описание товара 1"
                        }""")));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        model().attribute("product",
                                new Product(1, "Товар 1", "Описание товара 1")),
                        view().name("catalogue/products/product")
                );
    }

    @Test
    void getProductPage_ProductDoesNotExists_ReturnsError404Page() throws Exception {
        var request = MockMvcRequestBuilders.get("/catalogue/products/1")
                .with(user("user").roles("MANAGER"));

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/catalogue-api/products/1"))
                .willReturn(WireMock.notFound()));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().isNotFound(),
                        model().attribute("error", "Товар не найден"),
                        view().name("errors/404")
                );
    }

    @Test
    void getProduct_UserIsNotAuthorized_ReturnsForbidden() throws Exception {
        var request = MockMvcRequestBuilders.get("/catalogue/products/1")
                .with(user("user"));

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getProductEditPage_ProductExists_ReturnsProductEditPage() throws Exception {
        var request = MockMvcRequestBuilders.get("/catalogue/products/1/edit")
                .with(user("user").roles("MANAGER"));

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/catalogue-api/products/1"))
                .willReturn(WireMock.okJson("""
                        {
                            "id": 1,
                            "title": "Товар 1",
                            "details": "Описание товара 1"
                        }""")));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        model().attribute("product",
                                new Product(1, "Товар 1", "Описание товара 1")),
                        view().name("catalogue/products/edit")
                );
    }

    @Test
    void getProductEditPage_ProductDoesNotExist_ReturnsError404Page() throws Exception {
        var request = MockMvcRequestBuilders.get("/catalogue/products/1/edit")
                .with(user("user").roles("MANAGER"));

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/catalogue-api/products/1"))
                .willReturn(WireMock.notFound()));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().isNotFound(),
                        view().name("errors/404"),
                        model().attribute("error", "Товар не найден")
                );
    }

    @Test
    void getProductEditPage_UserIsNotAuthorized_ReturnsForbidden() throws Exception {
        var request = MockMvcRequestBuilders.get("/catalogue/products/1/edit")
                .with(user("user"));

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProduct_RequestIsValid_RedirectsToProductPage() throws Exception {
        var request = MockMvcRequestBuilders.post("/catalogue/products/1/edit")
                .param("title","Обновленный товар 1")
                .param("details", "Обновленное описание товара 1")
                .with(user("user").roles("MANAGER"))
                .with(csrf());

        WireMock.stubFor(WireMock.get("/catalogue-api/products/1")
                .willReturn(WireMock.okJson("""
                        {
                            "id": 1,
                            "title": "Товар 1",
                            "details": "Описание товара 1"
                        }""")));

        WireMock.stubFor(WireMock.patch("/catalogue-api/products/1")
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "title": "Обновленный товар 1",
                            "details": "Обновленное описание товара 1"
                        }"""))
                .willReturn(WireMock.noContent()));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().is3xxRedirection(),
                        redirectedUrl("/catalogue/products/1")
                );

        WireMock.verify(WireMock.patchRequestedFor(WireMock.urlPathMatching("/catalogue-api/products/1"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "title": "Обновленный товар 1",
                            "details": "Обновленное описание товара 1"
                        }""")));
    }

    @Test
    void updateProduct_RequestIsInvalid_ReturnsProductEditPage() throws Exception {
        var request = MockMvcRequestBuilders.post("/catalogue/products/1/edit")
                .param("title", " ")
                .with(user("user").roles("MANAGER"))
                .with(csrf());

        WireMock.stubFor(WireMock.get("/catalogue-api/products/1")
                .willReturn(WireMock.okJson("""
                        {
                            "id": 1,
                            "title": "Товар",
                            "details": "Описание товара"
                        }
                        """)));

        WireMock.stubFor(WireMock.patch("/catalogue-api/products/1")
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "title": " ",
                            "details": null
                        }"""))
                .willReturn(WireMock.badRequest()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                        .withBody("""
                                {
                                    "errors": ["Ошибка 1", "Ошибка 2"]
                                }""")));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().isBadRequest(),
                        view().name("catalogue/products/edit"),
                        model().attribute("product", new Product(1, "Товар", "Описание товара")),
                        model().attribute("errors", List.of("Ошибка 1", "Ошибка 2")),
                        model().attribute("payload", new UpdateProductPayload(" ", null))
                );

        WireMock.verify(WireMock.patchRequestedFor(WireMock.urlPathMatching("/catalogue-api/products/1"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "title": " ",
                            "details": null
                        }""")));
    }

    @Test
    void updateProduct_ProductDoesNotExist_ReturnsError404Page() throws Exception {
        var request = MockMvcRequestBuilders.post("/catalogue/products/1/edit")
                .param("title", "Новое название")
                .param("details", "Новое описание товара")
                .with(user("user").roles("MANAGER"))
                .with(csrf());

        WireMock.stubFor(WireMock.get("/catalogue-api/products/1")
                .willReturn(WireMock.notFound()));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().isNotFound(),
                        view().name("errors/404"),
                        model().attribute("error", "Товар не найден")
                );
    }

    @Test
    void updateProduct_UserIsNotAuthorized_ReturnsForbidden() throws Exception {
        var request = MockMvcRequestBuilders.post("/catalogue/products/1/edit")
                .param("title", "Новое название")
                .param("details", "Новое описание товара")
                .with(user("user"))
                .with(csrf());

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_ProductExists_RedirectsToProductsListPage() throws Exception {
        var request = MockMvcRequestBuilders.post("/catalogue/products/1/delete")
                .with(user("user").roles("MANAGER"))
                .with(csrf());

        WireMock.stubFor(WireMock.get("/catalogue-api/products/1")
                .willReturn(WireMock.okJson("""
                        {
                            "id": 1,
                            "title": "Товар",
                            "details": "Описание товара"
                        }
                        """)));

        WireMock.stubFor(WireMock.delete("/catalogue-api/products/1")
                .willReturn(WireMock.noContent()));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().is3xxRedirection(),
                        redirectedUrl("/catalogue/products/list")
                );

        WireMock.verify(WireMock.deleteRequestedFor(WireMock.urlPathMatching("/catalogue-api/products/1")));
    }

    @Test
    void deleteProduct_ProductDoesNotExist_ReturnsError404Page() throws Exception {
        var request = MockMvcRequestBuilders.post("/catalogue/products/1/delete")
                .with(user("user").roles("MANAGER"))
                .with(csrf());

        WireMock.stubFor(WireMock.get("/catalogue-api/products/1")
                .willReturn(WireMock.notFound()));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().isNotFound(),
                        view().name("errors/404"),
                        model().attribute("error", "Товар не найден")
                );
    }

    @Test
    void deleteProduct_UserIsNotAuthorized_ReturnsForbidden() throws Exception {
        var request = MockMvcRequestBuilders.post("/catalogue/products/1/delete")
                .with(user("user"))
                .with(csrf());

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}

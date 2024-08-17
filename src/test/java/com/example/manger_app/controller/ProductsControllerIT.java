package com.example.manger_app.controller;

import com.example.manger_app.controller.payload.NewProductPayload;
import com.example.manger_app.entity.Product;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@WireMockTest(httpPort = 54321)
public class ProductsControllerIT {
    @Autowired
    MockMvc mockMvc;

    @Test
    void getProductsList_ReturnsProductsListPage() throws Exception {
        var request = MockMvcRequestBuilders.get("/catalogue/products/list")
                .queryParam("filter", "товар")
                .with(user("second.user").roles("MANAGER"));

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/catalogue-api/products"))
                .withQueryParam("filter", WireMock.equalTo("товар"))
                .willReturn(WireMock.ok("""
                        [
                            {"id": 1, "title": "Товар 1", "details": "Описание товара 1"},
                            {"id": 2, "title": "Товар 2", "details": "Описание товара 2"}
                        ]""").withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        mockMvc.perform(request)
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        view().name("catalogue/products/list"),
                        model().attribute("filter", "товар"),
                        model().attribute("products", List.of(
                                new Product(1, "Товар 1", "Описание товара 1"),
                                new Product(2, "Товар 2", "Описание товара 2")
                        ))
                );

        WireMock.verify(WireMock.getRequestedFor(WireMock.urlPathMatching("/catalogue-api/products"))
                .withQueryParam("filter", WireMock.equalTo("товар")));
    }

    @Test
    void getProductsList_UserIsNotAuthorized_ReturnsForbidden() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.get("/catalogue/products/list")
                .queryParam("filter", "товар")
                .with(user("user"));

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getNewProductPage_ReturnsProductPage() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.get("/catalogue/products/create")
                .with(user("second.user").roles("MANAGER"));

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        view().name("catalogue/products/new_product")
                );
    }

    @Test
    void getNewProductPage_UserIsNotAuthorized_ReturnsForbidden() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.get("/catalogue/products/create")
                .with(user("user"));

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_RequestIsValid_RedirectsToProductPage() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.post("/catalogue/products/create")
                .param("title", "Новый товар")
                .param("details", "Описание нового товара")
                .with(user("user").roles("MANAGER"))
                .with(csrf());

        WireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/catalogue-api/products"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "title": "Новый товар",
                            "details": "Описание нового товара"
                        }"""))
                .willReturn(WireMock.created()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "id": 1,
                                    "title": "Новый товар",
                                    "details": "Описание нового товара"
                                }""")));

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpectAll(
                        status().is3xxRedirection(),
                        header().string(HttpHeaders.LOCATION, "/catalogue/products/1")
                );

        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/catalogue-api/products"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "title": "Новый товар",
                            "details": "Описание нового товара"
                        }""")));
    }

    @Test
    void createProduct_RequestIsInvalid_RedirectsToProductPage() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.post("/catalogue/products/create")
                .param("title", " ")
                .with(user("user").roles("MANAGER"))
                .with(csrf());

        WireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/catalogue-api/products"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "title": " ",
                            "details": null
                        }"""))
                .willReturn(WireMock.badRequest()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "errors": ["Ошибка 1", "Ошибка 2"]
                                }""")));

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpectAll(
                        status().isBadRequest(),
                        view().name("catalogue/products/new_product"),
                        model().attribute("payload", new NewProductPayload(" ", null)),
                        model().attribute("errors", List.of("Ошибка 1", "Ошибка 2"))
                );

        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/catalogue-api/products"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "title": " ",
                            "details": null
                        }""")));
    }

    @Test
    void createProduct_UserIsNotAuthorized_ReturnsForbidden() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.post("/catalogue/products/create")
                .param("title", "Новый товар")
                .param("details", "Описание нового товара")
                .with(user("user"))
                .with(csrf());

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}

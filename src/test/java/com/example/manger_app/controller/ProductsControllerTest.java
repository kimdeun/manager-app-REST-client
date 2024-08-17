package com.example.manger_app.controller;

import com.example.manger_app.client.BadRequestException;
import com.example.manger_app.client.ProductsRestClient;
import com.example.manger_app.controller.payload.NewProductPayload;
import com.example.manger_app.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Модульные тесты ProductsController")
class ProductsControllerTest {
    @Mock
    ProductsRestClient productsRestClient;

    @InjectMocks
    ProductsController controller;

    @Test
    @DisplayName("getProductsList вернет страницу списка отфильтрованных товаров")
    void getProductsList_ReturnsListOfProductsPage() {
        var model = new ConcurrentModel();
        var filter = "товар";

        var products = IntStream.range(1, 4)
                .mapToObj(i -> new Product(i, "Товар №%s".formatted(i),
                        "Описание товара №%s".formatted(i)))
                .toList();

        doReturn(products).when(productsRestClient).findAllProducts(filter);

        var result = controller.getProductsList(model, filter);

        assertEquals("catalogue/products/list", result);
        assertEquals(products, model.getAttribute("products"));
        assertEquals(filter, model.getAttribute("filter"));
    }

    @Test
    @DisplayName("getNewProductPage вернет страницу нового товара")
    void getNewProductPage_ReturnsNewProductPage() {
        var result = controller.getNewProductPage();

        assertEquals("catalogue/products/new_product", result);
    }

    @Test
    @DisplayName("createProduct создаст новый товар и перенаправит на страницу товара")
    void createProduct_RequestIsValid_ReturnsRedirectionToProductPage() {
        var payload = new NewProductPayload("Новый товар", "Описание нового товара");
        var model = new ConcurrentModel();

        doReturn(new Product(1, "Новый товар", "Описание нового товара"))
                .when(productsRestClient)
                .createProduct("Новый товар", "Описание нового товара");

        var result = controller.createProduct(payload, model);

        assertEquals("redirect:/catalogue/products/1", result);

        verify(productsRestClient).createProduct("Новый товар", "Описание нового товара");
        verifyNoMoreInteractions(productsRestClient);
    }

    @Test
    @DisplayName("createProduct вернет страницу с ошибками, если запрос невалидный")
    void createProduct_RequestIsInValid_ReturnsProductFormWithErrors() {
        var payload = new NewProductPayload(" ", null);
        var model = new ConcurrentModel();

        doThrow(new BadRequestException(List.of("Ошибка 1", "Ошибка 2")))
                .when(productsRestClient)
                .createProduct(" ", null);

        var result = controller.createProduct(payload, model);

        assertEquals("catalogue/products/new_product", result);
        assertEquals(payload, model.getAttribute("payload"));
        assertEquals(List.of("Ошибка 1", "Ошибка 2"), model.getAttribute("errors"));

        verify(productsRestClient).createProduct(" ", null);
        verifyNoMoreInteractions(productsRestClient);
    }
}
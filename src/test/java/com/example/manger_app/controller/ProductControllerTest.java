package com.example.manger_app.controller;

import com.example.manger_app.client.BadRequestException;
import com.example.manger_app.client.ProductsRestClient;
import com.example.manger_app.controller.payload.UpdateProductPayload;
import com.example.manger_app.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {
    @Mock
    ProductsRestClient productsRestClient;

    @Mock
    MessageSource messageSource;

    @InjectMocks
    ProductController productController;

    @Test
    void product_ProductExist_ReturnProduct() {
        var product = new Product(1, "Название товара №1", "Описание товара #1");

        doReturn(Optional.of(product)).when(productsRestClient).findProduct(1);

        var result = productController.product(1);

        assertEquals(product, result);

        verify(productsRestClient).findProduct(1);
        verifyNoMoreInteractions(productsRestClient);
    }

    @Test
    void product_ProductDoesNotExist_ThrowsNoSuchElementException() {
        var exception = assertThrows(NoSuchElementException.class, () -> productController.product(1));

        assertEquals("catalogue.errors.product.not_found", exception.getMessage());

        verify(productsRestClient).findProduct(1);
        verifyNoMoreInteractions(productsRestClient);
    }

    @Test
    void getProductPage_ReturnsProductPage() {
        var result = productController.getProductPage();

        assertEquals("catalogue/products/product", result);

        verifyNoMoreInteractions(productsRestClient);
    }

    @Test
    void getProductEditPage_ReturnsProductEditPage() {
        var result = productController.getProductEditPage();

        assertEquals("catalogue/products/edit", result);

        verifyNoMoreInteractions(productsRestClient);
    }

    @Test
    void updateProduct_RequestIsValid_RedirectsToProductPage() {
        var product = new Product(1, "Товар 1", "Описание товара 1");
        var payload = new UpdateProductPayload("Товар 1 обновленный",
                "Описание товара 1 обновленное");
        var model = new ConcurrentModel();
        var response = new MockHttpServletResponse();

        var result = productController.updateProduct(product, payload, model, response);

        assertEquals("redirect:/catalogue/products/1", result);

        verify(productsRestClient).updateProduct(1, "Товар 1 обновленный",
                "Описание товара 1 обновленное");
        verifyNoMoreInteractions(productsRestClient);
    }

    @Test
    void updateProduct_RequestIsInvalid_RedirectsToProductPage() {
        var product = new Product(1, "Товар 1", "Описание товара 1");
        var payload = new UpdateProductPayload(" ", null);
        var model = new ConcurrentModel();
        var response = new MockHttpServletResponse();

        doThrow(new BadRequestException(List.of("Ошибка 1", "Ошибка 2")))
                .when(productsRestClient)
                .updateProduct(1, " ", null);

        var result = productController.updateProduct(product, payload, model, response);

        assertEquals("catalogue/products/edit", result);
        assertEquals(payload, model.getAttribute("payload"));
        assertEquals(List.of("Ошибка 1", "Ошибка 2"), model.getAttribute("errors"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());

        verify(productsRestClient).updateProduct(1, " ", null);
        verifyNoMoreInteractions(productsRestClient);
    }

    @Test
    void deleteProduct_RedirectsToProductsListPage() {
        var product = new Product(1, "Товар 1", "Описание товара 1");

        var result = productController.deleteProduct(product);

        assertEquals("redirect:/catalogue/products/list", result);

        verify(productsRestClient).deleteProduct(1);
        verifyNoMoreInteractions(productsRestClient);
    }

    @Test
    void handleNoSuchElementException_Returns404ErrorPage() {
        var exception = new NoSuchElementException("error");
        var model = new ConcurrentModel();
        var response = new MockHttpServletResponse();
        var locale = Locale.of("ru");

        var result = productController.handleNoSuchElementException(exception, model, response, locale);

        assertEquals("errors/404", result);
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());

        verify(this.messageSource).getMessage("error", new Object[0], "error", Locale.of("ru"));
        verifyNoMoreInteractions(this.messageSource);
        verifyNoInteractions(this.productsRestClient);
    }
}

package com.example.manger_app.config;

import com.example.manger_app.client.RestClientProductsRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientBeans  {
    @Bean
    public RestClientProductsRestClient restClientProductsRestClient(
            @Value("${services.catalogue.uri:http://localhost:8081}") String catalogueBaseUri,
            @Value("${services.catalogue.username:}") String catalogueUserName,
            @Value("${services.catalogue.password:}") String cataloguePassword) {
        return new RestClientProductsRestClient(RestClient.builder()
                .baseUrl(catalogueBaseUri)
                .requestInterceptor(new BasicAuthenticationInterceptor(catalogueUserName, cataloguePassword) )
                .build());
    }
}

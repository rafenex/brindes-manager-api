package br.com.rafael.brindesmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class PostalCodeClientConfig {

    @Bean
    public RestClient postalCodeRestClient() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(3));
        // Dedicated builder: never inherits incoming credentials or application interceptors.
        return RestClient.builder()
                .baseUrl("https://viacep.com.br")
                .requestFactory(factory)
                .build();
    }
}

package com.mycomplex.news.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

@Configuration(proxyBeanMethods = false)
public class BlobStorageConfig {

    @Bean
    BlobContainerClient newsBlobContainerClient(
            @Value("${mycomplex.storage.blob.connection-string}")
            String connectionString,
            @Value("${mycomplex.storage.blob.container}")
            String containerName
    ) {
        return new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient();
    }
}
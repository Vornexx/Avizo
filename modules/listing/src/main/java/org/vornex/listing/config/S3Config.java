package org.vornex.listing.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vornex.listing.util.StorageProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class) // включаем бин конфигурации
public class S3Config {


    @Bean
    public S3Client s3Client(StorageProperties props) {
        System.out.println("Storage props: " + props.getAccessKey() + ", " + props.getSecretKey());
        S3Configuration svcConf = S3Configuration.builder()
                .pathStyleAccessEnabled(props.isPathStyleAccess())
                .build();

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                ))
                .region(Region.of(props.getRegion()))
                .serviceConfiguration(svcConf)
                .endpointOverride(URI.create(props.getEndpoint()))
                // ApacheHttpClient — production-ready, configurable (timeouts/pooling)
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(10)))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties props) {
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                ))
                .region(Region.of(props.getRegion()))
                .endpointOverride(URI.create(props.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(props.isPathStyleAccess())
                        .build())
                .build();
    }
}


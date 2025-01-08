package com.ns.result.config;


import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {


    @Value("${aws.os.scheme:https}")
    private String SCHEME;

    @Value("${aws.os.endpoint}")
    private String HOST;

    @Value("${aws.os.port:443}")
    private int PORT;

    @Value("${aws.os.account}")
    private String USERNAME;

    @Value("${aws.os.password}")
    private String PASSWORD;

    /**
     * OpenSearchClient Bean 설정
     *
     * @return OpenSearchClient
     */
    @Bean
    public OpenSearchClient openSearchClient() {

        final HttpHost httpHost = new HttpHost(SCHEME, HOST, PORT);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(httpHost),
                new UsernamePasswordCredentials(USERNAME, PASSWORD.toCharArray()));

        final OpenSearchTransport transport =
                ApacheHttpClient5TransportBuilder.builder(httpHost)
                        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)).build();

        return new OpenSearchClient(transport);
    }
}


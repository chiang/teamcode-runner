package io.teamcode.runner.config;

import io.teamcode.runner.network.NetworkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

/**
 * Created by chiang on 2017. 5. 5..
 */
@Configuration
public class RestConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

        logger.debug("Initializing rest template ...");

        return restTemplate;
    }

    /**
     * Timeout 은 내부 네트워크에서 구성되는 것을 고려, 짧게 잡는다.
     *
     * TODO 설정으로 빼자...
     *
     * @return
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(1000);
        factory.setConnectTimeout(1000);

        return factory;
    }

}

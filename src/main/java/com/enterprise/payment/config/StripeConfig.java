package com.enterprise.payment.config;

import com.stripe.StripeClient;
import com.stripe.net.HttpURLConnectionClient;
import com.stripe.net.RequestOptions;
import com.stripe.net.StripeResponseGetter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.connect-timeout}")
    private int connectTimeout;

    @Value("${stripe.read-timeout}")
    private int readTimeout;

    /**
     * WHAT IT DOES: Creates a Stripe client bean with custom HTTP settings.
     * WHAT HAPPENS: Instead of Stripe.apiKey = "sk_..." (global/unsafe),
     * we create a StripeClient with our key injected from environment.
     * The custom HttpClient sets timeouts to prevent hanging threads.
     * connectTimeout = max time to establish connection to Stripe servers.
     * readTimeout = max time to wait for Stripe's response.
     * Both prevent your threads from blocking indefinitely on network issues.
     */
    @Bean
    public StripeClient stripeClient() {
        return StripeClient.builder()
                .setApiKey(secretKey)
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    /**
     * WHAT IT DOES: Provides a pre-built RequestOptions bean
     * with standard headers for every Stripe call.
     * WHAT HAPPENS: Stripe-Version header pins the API version
     * so Stripe breaking changes never affect your app unexpectedly.
     */
//    @Bean
//    public RequestOptions defaultRequestOptions() {
//        return RequestOptions.builder()
//                .m
//                .build();
//    }
}


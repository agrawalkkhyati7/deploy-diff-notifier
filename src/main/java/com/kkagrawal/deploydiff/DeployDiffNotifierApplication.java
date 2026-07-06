package com.kkagrawal.deploydiff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class DeployDiffNotifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeployDiffNotifierApplication.class, args);
	}

	@Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }


}

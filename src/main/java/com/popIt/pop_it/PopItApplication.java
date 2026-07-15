package com.popIt.pop_it;

import com.popIt.pop_it.global.config.AwsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(AwsProperties.class)
@SpringBootApplication
public class PopItApplication {

	public static void main(String[] args) {
		SpringApplication.run(PopItApplication.class, args);
	}

}

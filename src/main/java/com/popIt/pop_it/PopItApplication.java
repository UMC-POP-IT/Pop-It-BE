package com.popIt.pop_it;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PopItApplication {

	public static void main(String[] args) {
		SpringApplication.run(PopItApplication.class, args);
	}

}

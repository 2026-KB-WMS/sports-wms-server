package com.kb.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class WmsServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WmsServerApplication.class, args);
	}

}

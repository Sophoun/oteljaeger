package com.sophoun.oteljaeger.demo;

import com.sophoun.oteljaeger.starter.EnableOtelJaeger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableOtelJaeger
public class OteljaegerDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(OteljaegerDemoApplication.class, args);
	}
}

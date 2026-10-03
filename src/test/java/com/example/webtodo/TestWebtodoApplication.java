package com.example.webtodo;

import org.springframework.boot.SpringApplication;

public class TestWebtodoApplication {

	public static void main(String[] args) {
		SpringApplication.from(WebtodoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

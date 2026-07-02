package com.course.promptplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.course.promptplatform.mapper")
@SpringBootApplication
public class AiPromptTemplatePlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiPromptTemplatePlatformApplication.class, args);
	}

}

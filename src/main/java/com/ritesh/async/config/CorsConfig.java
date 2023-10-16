package com.ritesh.async.config;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class CorsConfig implements WebMvcConfigurer {
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {

		registry.addMapping("/**").allowedOrigins("http://localhost","*","null").allowedMethods("*")
				.allowedHeaders("*").allowCredentials(true);
	}
}

package com.printkon.pdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import jakarta.validation.Validator;

@Slf4j
@Configuration
public class ValidationConfig {

	@Bean
	public Validator validator() {
		log.info("Configuring Bean Validation");
		return new LocalValidatorFactoryBean();
	}

	@Bean
	public MethodValidationPostProcessor methodValidationPostProcessor() {
		log.info("Configuring Method Validation");
		MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
		processor.setValidator(validator());
		return processor;
	}
}
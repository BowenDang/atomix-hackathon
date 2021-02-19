package org.byc.atomix.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class AtomixViewApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(AtomixViewApplication.class, args);

//		String[] beanNames = ctx.getBeanDefinitionNames();
//		Arrays.sort(beanNames);
//		for (String beanName : beanNames) {
//			System.out.println(beanName);
//		}
	}
}

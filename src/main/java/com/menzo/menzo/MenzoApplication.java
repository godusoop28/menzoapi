package com.menzo.menzo;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class MenzoApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(MenzoApplication.class);
		app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) MenzoApplication::migrateBeforeContextStarts);
		app.run(args);
	}

	/**
	 * Corre Flyway explícitamente contra el Environment ya resuelto (incluye overrides de
	 * variables de entorno como SPRING_DATASOURCE_URL) antes de crear el ApplicationContext.
	 * El auto-wiring de Spring Boot que hace correr Flyway antes de construir el
	 * EntityManagerFactory no estaba disparando ninguna migración en este stack (Hibernate
	 * fallaba validando un esquema vacío sin log de Flyway de por medio), así que forzamos
	 * el orden explícitamente en vez de depender de esa magia.
	 */
	private static void migrateBeforeContextStarts(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment env = event.getEnvironment();
		if (!env.getProperty("spring.flyway.enabled", Boolean.class, Boolean.TRUE)) {
			return;
		}
		Flyway.configure()
				.dataSource(
						env.getRequiredProperty("spring.datasource.url"),
						env.getProperty("spring.datasource.username"),
						env.getProperty("spring.datasource.password"))
				.locations(env.getProperty("spring.flyway.locations", "classpath:db/migration"))
				.baselineOnMigrate(env.getProperty("spring.flyway.baseline-on-migrate", Boolean.class, Boolean.FALSE))
				.load()
				.migrate();
	}

}

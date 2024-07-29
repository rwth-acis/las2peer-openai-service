package services.openAIService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OpenAIServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(OpenAIServiceApplication.class);

	public static void main(String[] args) {
		System.setProperty("server.servlet.context-path", "/openai");
		try {
			SpringApplication.run(OpenAIServiceApplication.class, args);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Application failed to start.", e);
		}
		
		logger.info("Application has started successfully.");	
	}

}

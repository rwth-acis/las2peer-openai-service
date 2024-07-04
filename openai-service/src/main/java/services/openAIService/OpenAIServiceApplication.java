package services.openAIService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OpenAIServiceApplication {

	public static void main(String[] args) {
		System.setProperty("server.servlet.context-path", "/openai");
		SpringApplication.run(OpenAIServiceApplication.class, args);
	}

}

package services.openAIService.config;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenAIServiceConfig {

    @Bean
    public OpenAPI openAIServiceconfig() {
        Server sv = new Server();
        sv.setUrl("http://localhost:8080");
        sv.setDescription("Development environment URL");
        Contact contact = new Contact();
        contact.setEmail("yue.yin@rwth-aachen.de, samuel.kwong@rwth-aachen.de");
        contact.setName("Yue Yin, Samuel Kwong");
        License lis = new License().name("CC0").url("https://github.com/rwth-acis/las2peer-openai-service/blob/main/LICENSE");
        Info info = new Info()
            .title("OpenAI Service")
            .version("2.0.0")
            .contact(contact)
            .description("A service to make request to OpenAI API functions and connect to the other services utilizing OpenAPI services.").termsOfService("https://www.bezkoder.com/terms")
            .license(lis);
        return new OpenAPI().info(info).servers(List.of(sv));
    }
}

package cn.suhoan.asihavewritten.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DockerLogAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockerLogAgentApplication.class, args);
    }
}

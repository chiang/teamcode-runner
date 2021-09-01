package io.teamcode.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by chiang on 2017. 4. 27..
 */
@ComponentScan("io.teamcode")
@EnableCaching
@EnableScheduling
@SpringBootApplication // same as @Configuration @EnableAutoConfiguration @ComponentScan
public class Runner {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(Runner.class);
        app.addListeners(new ApplicationPidFileWriter());
        app.run(args);
    }
}

package net.vivans.dcim.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "net.vivans.dcim")
@EntityScan(basePackages = "net.vivans.dcim.module")
@EnableScheduling
public class ManagerServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagerServerApplication.class, args);
    }

}

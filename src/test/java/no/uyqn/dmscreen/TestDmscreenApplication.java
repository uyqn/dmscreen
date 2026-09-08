package no.uyqn.dmscreen;

import org.springframework.boot.SpringApplication;

public class TestDmscreenApplication {

    public static void main(String[] args) {
        SpringApplication.from(Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}

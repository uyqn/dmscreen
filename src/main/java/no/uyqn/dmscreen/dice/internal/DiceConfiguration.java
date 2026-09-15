package no.uyqn.dmscreen.dice.internal;

import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DiceConfiguration {
    @Bean
    public RandomGenerator randomGenerator() {
        return RandomGenerator.of("L64X128MixRandom");
    }
}

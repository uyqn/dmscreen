package no.uyqn.dmscreen.dice.internal;

import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DiceConfiguration {
    public static final String RANDOM_ALGORITHM = "L64X128MixRandom";

    @Bean
    public RandomGenerator randomGenerator() {
        return RandomGenerator.of(RANDOM_ALGORITHM);
    }
}

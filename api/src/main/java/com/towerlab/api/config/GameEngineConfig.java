package com.towerlab.api.config;

import com.towerlab.engine.config.ConfigLoader;
import com.towerlab.engine.config.GameConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads GameConfig once at startup through ConfigLoader from the engine's classpath resources.
 */
@Configuration
public class GameEngineConfig {

    @Bean
    public GameConfig gameConfig() {
        ConfigLoader loader = new ConfigLoader();
        return loader.loadFromClasspath("");
    }
}

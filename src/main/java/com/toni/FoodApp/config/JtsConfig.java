package com.toni.FoodApp.config;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class JtsConfig {

    /**
     * Creates a GeometryFactory bean that is configured to use
     * SRID 4326 (WGS 84), which matches the database column.
     * This bean will be injected wherever I use @Autowired or
     * a final field in a @RequiredArgsConstructor class.
     */
    @Bean
    public GeometryFactory geometryFactory() {
        return new GeometryFactory(new PrecisionModel(), 4326);
    }
}

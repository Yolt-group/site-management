package nl.ing.lovebird.sitemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"nl.ing.lovebird.sitemanagement", "nl.ing.lovebird.sitemanagement.health"})
@EnableAsync
@EnableScheduling
@EnableCaching
public class SiteManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiteManagementApplication.class, args);  //NOSONAR (spring has auto connection close)
    }

}

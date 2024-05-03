package com.samourai.whirlpool.saas.config;

import com.samourai.whirlpool.cli.config.CliServicesConfig;
import com.samourai.whirlpool.cli.services.*;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@ComponentScan(
    value = "com.samourai.whirlpool.cli.services",
    excludeFilters = {
      // exclude unused CLI services
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WsNotifierService.class),
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WSMessageService.class),
    })
@EntityScan(value = "com.samourai.whirlpool.cli.persistence")
@EnableJpaRepositories(value = "com.samourai.whirlpool.cli.persistence")
@EnableScheduling
public class SaasServicesConfig extends CliServicesConfig {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public SaasServicesConfig() {
    super();
  }
}

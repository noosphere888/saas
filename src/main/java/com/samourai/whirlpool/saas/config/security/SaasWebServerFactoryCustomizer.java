package com.samourai.whirlpool.saas.config.security;

import com.samourai.whirlpool.cli.config.security.CliWebServerFactoryCustomizer;
import com.samourai.whirlpool.saas.config.SaasConfig;
import org.springframework.stereotype.Component;

/** Configure HTTPS server. */
@Component
public class SaasWebServerFactoryCustomizer extends CliWebServerFactoryCustomizer {

  public SaasWebServerFactoryCustomizer(SaasConfig saasConfig) {
    super(saasConfig);
  }
}

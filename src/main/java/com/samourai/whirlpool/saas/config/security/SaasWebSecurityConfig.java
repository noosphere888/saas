package com.samourai.whirlpool.saas.config.security;

import com.samourai.whirlpool.saas.config.SaasConfig;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableWebSecurity
public class SaasWebSecurityConfig extends WebSecurityConfigurerAdapter {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private SaasConfig saasConfig;

  @Autowired
  public SaasWebSecurityConfig(SaasConfig saasConfig) {
    this.saasConfig = saasConfig;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    boolean httpEnable = saasConfig.getApi().isHttpEnable();
    if (log.isDebugEnabled()) {
      log.debug("Configuring REST API: httpEnable=" + httpEnable);
    }

    String[] ENDPOINTS = new String[] {};

    // disable CSRF
    http.csrf()
        .disable()

        // allow endpoints
        .authorizeRequests()
        .antMatchers(ENDPOINTS)
        .permitAll()

        // reject others
        .anyRequest()
        .denyAll();

    if (!httpEnable) {
      http.requiresChannel().anyRequest().requiresSecure();
    }
  }
}

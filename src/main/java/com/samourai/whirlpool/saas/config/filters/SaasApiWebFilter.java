package com.samourai.whirlpool.saas.config.filters;

import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.config.filters.CliApiWebFilter;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

@WebFilter(CliApiEndpoint.REST_PREFIX + "*")
public class SaasApiWebFilter extends CliApiWebFilter {

  public SaasApiWebFilter() {
    super();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    super.doFilter(request, response, chain);
  }
}

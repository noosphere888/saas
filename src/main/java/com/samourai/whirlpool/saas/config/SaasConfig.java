package com.samourai.whirlpool.saas.config;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.persistence.repository.UtxoConfigRepository;
import com.samourai.whirlpool.cli.persistence.repository.WalletStateRepository;
import org.springframework.stereotype.Service;

@Service
public class SaasConfig extends CliConfig {

  public SaasConfig(
      UtxoConfigRepository utxoConfigRepository, WalletStateRepository walletStateRepository) {
    super(utxoConfigRepository, walletStateRepository);
  }
}

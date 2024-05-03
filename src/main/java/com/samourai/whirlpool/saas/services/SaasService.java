package com.samourai.whirlpool.saas.services;

import com.google.common.eventbus.Subscribe;
import com.samourai.wallet.cahoots.multi.MultiCahoots;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.services.*;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.event.WalletCloseEvent;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.saas.config.util.WhirlpoolUtil;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SaasService extends CliService implements SaasListener {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ArrayList<Long> recentStonewalls = new ArrayList<>();
  private HashMap<String, Integer> stonewallx2Credits = new HashMap<>();
  private CahootsListener cahootsListener;

  public SaasService(
      CliArgs appArgs,
      CliConfig cliConfig,
      CliConfigService cliConfigService,
      CliWalletService cliWalletService,
      CliUpgradeService cliUpgradeService,
      CliTorClientService cliTorClientService,
      JavaHttpClientService httpClientService,
      DbService dbService) {
    super(
        appArgs,
        cliConfig,
        cliConfigService,
        cliWalletService,
        cliUpgradeService,
        cliTorClientService,
        httpClientService,
        dbService);
    WhirlpoolEventService.getInstance().register(this);
  }

  @Override
  protected void startCliStatusOrchestrator() {
    // comment this to hide real-time mixing status
    super.startCliStatusOrchestrator();
  }

  @Override
  protected void onWalletReady(CliWallet cliWallet) throws Exception {
    // start whirlpool mixing
    super.onWalletReady(cliWallet);

    // listen for Cahoots
    cahootsListener = new CahootsListener(cliWallet, this);
    cahootsListener.start();
  }

  @Subscribe
  public void onWalletClose(WalletCloseEvent closeEvent) throws Exception {
    stopCahootsListener();
  }

  @Override
  public void shutdown() {
    super.shutdown();
    stopCahootsListener();
  }

  private void stopCahootsListener() {
    // stop listening for Cahoots
    if (cahootsListener != null) {
      cahootsListener.exit();
      cahootsListener = null;
    }
  }

  @Override
  public void onMultiCahootsCompletion(long revenueSatoshis, MultiCahoots multiCahoots) {
    // TODO dump to json file
    log.info(
        "Completed MultiCahoots. Revenue: "
            + Coin.valueOf(revenueSatoshis).toPlainString()
            + " BTC");
    log.info(
        "Transaction IDs: STOWAWAY>"
            + multiCahoots.getStowawayTransaction().getHashAsString()
            + ", STONEWALLx2>"
            + multiCahoots.getStonewallTransaction().getHashAsString());
  }

  @Override
  public void creditWithStonewallx2(String paymentCode) {
    int balance = this.stonewallx2Credits.get(paymentCode);
    balance++;
    this.stonewallx2Credits.put(paymentCode, balance);
    log.info(
        "Giving Stonewallx2 credit to "
            + paymentCode
            + ", remaining: "
            + this.stonewallx2Credits.get(paymentCode));
  }

  @Override
  public int getBalanceOfSender(String paymentCode) {
    try {
      return this.stonewallx2Credits.get(paymentCode);
    } catch (NullPointerException e) {
      this.stonewallx2Credits.put(paymentCode, 0);
      return 0;
    }
  }

  @Override
  public void onStonewallx2Completion(String senderCode, long amount) {
    deductStonewallx2Credit(senderCode);
    this.recentStonewalls.add(amount);
    if (this.recentStonewalls.size() > 50) {
      int difference = this.recentStonewalls.size() - 50;
      for (int i = 0; i < difference; i++) {
        this.recentStonewalls.remove(i);
      }
    }

    WhirlpoolUtil.WhirlpoolTier tier = WhirlpoolUtil.getOptiminalTier(this.recentStonewalls);
    log.info("Latest optimal tier: " + tier.getTier());
    try {
      this.cliWalletService.getSessionWallet().getConfig().setAutoTx0PoolId(tier.getTier());
    } catch (NoSessionWalletException e) {
      e.printStackTrace();
    }
  }

  public void deductStonewallx2Credit(String senderCode) {
    if (this.stonewallx2Credits.containsKey(senderCode)) {
      int balance = this.stonewallx2Credits.get(senderCode);
      if (balance != 0) {
        balance--;
        this.stonewallx2Credits.put(senderCode, balance);
        log.info(
            "Deducted Stonewallx2 credit from "
                + senderCode
                + ", remaining: "
                + this.stonewallx2Credits.get(senderCode));
      }
    }
  }
}

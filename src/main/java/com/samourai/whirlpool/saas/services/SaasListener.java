package com.samourai.whirlpool.saas.services;

import com.samourai.wallet.cahoots.multi.MultiCahoots;

public interface SaasListener {
  public void onMultiCahootsCompletion(long revenueSatoshis, MultiCahoots multiCahoots);

  public void creditWithStonewallx2(String paymentCode);

  public int getBalanceOfSender(String paymentCode);

  public void onStonewallx2Completion(String senderCode, long amount);
}

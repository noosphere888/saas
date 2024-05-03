package com.samourai.whirlpool.saas.config.util;

import java.util.Collections;
import java.util.List;

public class WhirlpoolUtil {
  public enum WhirlpoolTier {
    mbtc("0.001btc"),
    mbtc10("0.01btc"),
    mbtc50("0.05btc"),
    half("0.5btc");

    private final String tier;

    WhirlpoolTier(String tier) {
      this.tier = tier;
    }

    public String getTier() {
      return tier;
    }
  }

  public static WhirlpoolTier getOptiminalTier(List<Long> previousAmounts) {
    if (previousAmounts.isEmpty()) {
      return WhirlpoolTier.mbtc10; // Default until enough Stonewall data is gathered.
    }

    Collections.sort(previousAmounts);

    long middle;
    if (previousAmounts.size() % 2 == 0) {
      middle =
          (previousAmounts.get(previousAmounts.size() / 2)
                  + previousAmounts.get(previousAmounts.size() / 2 - 1))
              / 2;
    } else {
      middle = previousAmounts.get(previousAmounts.size() / 2);
    }

    if (middle >= 50000000) return WhirlpoolTier.half;
    else if (middle >= 5000000) return WhirlpoolTier.mbtc50;
    else if (middle >= 1000000) return WhirlpoolTier.mbtc10;
    else return WhirlpoolTier.mbtc;
  }
}

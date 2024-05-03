package com.samourai.whirlpool.saas.services;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.cahoots.SorobanCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsUtxo;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.multi.MultiCahoots;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.saas.util.FeeUtil;
import com.samourai.whirlpool.saas.util.LogFormatter;
import com.samourai.xmanager.client.XManagerClient;
import io.reactivex.Observable;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.Provider;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.FileHandler;


import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CahootsListener {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Provider PROVIDER = new BouncyCastleProvider();

  private SaasListener saasListener;
  private CahootsWallet cahootsWallet;
  private RpcClient rpcClient;
  private SorobanCahootsService sorobanCahootsService;
  private SorobanMeetingService sorobanMeetingService;
  private boolean started;
  private java.util.logging.Logger cahootsLogger = null;

  public CahootsListener(WhirlpoolWallet whirlpoolWallet, SaasListener saasListener)
      throws Exception {
      this.cahootsLogger = initCahootsLogger();
    this.saasListener = saasListener;
    WhirlpoolWalletConfig config = whirlpoolWallet.getConfig();
    NetworkParameters params = config.getNetworkParameters();
    this.cahootsWallet =
        new CahootsWallet(whirlpoolWallet.getWalletSupplier(), BIP_FORMAT.PROVIDER, params) {
          @Override
          public long fetchFeePerB() {
            long feePerB = FeeUtil.getInstance().getSuggestedFeeDefaultPerB();
            return feePerB;
          }

          @Override
          protected List<CahootsUtxo> fetchUtxos(int account) {
            WhirlpoolAccount whirlpoolAccount = WhirlpoolAccount.POSTMIX;
            Collection<WhirlpoolUtxo> utxos =
                whirlpoolWallet.getUtxoSupplier().findUtxos(whirlpoolAccount);

            List<CahootsUtxo> cahootsUtxos = new LinkedList<>();
            for (WhirlpoolUtxo utxo : utxos) {
              try {
                MyTransactionOutPoint outpoint = utxo.getUtxo().computeOutpoint(params);
                ECKey ecKey =
                    ECKey.fromPrivate(
                        whirlpoolWallet
                            .getUtxoSupplier()
                            ._getPrivKey(utxo.getUtxo().tx_hash, utxo.getUtxo().tx_output_n));
                String path = utxo.getUtxo().getPath();
                if (path != null && ecKey != null) {
                  CahootsUtxo cahootsUtxo = new CahootsUtxo(outpoint, path, ecKey);
                  cahootsUtxos.add(cahootsUtxo);
                }
              } catch (Exception e) {
                log.error("", e);
              }
            }
            return cahootsUtxos;
          }
        };
    log.info(cahootsWallet.getBip47Wallet().getAccount(0).getPaymentCode());
    log.info(whirlpoolWallet.getDepositAddress(false));
    // test
    XManagerClient xManagerClient = whirlpoolWallet.getConfig().computeXManagerClient();
    OnlineCahootsService onlineCahootsService =
        new OnlineCahootsService(cahootsWallet, xManagerClient);
    BIP47Wallet bip47Wallet = cahootsWallet.getBip47Wallet();
    IHttpClient httpClient = new JavaHttpClient(20000, Optional.empty(), HttpUsage.BACKEND);
    this.rpcClient = new RpcClient(httpClient, false, params);
    this.sorobanMeetingService =
        new SorobanMeetingService(
            Bip47UtilJava.getInstance(), params, PROVIDER, bip47Wallet, 0, rpcClient);
    SorobanService sorobanService =
        new SorobanService(
            Bip47UtilJava.getInstance(), params, PROVIDER, bip47Wallet, 0, rpcClient);
    this.sorobanCahootsService =
        new SorobanCahootsService(onlineCahootsService, sorobanService, sorobanMeetingService);
  }

  public void start() {
    new Thread(
            () -> {
              this.started = true;
              log.info("Listening for Cahoots...");
              while (started) {
                try {
                  sorobanMeetingService
                      .receiveMeetingRequest(20000)
                      .subscribe(
                          sorobanRequestMessage -> {
                            PaymentCode senderPaymentCode =
                                new PaymentCode(sorobanRequestMessage.getSender());
                            log.error("SENDER:: " + senderPaymentCode);
                            int credits =
                                saasListener.getBalanceOfSender(senderPaymentCode.toString());
                            if (sorobanRequestMessage.getType() == CahootsType.STONEWALLX2
                                && credits > 0) {
                              log.info("Accepting Stonewallx2 Cahoots");
                              acceptSoroban(senderPaymentCode, sorobanRequestMessage);
                            } else if (sorobanRequestMessage.getType() == CahootsType.STOWAWAY) {
                              log.info("Accepting Stowaway Cahoots");
                              acceptSoroban(senderPaymentCode, sorobanRequestMessage);
                            } else if (sorobanRequestMessage.getType() == CahootsType.MULTI) {
                              log.info("Accepting Multi Cahoots");
                              acceptSoroban(senderPaymentCode, sorobanRequestMessage);
                            } else {
                              declineSoroban(senderPaymentCode, sorobanRequestMessage);
                            }
                          },
                          error -> {
                            if (!(error instanceof TimeoutException)) {
                              log.error("ERROR! " + error);
                            }
                          });
                } catch (Exception e) {
                  log.error("", e);
                }
              }
            })
        .start();
  }

  public void exit() {
    started = false;
    rpcClient.exit();
  }

  private void acceptSoroban(PaymentCode senderPaymentCode, SorobanRequestMessage cahootsRequest)
      throws Exception {
    sorobanMeetingService
        .sendMeetingResponse(senderPaymentCode, cahootsRequest, true)
        .subscribe(
            sorobanResponseMessage -> {
              boolean accept = sorobanResponseMessage.isAccept();
              log.info("Accepted: " + accept);
              if (accept) {
                log.info("Beginning cahoots...");
                startListeningForCahootsMessages(cahootsRequest);
              }
            });
  }

  private void declineSoroban(PaymentCode senderPaymentCode, SorobanRequestMessage cahootsRequest)
      throws Exception {
    sorobanMeetingService
        .sendMeetingResponse(senderPaymentCode, cahootsRequest, false)
        .subscribe(
            sorobanResponseMessage -> {
              boolean accept = sorobanResponseMessage.isAccept();
              log.info("Accepted: " + accept);
              if (!accept) {
                log.info("Declining cahoots...");
              }
            });
  }

  private void startListeningForCahootsMessages(SorobanRequestMessage cahootsRequest)
      throws Exception {
    int accountIndex = SamouraiAccountIndex.POSTMIX;
    CahootsContext cahootsContext =
        CahootsContext.newCounterparty(cahootsRequest.getType(), accountIndex);
    Observable<SorobanMessage> sorobanListener =
        sorobanCahootsService.contributor(
            cahootsContext, new PaymentCode(cahootsRequest.getSender()), 20000);
    sorobanListener.subscribe(
        sorobanMessage -> {
          OnlineCahootsMessage cahootsMessage = (OnlineCahootsMessage) sorobanMessage;
          if (sorobanMessage.isDone()) {
            Cahoots cahoots = cahootsMessage.getCahoots();
            if (cahootsRequest.getType() == CahootsType.MULTI) {
                long timeMs = System.currentTimeMillis();
              MultiCahoots multiCahoots = (MultiCahoots) cahoots;
              String stowawayTxid = multiCahoots.getStowawayTransaction().getHashAsString();
              String stonewallTxid = multiCahoots.getStonewallTransaction().getHashAsString();
              long ourFee = multiCahoots.getStonewallx2().getFeeAmount() / 2;
              long stowawayRevenue = multiCahoots.getStowaway().getSpendAmount();
              long actualRevenue = stowawayRevenue - ourFee;
              log.info("Total revenue from MultiCahoots: " + actualRevenue);
              log.info("ourFee: " + ourFee + "\nstowawayRevenue: " + stowawayRevenue);
              log.info("MultiCahoots completed.");
              cahootsLogger.info(timeMs + " | " + stowawayTxid + " | " + stonewallTxid + " | " + actualRevenue + " | " + stowawayRevenue + " | " + ourFee);
              saasListener.onMultiCahootsCompletion(
                  actualRevenue, (MultiCahoots) cahootsMessage.getCahoots());
            } else if (cahootsRequest.getType() == CahootsType.STOWAWAY) {
              saasListener.creditWithStonewallx2(cahootsRequest.getSender());
            } else {
              log.info(
                  "Stonewallx2 completed: "
                      + cahoots.getSpendAmount()
                      + " "
                      + cahootsRequest.getSender());
              saasListener.onStonewallx2Completion(
                  cahootsRequest.getSender(), cahoots.getSpendAmount());
            }
          }
        },
        error -> {});
  }

    private static java.util.logging.Logger initCahootsLogger() {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MULTICAHOOTS");
        try {
            FileHandler fh = new FileHandler("./cahoots.log", true);
            logger.addHandler(fh);
            LogFormatter formatter = new LogFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException| IOException e) {
            e.printStackTrace();
        }
        return logger;
    }
}

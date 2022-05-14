package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.BackendPushTxResponse;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipFormat.BipFormatSupplierImpl;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.client.indexHandler.IndexHandlerSupplier;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.JSONUtils;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.dataPersister.DataPersister;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSource;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceWithStrictMode;
import com.samourai.whirlpool.client.wallet.data.minerFee.MinerFeeSupplier;
import com.samourai.whirlpool.client.wallet.data.paynym.PaynymSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.ExpirablePoolSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;
import com.samourai.whirlpool.protocol.rest.PushTxSuccessResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AndroidDataSource implements DataSource, DataSourceWithStrictMode {
    private PushTx pushTx;

    protected WalletSupplier walletSupplier;
    protected MinerFeeSupplier minerFeeSupplier;
    protected ChainSupplier chainSupplier;
    protected Tx0PreviewService tx0ParamService;
    protected ExpirablePoolSupplier poolSupplier;
    protected UtxoSupplier utxoSupplier;

    public AndroidDataSource(WhirlpoolWallet whirlpoolWallet, HD_Wallet bip44w, PushTx pushTx, FeeUtil feeUtil, APIFactory apiFactory, UTXOFactory utxoFactory, BIP47Util bip47Util, BIP47Meta bip47Meta) throws Exception {
        this.pushTx = pushTx;
        WalletStateSupplier walletStateSupplier = whirlpoolWallet.getWalletStateSupplier();
        BipFormatSupplier bipFormatSupplier = new BipFormatSupplierImpl();
        this.walletSupplier = new WalletSupplierImpl(walletStateSupplier, bip44w);
        this.minerFeeSupplier = computeMinerFeeSupplier(feeUtil);
        this.chainSupplier = new AndroidChainSupplier(apiFactory);
        this.tx0ParamService = new Tx0PreviewService(minerFeeSupplier, whirlpoolWallet.getConfig());
        this.poolSupplier = new ExpirablePoolSupplier(whirlpoolWallet.getConfig().getRefreshPoolsDelay(), whirlpoolWallet.getConfig().getServerApi(), tx0ParamService);
        this.utxoSupplier = new AndroidUtxoSupplier(walletSupplier, whirlpoolWallet.getUtxoConfigSupplier(), chainSupplier, poolSupplier, bipFormatSupplier, utxoFactory, bip47Util, bip47Meta);
    }

    protected MinerFeeSupplier computeMinerFeeSupplier(FeeUtil feeUtil) {
        return new AndroidMinerFeeSupplier(feeUtil);
    }

    @Override
    public void open() throws Exception {
        // load pools (or fail)
        poolSupplier.load();
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public String pushTx(String s) throws Exception {
        return pushTx.samourai(s, new ArrayList<>());
    }

    @Override
    public WalletSupplier getWalletSupplier() {
        return walletSupplier;
    }

    @Override
    public UtxoSupplier getUtxoSupplier() {
        return utxoSupplier;
    }

    @Override
    public MinerFeeSupplier getMinerFeeSupplier() {
        return minerFeeSupplier;
    }

    @Override
    public ChainSupplier getChainSupplier() {
        return chainSupplier;
    }

    @Override
    public PoolSupplier getPoolSupplier() {
        return poolSupplier;
    }

    @Override
    public PaynymSupplier getPaynymSupplier() {
        return null;
    }

    @Override
    public Tx0PreviewService getTx0PreviewService() {
        return tx0ParamService;
    }

    @Override
    public String pushTx(String txHex, Collection<Integer> collection) throws Exception {
        List<Integer> strictModeVouts = new ArrayList<>(collection);
        String response = pushTx.samourai(txHex, strictModeVouts);

        // check strict-mode response
        BackendPushTxResponse pushTxResponse = JSONUtils.getInstance().getObjectMapper().readValue(response, BackendPushTxResponse.class);
        BackendApi.checkPushTxResponse(pushTxResponse);
        return response;
    }
}

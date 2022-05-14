package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipWallet.BipDerivation;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.client.indexHandler.AddressFactoryWalletStateIndexHandler;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.Chain;
import com.samourai.wallet.hd.WALLET_INDEX;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.data.walletState.WalletStateSupplier;

import java.util.LinkedHashMap;
import java.util.Map;

public class AndroidWalletStateSupplier implements WalletStateSupplier {
    private AddressFactory addressFactory;
    private Map<String, IIndexHandler> indexHandlerWallets;

    public AndroidWalletStateSupplier(AddressFactory addressFactory) {
        this.addressFactory = addressFactory;
        this.indexHandlerWallets = new LinkedHashMap<String, IIndexHandler>();
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void setInitialized(boolean b) {
        // ignored
    }

    @Override
    public boolean isNymClaimed() {
        return false;
    }

    @Override
    public void setNymClaimed(boolean b) {

    }

    @Override
    public IIndexHandler getIndexHandlerExternal() {
        // ignored
        return null;
    }

    @Override
    public IIndexHandler getIndexHandlerWallet(BipWallet bipWallet, Chain chain) {
        String persistKey = computePersistKeyWallet(bipWallet.getAccount(), bipWallet.getDerivation(), chain);
        IIndexHandler indexHandlerWallet = indexHandlerWallets.get(persistKey);
        if (indexHandlerWallet == null) {
            WALLET_INDEX walletIndex = WALLET_INDEX.find(bipWallet.getDerivation(), chain);
            indexHandlerWallet = new AddressFactoryWalletStateIndexHandler(addressFactory, walletIndex);
            indexHandlerWallets.put(persistKey, indexHandlerWallet);
        }
        return indexHandlerWallet;
    }

    protected String computePersistKeyWallet(WhirlpoolAccount account, BipDerivation bipDerivation, Chain chain) {
        return account.name() + "_" + bipDerivation.getPurpose() + "_" + chain.getIndex();
    }

    @Override
    public void load() throws Exception {
        // ignored
    }

    @Override
    public boolean persist(boolean b) throws Exception {
        // ignored
        return false;
    }

   /* @Override
    public IIndexHandler getIndexHandlerWallet(WhirlpoolAccount account, AddressType addressType, Chain chain) {
        String persistKey = computePersistKeyWallet(account, addressType, chain);
        IIndexHandler indexHandlerWallet = indexHandlerWallets.get(persistKey);
        if (indexHandlerWallet == null) {
            WALLET_INDEX walletIndex = WALLET_INDEX.find(account, addressType, chain);
            indexHandlerWallet = new AddressFactoryWalletStateIndexHandler(addressFactory, walletIndex);
            indexHandlerWallets.put(persistKey, indexHandlerWallet);
        }
        return indexHandlerWallet;
    }

    protected String computePersistKeyWallet(
            WhirlpoolAccount account, AddressType addressType, Chain chain) {
        return account.name() + "_" + addressType.getPurpose() + "_" + chain.getIndex();
    }

    @Override
    public IIndexHandler getIndexHandlerExternal() {
        // ignored
        return null;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void setInitialized(boolean initialized) {
        // ignored
    }

    @Override
    public void load() throws Exception {
        // ignored
    }

    @Override
    public boolean persist(boolean force) throws Exception {
        // ignored
        return false;
    }*/
}

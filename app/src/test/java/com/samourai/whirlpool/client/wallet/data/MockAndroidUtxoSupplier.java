package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.send.provider.SimpleUtxoKeyProvider;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionOutPoint;

public class MockAndroidUtxoSupplier extends AndroidUtxoSupplier {
    private SimpleUtxoKeyProvider utxoKeyProvider;

    public MockAndroidUtxoSupplier(WalletSupplier walletSupplier,
                                   UtxoConfigSupplier utxoConfigSupplier,
                                   ChainSupplier chainSupplier,
                                   PoolSupplier poolSupplier,
                                   BipFormatSupplier bipFormatSupplier,
                                   UTXOFactory utxoFactory,
                                   BIP47Util bip47Util,
                                   BIP47Meta bip47Meta) throws Exception {
        super(walletSupplier, utxoConfigSupplier, chainSupplier, poolSupplier, bipFormatSupplier, utxoFactory, bip47Util, bip47Meta);
        this.utxoKeyProvider = new SimpleUtxoKeyProvider();
    }

    @Override
    public byte[] _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
        return utxoKeyProvider._getPrivKey(utxoHash, utxoIndex);
    }

    public void setKey(TransactionOutPoint outPoint, ECKey ecKey) {
        utxoKeyProvider.setKey(outPoint, ecKey);
    }
}

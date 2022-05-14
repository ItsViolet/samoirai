package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.whirlpool.client.tx0.Tx0PreviewService;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.BasicUtxoSupplier;
import com.samourai.whirlpool.client.wallet.data.utxo.UtxoData;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;

import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class AndroidUtxoSupplier extends BasicUtxoSupplier {
    private Logger log = LoggerFactory.getLogger(AndroidUtxoSupplier.class);

    private UTXOFactory utxoFactory;
    private BIP47Util bip47Util;
    private BIP47Meta bip47Meta;
    private long lastUpdate;

    public AndroidUtxoSupplier(WalletSupplier walletSupplier,
                               UtxoConfigSupplier utxoConfigSupplier,
                               ChainSupplier chainSupplier,
                               PoolSupplier poolSupplier,
                               BipFormatSupplier bipFormatSupplier,
                               UTXOFactory utxoFactory,
                               BIP47Util bip47Util,
                               BIP47Meta bip47Meta) throws Exception {
        super(walletSupplier, utxoConfigSupplier, chainSupplier, poolSupplier, bipFormatSupplier);
        this.utxoFactory = utxoFactory;
        this.bip47Util = bip47Util;
        this.bip47Meta = bip47Meta;
        this.lastUpdate = -1;
    }

    @Override
    public UtxoData getValue() {
        UtxoData value = super.getValue();
        if (value == null || lastUpdate < utxoFactory.getLastUpdate()) {
            // fetch value
            value = computeValue();

            // set
            try {
                setValue(value);
                lastUpdate = System.currentTimeMillis();
            } catch (Exception e) {
                log.error("utxoSupplier.setValue failed!");
            }
        }
        return value;
    }

    @Override
    public void refresh() {
        this.lastUpdate = 0;
    }

    private UtxoData computeValue() {
        if (log.isDebugEnabled()) {
            log.debug("utxoSupplier.computeValue()");
        }
        List<UnspentOutput> utxos = new LinkedList();
        utxos.addAll(toUnspentOutputs(utxoFactory.getP2PKH().values(), WhirlpoolAccount.DEPOSIT, BIP_FORMAT.LEGACY));
        utxos.addAll(toUnspentOutputs(utxoFactory.getP2SH_P2WPKH().values(), WhirlpoolAccount.DEPOSIT, BIP_FORMAT.SEGWIT_COMPAT));
        utxos.addAll(toUnspentOutputs(utxoFactory.getP2WPKH().values(), WhirlpoolAccount.DEPOSIT, BIP_FORMAT.SEGWIT_NATIVE));
        utxos.addAll(toUnspentOutputs(utxoFactory.getPreMix().values(), WhirlpoolAccount.PREMIX, BIP_FORMAT.SEGWIT_NATIVE));
        utxos.addAll(toUnspentOutputs(utxoFactory.getAllPostMix().values(), WhirlpoolAccount.POSTMIX, BIP_FORMAT.SEGWIT_NATIVE));

        UnspentOutput[] utxosArray = utxos.toArray(new UnspentOutput[]{});
        WalletResponse.Tx[] txs = new WalletResponse.Tx[]{}; // ignored
        return new UtxoData(utxosArray, txs);
    }

    private Collection<UnspentOutput> toUnspentOutputs(Collection<UTXO> utxos, WhirlpoolAccount whirlpoolAccount, BipFormat addressType) {
        List<UnspentOutput> unspentOutputs = new LinkedList<>();

        BipWallet bipWallet = getWalletSupplier().getWallet(whirlpoolAccount, addressType);
        if (bipWallet == null) {
            log.error("Wallet not found for "+whirlpoolAccount+"/"+addressType);
            return unspentOutputs;
        }
        String xpub = bipWallet.getPub();
        for (UTXO utxo : utxos) {
            Collection<UnspentOutput> unspents = utxo.toUnspentOutputs(xpub);
            unspentOutputs.addAll(unspents);
        }
        if (log.isDebugEnabled()) {
            log.debug("set utxos["+whirlpoolAccount+"]["+addressType+"] = "+utxos.size()+" UTXO = "+unspentOutputs.size()+" unspentOutputs");
        }
        return unspentOutputs;
    }

    @Override
    public byte[] _getPrivKeyBip47(WhirlpoolUtxo whirlpoolUtxo) throws Exception {
        String address = whirlpoolUtxo.getUtxo().addr;
        String pcode = bip47Meta.getPCode4Addr(address);
        int idx = bip47Meta.getIdx4Addr(address);
        if (log.isDebugEnabled()) {
            log.debug("_getPrivKeyBip47: pcode="+pcode+", idx="+idx);
        }
        PaymentAddress addr = bip47Util.getReceiveAddress(new PaymentCode(pcode), idx);
        return addr.getReceiveECKey().getPrivKeyBytes();
    }
}

package com.samourai.wallet.utxos.models;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormat;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import org.bitcoinj.core.NetworkParameters;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * UTXO model for UI
 * since there is already a UTXO class exist, {@link UTXOCoin} class is mainly used for RecyclerView
 * UTXOCoin support extra UI related states like isSelected doNotSpend etc..
 */
public class UTXOCoin {
    public String address = "";
    public int id;
    public int account = 0;
    public long amount = 0L;
    public String hash = "";
    public String path = "";
    public String xpub = "";
    public int idx = 0;
    public boolean isSelected = false;
    private MyTransactionOutPoint outPoint;

    public MyTransactionOutPoint getOutPoint() {
        return outPoint;
    }


    public UTXOCoin(MyTransactionOutPoint outPoint, UTXO utxo, int account) {
        if (outPoint == null || utxo == null) {
            return;
        }
        this.outPoint = outPoint;
        this.address = outPoint.getAddress();
        this.path = utxo.getPath() == null ? "" : utxo.getPath();
        this.xpub = utxo.getXpub() == null ? "" : utxo.getXpub();
        this.amount = outPoint.getValue().longValue();
        this.hash = outPoint.getTxHash().toString();
        this.idx = outPoint.getTxOutputN();
        this.account = account;
    }

    public boolean isBlocked(){
        return BlockedUTXO.getInstance().containsAny(this.hash,this.idx);
    }

    public UnspentOutput toUnspentOutput() {
        return new UnspentOutput(outPoint, path, xpub);
    }

    public UTXO toUTXO() {
        List<MyTransactionOutPoint> outs = new ArrayList<>();
        outs.add(outPoint);
        return new UTXO(outs, path, xpub);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof UTXOCoin) {
            UTXOCoin coin = ((UTXOCoin) obj);
            return (
                    this.idx == coin.idx &&
                    this.amount == coin.amount &&
                    this.hash.equals(coin.hash) &&
                    this.address.equals(coin.address) &&
                    this.isBlocked() == coin.isBlocked() &&
                    this.account == coin.account);
        }
        return super.equals(obj);
    }
}



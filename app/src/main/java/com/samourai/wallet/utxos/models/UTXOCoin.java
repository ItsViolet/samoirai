package com.samourai.wallet.utxos.models;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;

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
        this.amount = outPoint.getValue().longValue();
        this.hash = outPoint.getTxHash().toString();
        this.idx = outPoint.getTxOutputN();
        this.account = account;
    }

    public boolean isBlocked(){
        return BlockedUTXO.getInstance().containsAny(this.hash,this.idx);
    }

    public UnspentOutput toUnspentOutput() {
        String pubkey = null; // TODO
        String xpub = AddressFactory.getInstance().account2xpub().get(account);
        return new UnspentOutput(outPoint, pubkey, path, xpub);
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



package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.stomp.client.AndroidStompClient;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.provider.CahootsUtxoProvider;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class AndroidCahootsUtxoProvider implements CahootsUtxoProvider {
    private Logger log = LoggerFactory.getLogger(LoggerFactory.class);
    private static AndroidCahootsUtxoProvider instance = null;

    private APIFactory apiFactory;

    public static AndroidCahootsUtxoProvider getInstance(Context ctx) {
        if (instance == null) {
            instance = new AndroidCahootsUtxoProvider(ctx);
        }
        return instance;
    }

    private AndroidCahootsUtxoProvider(Context ctx) {
        this.apiFactory = APIFactory.getInstance(ctx);
    }

    @Override
    public List<CahootsUtxo> getUtxosWpkhByAccount(int account) {
        // fetch utxos
        List<UTXO> apiUtxos;
        if(account == SamouraiAccountIndex.POSTMIX)    {
            apiUtxos = apiFactory.getUtxosPostMix(true);
        }
        else    {
            apiUtxos = apiFactory.getUtxos(true);
        }

        // filter WPKH
        apiUtxos = UTXO.filterUtxosWpkh(apiUtxos);

        // convert to CahootsUtxo
        List<CahootsUtxo> utxos = new LinkedList<>();
        for(UTXO utxo : apiUtxos)   {
            MyTransactionOutPoint outpoint = utxo.getOutpoints().get(0);
            String path = utxo.getPath();
            if(path != null)   {
                String address = outpoint.getAddress();
                byte[] ecKey = SendFactory.getPrivKey(address, account).getPrivKeyBytes();
                String xpub = utxo.getXpub();
                CahootsUtxo cahootsUtxo = new CahootsUtxo(outpoint, path, xpub, ecKey);
                utxos.add(cahootsUtxo);
            } else {
                log.error("Skipping UTXO (path is null): "+outpoint.toString());
            }
        }
        return utxos;
    }
}

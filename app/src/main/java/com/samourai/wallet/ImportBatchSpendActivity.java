package com.samourai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.whirlpool.WhirlpoolConst;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Transaction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class ImportBatchSpendActivity extends SamouraiActivity {

    private EditText edBatch = null;
    private Button btOK = null;
    private Button btCancel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_batch_spend);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        edBatch = (EditText)findViewById(R.id.batch);
        StringBuilder stringBuilder = new StringBuilder();

        btOK = (Button)findViewById(R.id.ok);
        btOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String text = edBatch.getText().toString();
//                String text = "C++\nJava\nKotlin";
//                String[] lines = text.split("\r?\n|\r");

//                { "batch": [{ "dest": "+holysnow471", "amt": 17000 }, { "dest": "PM8TJQpMzANwsqTFDwsf2ZK9fmqabrmH9Vk6ocTvTJoaAsqTGYRGMu6DzMDKArdyynHNGuJt2sxXk4xGwZZyYXqjLgFTJ8Y7kJu74a32yLVmBje7sbzP", "amt": 200000 }, { "dest": "mpQcGEbsZXoCWv464TyaC6KdKa8tvB3y6W", "amt": 3267 }, { "dest": "2My2UzTAR2ehZjDger7rypkKtymRxC1VKo1", "amt": 234532 }, { "dest": "tb1qjqdj7txr6d9t54kh496kysc3v7kyawxs5fez46", "amt": 200000 }, { "dest": "tb1pme503kta7rdpm5zctx4u7ystrt4c9463hk9r7zvw8cfr9ay7n6rq3xm626", "amt": 134782 }, { "dest": "tb1qr2rn7s7urcptk5d6mfwec5n9q4xtd5075ug3w2qy2q98pdmfna7q0vvx5p", "amt": 8209 } ], "account": 0, "fee": 1 }
//                String text = "{ \"batch\": [{ \"dest\": \"+holysnow471\", \"amt\": 17000 }, { \"dest\": \"PM8TJQpMzANwsqTFDwsf2ZK9fmqabrmH9Vk6ocTvTJoaAsqTGYRGMu6DzMDKArdyynHNGuJt2sxXk4xGwZZyYXqjLgFTJ8Y7kJu74a32yLVmBje7sbzP\", \"amt\": 200000 }, { \"dest\": \"mpQcGEbsZXoCWv464TyaC6KdKa8tvB3y6W\", \"amt\": 3267 }, { \"dest\": \"2My2UzTAR2ehZjDger7rypkKtymRxC1VKo1\", \"amt\": 234532 }, { \"dest\": \"tb1qjqdj7txr6d9t54kh496kysc3v7kyawxs5fez46\", \"amt\": 200000 }, { \"dest\": \"tb1pme503kta7rdpm5zctx4u7ystrt4c9463hk9r7zvw8cfr9ay7n6rq3xm626\", \"amt\": 134782 }, { \"dest\": \"tb1qr2rn7s7urcptk5d6mfwec5n9q4xtd5075ug3w2qy2q98pdmfna7q0vvx5p\", \"amt\": 8209 } ], \"account\": 0, \"fee\": 1 }";

                int account = 0;
                int fee = 0;
                long totalAmount = 0L;
                HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();

                try {

                    JSONObject obj = new JSONObject(text);
                    if(obj.has("batch")) {

                        if(obj.has("account")) {
                            System.out.println("account:" + obj.getInt("account"));
                            account = obj.getInt("account");
                        }
                        if(obj.has("fee") && obj.getInt("fee") > 0) {
                            System.out.println("fee:" + obj.getInt("fee"));
                            fee = obj.getInt("fee");
                        }

                        JSONArray array = obj.getJSONArray("batch");
                        for(int i = 0; i < array.length(); i++) {
                            JSONObject dest = (JSONObject) array.get(i);
                            String strDestination = null;
                            Long amount = 0L;
                            if(dest.has("dest")) {
                                System.out.println("dest:" + dest.getString("dest"));
                                strDestination = dest.getString("dest");
                            }
                            if(dest.has("amt")) {
                                System.out.println("amt:" + dest.getLong("amt"));
                                amount = dest.getLong("amt");
                            }

                            if(FormatsUtil.getInstance().isValidPaymentCode(strDestination) && BIP47Meta.getInstance().getOutgoingStatus(strDestination) == BIP47Meta.STATUS_SENT_CFM) {
                                System.out.println("payment code:" + strDestination);

                                int index = BIP47Meta.getInstance().getOutgoingIdx(strDestination);
                                PaymentAddress strAddress = BIP47Util.getInstance(ImportBatchSpendActivity.this).getSendAddress(new PaymentCode(strDestination), index);
                                BIP47Meta.getInstance().incOutgoingIdx(strDestination);

                                stringBuilder.append(strDestination + ":" + strAddress.getSegwitAddressSend().getBech32AsString() + " (index " + index + "), " + amount + "\n");

                                strDestination = strAddress.getSegwitAddressSend().getBech32AsString();
                            }
                            else if(FormatsUtil.getInstance().isValidBitcoinAddress(strDestination)) {
                                System.out.println("address:" + strDestination);

                                stringBuilder.append(strDestination + ":" + amount  + "\n");

                            }
                            else if(strDestination.startsWith("+")) {
                                System.out.println("paynym:" + strDestination);
                                continue;
                            }
                            else {
                                ;
                            }

                            totalAmount += amount;

                            if(receivers.containsKey(strDestination)) {
                                BigInteger _amount = receivers.get(strDestination);
                                _amount = BigInteger.valueOf(_amount.longValue() + amount);
                                receivers.put(strDestination, _amount);
                            }
                            else {
                                receivers.put(strDestination, BigInteger.valueOf(amount));
                            }

                        }
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                stringBuilder.append("spend amount:" + totalAmount  + "\n");

                List<UTXO> utxos = null;
                if (account == 0) {
                    utxos = APIFactory.getInstance(ImportBatchSpendActivity.this).getUtxos(true);
                }
                else if (account == WhirlpoolConst.WHIRLPOOL_POSTMIX_ACCOUNT) {
                    utxos = APIFactory.getInstance(ImportBatchSpendActivity.this).getUtxosPostMix(true);
                }

                Collections.sort(utxos, new UTXO.UTXOComparator());
                long totalValueSelected = 0L;
                List<MyTransactionOutPoint> outpoints = new ArrayList<MyTransactionOutPoint>();
                BigInteger bFee = BigInteger.valueOf(0L);
                for(UTXO utxo : utxos) {
                    outpoints.addAll(utxo.getOutpoints());
                    totalValueSelected += utxo.getValue();

                    Vector<MyTransactionOutPoint> _outpoints = new Vector<MyTransactionOutPoint>(outpoints);
                    Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(_outpoints, SamouraiWallet.getInstance().getCurrentNetworkParams());
                    int p2tr_p2wsh = FeeUtil.getInstance().getOutpointCountP2TR_P2WSH(_outpoints);
                    BigInteger rFee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getMiddle(), outpointTypes.getRight(), (receivers.size() - p2tr_p2wsh) + 2, p2tr_p2wsh);

                    if(totalValueSelected + rFee.longValue() > totalAmount) {
                        bFee = rFee;
                        break;
                    }
                }
                stringBuilder.append("selected amount:" + totalValueSelected + "\n");
                stringBuilder.append("calculated fee:" + bFee.longValue() + "\n");

                long changeAmount = totalValueSelected - (totalAmount + bFee.longValue());
                int change_idx = 0;
                if (changeAmount > 0L) {
                    change_idx = BIP84Util.getInstance(ImportBatchSpendActivity.this).getWallet().getAccount(0).getChange().getAddrIdx();
                    String change_address = BIP84Util.getInstance(ImportBatchSpendActivity.this).getAddressAt(AddressFactory.CHANGE_CHAIN, change_idx).getBech32AsString();
                    receivers.put(change_address, BigInteger.valueOf(changeAmount));
                    stringBuilder.append("change:" + change_address + ", " + changeAmount + "\n");
                }
                else {
                    Toast.makeText(ImportBatchSpendActivity.this, R.string.error_change_output, Toast.LENGTH_SHORT).show();
                    return;
                }

                Transaction tx = SendFactory.getInstance(ImportBatchSpendActivity.this).makeTransaction(outpoints, receivers);
                Transaction signedTx = SendFactory.getInstance(ImportBatchSpendActivity.this).signTransaction(tx, account);
                final String hexTx = new String(Hex.encode(signedTx.bitcoinSerialize()));
                System.out.println("hex tx:" + hexTx);

                System.out.println("log:" + stringBuilder.toString());

                final TextView tvText = new TextView(getApplicationContext());
                tvText.setText(hexTx);
                tvText.setTextIsSelectable(true);
                tvText.setPadding(40, 10, 40, 10);
                tvText.setTextColor(0xffffffff);
                AlertDialog.Builder dlg = new AlertDialog.Builder(ImportBatchSpendActivity.this)
                        .setTitle(R.string.app_name)
                        .setView(tvText)
                        .setCancelable(false)

                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        });

                if(!isFinishing())    {
                    dlg.show();
                }

                /*
                try {

                    final TextView tvText = new TextView(getApplicationContext());
                    tvText.setText("");
                    tvText.setTextIsSelectable(true);
                    tvText.setPadding(40, 10, 40, 10);
                    tvText.setTextColor(0xffffffff);
                    AlertDialog.Builder dlg = new AlertDialog.Builder(ImportBatchSpendActivity.this)
                            .setTitle(R.string.app_name)
                            .setView(tvText)
                            .setCancelable(true)

                            .setNegativeButton(R.string.options_display_privkey, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                }
                            })
                            .setPositiveButton(R.string.options_display_privkey, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {

                                }

                            });

                    if(!isFinishing())    {
                        dlg.show();
                    }

                }
                catch(Exception e) {
                    Toast.makeText(ImportBatchSpendActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                */

            }
        });

        btCancel = (Button)findViewById(R.id.cancel);
        btCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        AppUtil.getInstance(ImportBatchSpendActivity.this).checkTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

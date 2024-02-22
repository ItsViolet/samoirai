package com.samourai.wallet.send.review;

import static com.samourai.wallet.send.review.EnumSendType.SPEND_SIMPLE;
import static java.util.Objects.nonNull;

import android.content.Intent;
import android.widget.CheckBox;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.Lists;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.TxAnimUIActivity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.WALLET_INDEX;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.func.FormatsUtil;
import com.samourai.wallet.util.func.SendAddressUtil;

import java.math.BigInteger;
import java.util.List;

public class SpendSimpleTxBroadcaster {

    private final ReviewTxModel model;
    private final SamouraiActivity activity;

    private SpendSimpleTxBroadcaster(final ReviewTxModel model,
                                     final SamouraiActivity activity) {

        this.model = model;
        this.activity = activity;
    }

    public static SpendSimpleTxBroadcaster create(final ReviewTxModel model,
                                                  final SamouraiActivity activity) {

        return new SpendSimpleTxBroadcaster(model, activity);
    }

    public SpendSimpleTxBroadcaster broadcast() {

        final List<MyTransactionOutPoint> outPoints = Lists.newArrayList();
        for (final UTXO u : model.getTxData().getSelectedUTXO()) {
            outPoints.addAll(u.getOutpoints());
        }

        final String changeAddress = SendParams.generateChangeAddress(
                activity,
                model.getTxData().getChange(),
                model.getSendType().getType(),
                model.getAccount(),
                model.getChangeType(),
                getChangeIndex(),
                true);

        if (nonNull(changeAddress)) {
            model.getTxData().getReceivers().put(
                    changeAddress,
                    BigInteger.valueOf(model.getTxData().getChange()));
        }

        SendParams.getInstance().setParams(
                outPoints,
                model.getTxData().getReceivers(),
                BIP47Meta.getInstance().getPcodeFromLabel(model.getAddressLabel()),
                model.getSendType().getType(),
                model.getTxData().getChange(),
                model.getChangeType(),
                model.getAccount(),
                model.getAddress(),
                SendAddressUtil.getInstance().get(model.getAddress()) == 1,
                false,
                model.getAmount(),
                getChangeIndex(),
                model.getTxNote().getValue()
        );

        activity.startActivity(new Intent(activity, TxAnimUIActivity.class));


        return this;
    }

    public int getChangeIndex() {
        final int changeType = model.getChangeType();
        if (model.isPostmixAccount()) {
            return model.getTxData().getIdxBIP84PostMixInternal();
        } else if (changeType == 84) {
            return model.getTxData().getIdxBIP84Internal();
        } else if (changeType == 49) {
            return model.getTxData().getIdxBIP49Internal();
        } else {
            return model.getTxData().getIdxBIP44Internal();
        }
    }
}
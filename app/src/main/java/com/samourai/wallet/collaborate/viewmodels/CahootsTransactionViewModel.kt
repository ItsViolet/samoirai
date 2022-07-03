package com.samourai.wallet.collaborate.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.math.MathUtils
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.rpc.PaymentCode
import com.samourai.wallet.cahoots.CahootsMode
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.segwit.SegwitAddress
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.cahoots.ManualCahootsActivity
import com.samourai.wallet.send.soroban.meeting.SorobanMeetingSendActivity
import com.samourai.wallet.util.FormatsUtil
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex
import java.text.DecimalFormat
import kotlin.math.absoluteValue

class CahootsTransactionViewModel : ViewModel() {

    enum class CahootTransactionType(val cahootsType: CahootsType?, val cahootsMode: CahootsMode?) {
        STONEWALLX2_MANUAL(CahootsType.STONEWALLX2, CahootsMode.MANUAL),
        STONEWALLX2_SAMOURAI(CahootsType.STONEWALLX2, CahootsMode.SAMOURAI),
        STONEWALLX2_SOROBAN(CahootsType.STONEWALLX2, CahootsMode.SOROBAN),
        STOWAWAY_MANUAL(CahootsType.STOWAWAY, CahootsMode.MANUAL),
        STOWAWAY_SOROBAN(CahootsType.STOWAWAY, CahootsMode.SOROBAN),
        MULTI_SOROBAN(CahootsType.MULTI, CahootsMode.SOROBAN),
    }

    private val feesPerByte: MutableLiveData<String> = MutableLiveData("")
    private val currentPage = MutableLiveData(0)
    private val feeRange: MutableLiveData<Float> = MutableLiveData(0.5f)
    private val transactionAccountType = MutableLiveData(SamouraiAccountIndex.DEPOSIT)
    private val validTransaction = MutableLiveData(false)
    private val cahootsType = MutableLiveData<CahootTransactionType?>(null)
    private val destinationAddress = MutableLiveData<String?>(null)
    private val amount = MutableLiveData(0.0)
    private var collaboratorPcode = MutableLiveData<String?>(null)
    private val decimalFormatSatPerByte = DecimalFormat("#.##").also {
        it.isDecimalSeparatorAlwaysShown = true
    }

    fun getFeeSatsValueLive(): LiveData<String> = feesPerByte

    var balance: Long = 0L;
    var feeLow: Long = 0L
    var feeHigh: Long = 0L
    var feeMed: Long = 0L

    val transactionAccountTypeLive: LiveData<Int>
        get() = transactionAccountType

    val validTransactionLive: LiveData<Boolean>
        get() = validTransaction

    val amountLive: LiveData<Double>
        get() = amount

    val destinationAddressLive: LiveData<String?>
        get() = destinationAddress

    val cahootsTypeLive: LiveData<CahootTransactionType?>
        get() = cahootsType

    val collaboratorPcodeLive: LiveData<String?>
        get() = collaboratorPcode

    val feeSliderValue: LiveData<Float>
        get() = feeRange

    val pageLive: LiveData<Int>
        get() = currentPage


    init {
        feeLow = FeeUtil.getInstance().lowFee.defaultPerKB.toLong()
        feeMed = FeeUtil.getInstance().suggestedFee.defaultPerKB.toLong()
        feeHigh = FeeUtil.getInstance().highFee.defaultPerKB.toLong()
        if (feeHigh == 1000L && feeLow == 1000L) {
            feeHigh = 3000L
        }
        if (feeHigh > feeLow && (feeMed - feeHigh) != 0L) {
            try {
                val currentSlider = ((feeMed.toFloat() - feeHigh.toFloat())
                    .div(feeHigh.toFloat().minus(feeLow.toFloat()))).absoluteValue
                feeRange.value = currentSlider
                feeRange.postValue(currentSlider)
                calculateFees(feeRange.value ?: 0.5f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAccountType(account: Int, context: Context) {
        this.transactionAccountType.value = account
        this.transactionAccountType.postValue(account)
        if (account == SamouraiAccountIndex.DEPOSIT) {
            balance = APIFactory.getInstance(context)
                .xpubBalance;
        }
        if (account == SamouraiAccountIndex.POSTMIX) {
            balance = APIFactory.getInstance(context)
                .xpubPostMixBalance
        }
        validate()
    }

    private fun validate() {
        var isValid = true
        val amountSats = (amount.value?.times(1e8)?.toLong() ?: 0L).toLong()
        if (balance < amountSats) {
            isValid = false
        }
        if (amountSats == 0L) {
            isValid = false
        }
        if (cahootsType.value?.cahootsMode == CahootsMode.SOROBAN) {
            if (!FormatsUtil.getInstance().isValidBitcoinAddress(destinationAddress.value ?: "")
                && !FormatsUtil.getInstance().isValidPaymentCode(destinationAddress.value ?: "")
            ) {
                isValid = false
            }
        } else {
            if (!FormatsUtil.getInstance().isValidBitcoinAddress(destinationAddress.value ?: "")
                && !FormatsUtil.getInstance().isValidPaymentCode(destinationAddress.value ?: "")
            ) {
                isValid = false
            }
        }
        if (cahootsType.value?.cahootsMode == CahootsMode.SOROBAN || cahootsType.value?.cahootsMode == CahootsMode.SAMOURAI) {
            if (collaboratorPcode.value == null) {
                isValid = false
            }
        }
        validTransaction.postValue(isValid)
    }

    fun setAmount(amount: Double) {
        this.amount.value = amount
        this.amount.postValue(amount)
        validate()
    }

    fun setCahootType(type: CahootTransactionType?) {
        this.cahootsType.postValue(type)
    }

    fun setCollaborator(pcode: String) {
        this.collaboratorPcode.postValue(pcode)
        validate()
    }

    fun setPage(page: Int) {
        currentPage.postValue(page)
    }

    fun setFeeRange(it: Float) {
        calculateFees(it)
    }

    private fun calculateFees(it: Float) {
        feeLow = FeeUtil.getInstance().lowFee.defaultPerKB.toLong()
        feeMed = FeeUtil.getInstance().suggestedFee.defaultPerKB.toLong()
        feeHigh = FeeUtil.getInstance().highFee.defaultPerKB.toLong()
        if (feeHigh == 1000L && feeLow == 1000L) {
            feeHigh = 3000L
        }
        val fees = MathUtils.lerp(feeLow.toFloat(), feeHigh.toFloat(), it).coerceAtLeast(1f)
        feesPerByte.postValue(decimalFormatSatPerByte.format(fees / 1000))
    }

    fun setAddress(addressEdit: String) {
        this.destinationAddress.postValue(addressEdit)
        validate()
    }

    fun send(context: Context) {
        val account = if (transactionAccountType.value == SamouraiAccountIndex.DEPOSIT) SamouraiAccountIndex.DEPOSIT else SamouraiAccountIndex.POSTMIX
        var type = cahootsType.value ?: return;
        // choose Cahoots counterparty
        val value = amount.value ?: 0L
        if (value == 0) {
            return;
        }
        var address = destinationAddress.value
        if (type.cahootsType == CahootsType.STOWAWAY) {
            address = collaboratorPcode.value
        }
        if (FormatsUtil.getInstance().isValidPaymentCode(address)) {
            val _pcode = PaymentCode(address)
            val paymentAddress = BIP47Util.getInstance(context).getSendAddress(_pcode, BIP47Meta.getInstance().getOutgoingIdx(address))
            address = if (BIP47Meta.getInstance().getSegwit(address)) {
                val segwitAddress = SegwitAddress(paymentAddress.sendECKey, SamouraiWallet.getInstance().currentNetworkParams)
                segwitAddress.bech32AsString
            } else {
                paymentAddress.sendECKey.toAddress(SamouraiWallet.getInstance().currentNetworkParams).toString()
            }

        }
        val amountInSats = amount.value?.times(1e8) ?: 0.0
        if (collaboratorPcode.value == BIP47Meta.getMixingPartnerCode()) {
            type = CahootTransactionType.MULTI_SOROBAN
        }
        if (CahootsMode.MANUAL == type.cahootsMode) {
            // Cahoots manual
            val intent = ManualCahootsActivity.createIntentSender(context, account, type.cahootsType, amountInSats.toLong(), address)
            context.startActivity(intent)
            return
        }
        if (CahootsMode.SOROBAN == type.cahootsMode) {
            // choose Cahoots counterparty
            val intent = SorobanMeetingSendActivity.createIntent(context, account, type.cahootsType, amountInSats.toLong(), address, collaboratorPcode.value)
            context.startActivity(intent)
            return
        }
    }

}
package com.samourai.wallet.psbt

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.cahoots.psbt.PSBT
import com.samourai.wallet.cahoots.psbt.PSBTUtil
import com.samourai.wallet.databinding.ActivityPsbtBinding
import com.samourai.wallet.send.PushTx
import com.samourai.wallet.util.LogUtil
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.registry.RegistryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Transaction
import org.bouncycastle.util.encoders.Hex
import java.io.File


class PSBTActivity : SamouraiActivity() {
    private lateinit var binding: ActivityPsbtBinding
    private var psbt = "";
    val psbtViewModel: PSBTViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_psbt)
        binding = ActivityPsbtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        title = "PSBT"
        setSupportActionBar(binding.psbtToolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.psbtGroup.visibility = View.GONE
        if (intent.extras?.containsKey("psbt") == true) {
            psbt = intent.extras?.getString("psbt") ?: ""
            try {
                psbtViewModel.setPSBT(psbt);
            } catch (e: Exception) {
                Toast.makeText(this, "Error $e", Toast.LENGTH_SHORT).show()
            }
        }

        psbtViewModel.getPSBT().observe(this) {
            it?.let {
                binding.psbtPreview.text = it.dump()
                binding.psbtPreview.setOnClickListener {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("PSBT")
                        .setMessage(binding.psbtPreview.text)
                        .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }

            binding.signTx.isEnabled = it != null
        }

        psbtViewModel.getSignedTx().observe(this) {
            it?.let {
                binding.qrView.setContent(UR.fromBytes(RegistryType.CRYPTO_PSBT.type, Hex.encode(it.bitcoinSerialize())))
                binding.txPreview.text = String(Hex.encode(it.bitcoinSerialize()))
                binding.txPreview.setOnClickListener {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Signed Tx")
                        .setMessage(binding.txPreview.text)
                        .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
            binding.signTx.isEnabled = it != null
            TransitionManager.beginDelayedTransition(binding.root)
            val psbtGroupVisibility = if (it == null) View.VISIBLE else View.GONE;
            val visibilityTx = if (it == null) View.GONE else View.VISIBLE;
            binding.txTitle.text = if (it != null) "Signed Transaction" else "Unsigned Transaction"
            binding.psbtGroup.visibility = psbtGroupVisibility
            binding.txGroup.visibility = visibilityTx
        }
        binding.txCopy.setOnClickListener {
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        }

        binding.signTx.setOnClickListener {
            psbtViewModel.singTx();
        }

        binding.broadCastTransactionBtn.setOnClickListener {
            val transaction = psbtViewModel.getSignedTx().value
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm)
                .setMessage("Do you want to broadcast this transaction?")
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(R.string.confirm) { dialog, _ ->
                    dialog.dismiss()
                    transaction?.let {
                        showLoading(true)
                        psbtViewModel.viewModelScope.launch {
                            try {
                                withContext(Dispatchers.Default) {
                                    val signedHex = String(Hex.encode(it.bitcoinSerialize()))
                                    val response = PushTx.getInstance(applicationContext).pushTx(signedHex)
                                    if (response.first) {
                                        withContext(Dispatchers.Main) {
                                            showLoading(false)
                                            Toast.makeText(applicationContext, R.string.tx_sent_ok, Toast.LENGTH_SHORT).show();
                                            this@PSBTActivity.finish()
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            showLoading(false)
                                            Toast.makeText(applicationContext, "Error: Unable broadcast transaction", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                            } catch (e: Exception) {
                                LogUtil.error("PSBT", e)
                                withContext(Dispatchers.Main) {
                                    showLoading(false)
                                    Toast.makeText(applicationContext, "Error $e", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }.show()
        }

        binding.txShareBtn.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.tx_share_menu, popup.menu)
            val tx = psbtViewModel.getSignedTx().value;
            tx?.let { transaction ->
                popup.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.menu_share_as_text -> {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, String(Hex.encode(transaction.bitcoinSerialize())))
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)
                        }
                        R.id.menu_save_as_tx_file -> {
                            val txFile = "${externalCacheDir}${File.separator}${tx.hashAsString}.tx"
                            val file = File(txFile)
                            psbtViewModel.viewModelScope.launch {
                                withContext(Dispatchers.Default) {
                                    if (file.exists()) {
                                        file.delete()
                                    }
                                    file.writeBytes(transaction.bitcoinSerialize())
                                    file.setReadable(true, false)
                                }
                                withContext(Dispatchers.Main) {
                                    val intent = Intent()
                                    intent.action = Intent.ACTION_SEND
                                    intent.type = "**/**"
                                    if (Build.VERSION.SDK_INT >= 24) {
                                        //From API 24 sending FIle on intent ,require custom file provider
                                        intent.putExtra(
                                            Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                this@PSBTActivity,
                                                this@PSBTActivity
                                                    .packageName + ".provider", file
                                            )
                                        )
                                    } else {
                                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                                    }
                                    startActivity(
                                        Intent.createChooser(
                                            intent,
                                            "Share"
                                        )
                                    )
                                }
                            }
                        }
                    }
                    true
                }
                popup.show()
            }

        }
    }

    override fun onBackPressed() {
        showLoading(false);
        if (psbtViewModel.getSignedTx().value != null) {
            psbtViewModel.clear()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            this.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLoading(show: Boolean) {
        binding.broadcastProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.broadCastTransactionBtn?.text = if (show) " " else getString(R.string.broadcast_transaction)
    }
}


class PSBTViewModel : ViewModel() {
    private var _psbtLiveData: MutableLiveData<PSBT?> = MutableLiveData(null)
    private var _signedTx: MutableLiveData<Transaction?> = MutableLiveData(null)

    fun getPSBT(): LiveData<PSBT?> {
        return _psbtLiveData
    }

    fun getSignedTx(): LiveData<Transaction?> {
        return _signedTx
    }

    fun clear() {
        this._signedTx.postValue(null)
    }

    fun singTx() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                if (_psbtLiveData.value != null) {
                    val psbt = _psbtLiveData.value;
                    val unsignedHash = psbt!!.transaction.hashAsString
                    val unsignedHex = String(Hex.encode(psbt.transaction.bitcoinSerialize()))
                    val tx: Transaction = PSBTUtil.getInstance(null).doPSBTSignTx(psbt);
                    LogUtil.debug("PSBTUtil", "unsigned tx hash:$unsignedHash")
                    LogUtil.debug("PSBTUtil", "unsigned tx:$unsignedHex")
                    LogUtil.debug("PSBTUtil", "  signed tx hash:" + tx.hashAsString)
                    val signedHex = String(Hex.encode(tx.bitcoinSerialize()))
                    LogUtil.debug("PSBTUtil", "  signed tx:$signedHex")
                    withContext(Dispatchers.Main) {
                        _signedTx.postValue(tx)
                    }
                }
            }
        }
    }

    fun setPSBT(strPSBT: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                var _psbt: PSBT? = null
                PSBT.setDebug(true)
                _psbt = try {
                    PSBT.fromBytes(Hex.decode(strPSBT), SamouraiWallet.getInstance().currentNetworkParams)
                } catch (e: Exception) {
                    throw  e;
                }
                val psbt = PSBT.fromBytes(_psbt?.toBytes(), SamouraiWallet.getInstance().currentNetworkParams)
                withContext(Dispatchers.Main) {
                    _psbtLiveData.postValue(psbt)
                }
            }
        }


    }
}


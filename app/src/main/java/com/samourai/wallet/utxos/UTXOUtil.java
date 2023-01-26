package com.samourai.wallet.utxos;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.util.FormatsUtil;

import org.bitcoinj.core.Address;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class UTXOUtil {

    public enum AddressTypes {
        LEGACY,
        SEGWIT_COMPAT,
        SEGWIT_NATIVE;
    }

    private static UTXOUtil instance = null;

    private static HashMap<String, List<String>> utxoAutoTags = null;
    private static HashMap<String,String> utxoNotes = null;
    private static HashMap<String,Integer> utxoScores = null;

    private UTXOUtil() {
        ;
    }

    public static UTXOUtil getInstance() {

        if(instance == null) {
            utxoAutoTags = new HashMap<String,List<String>>();
            utxoNotes = new HashMap<String,String>();
            utxoScores = new HashMap<String,Integer>();
            instance = new UTXOUtil();
        }

        return instance;
    }

    public void reset() {
        utxoAutoTags.clear();
        utxoNotes.clear();
        utxoScores.clear();
    }

    public void add(String hash, int idx, String tag) {
        add(hash + "-" + idx, tag);
    }

    public void add(String utxo, String tag) {
        if(utxoAutoTags.containsKey(utxo) && !utxoAutoTags.get(utxo).contains(tag)) {
            utxoAutoTags.get(utxo).add(tag);
        }
        else {
            List<String> tags = new ArrayList<String>();
            tags.add(tag);
            utxoAutoTags.put(utxo, tags);
        }
    }

    public List<String> get(String hash, int idx) {
        if (utxoAutoTags.containsKey(hash + "-" + idx)) {
            return utxoAutoTags.get(hash + "-" + idx);
        } else {
            return null;
        }

    }

    public List<String> get(String utxo) {
        if (utxoAutoTags.containsKey(utxo)) {
            return utxoAutoTags.get(utxo);
        } else {
            return null;
        }

    }

    public HashMap<String, List<String>> getTags() {
        return utxoAutoTags;
    }

    public void remove(String hash, int idx) {
        utxoAutoTags.remove(hash + "-" + idx);
    }

    public void remove(String utxo) {
        utxoAutoTags.remove(utxo);
    }

    public void addNote(String hash, String note) {
        utxoNotes.put(hash, note);
    }

    public String getNote(String hash) {
        if(utxoNotes.containsKey(hash))  {
            return utxoNotes.get(hash);
        }
        else    {
            return null;
        }

    }

    public HashMap<String,String> getNotes() {
        return utxoNotes;
    }

    public void removeNote(String hash) {
        utxoNotes.remove(hash);
    }

    public void addScore(String utxo, int score) {
        utxoScores.put(utxo, score);
    }

    public int getScore(String utxo) {
        if(utxoScores.containsKey(utxo))  {
            return utxoScores.get(utxo);
        }
        else    {
            return 0;
        }

    }

    public void incScore(String utxo, int score) {
        if(utxoScores.containsKey(utxo))  {
            utxoScores.put(utxo, utxoScores.get(utxo) + score);
        }
        else    {
            utxoScores.put(utxo, score);
        }

    }

    public HashMap<String,Integer> getScores() {
        return utxoScores;
    }

    public void removeScore(String utxo) {
        utxoScores.remove(utxo);
    }

    public JSONArray toJSON() {

        JSONArray utxos = new JSONArray();
        for (String key : utxoAutoTags.keySet()) {
            List<String> tags = utxoAutoTags.get(key);
            List<String> _tags = new ArrayList<String>(new HashSet<String>(tags));
            for(String t : _tags) {
                JSONArray tag = new JSONArray();
                tag.put(key);
                tag.put(t);
                utxos.put(tag);
            }
        }

        return utxos;
    }

    public void fromJSON(JSONArray utxos) {

        utxoAutoTags.clear();

        try {
            for (int i = 0; i < utxos.length(); i++) {
                JSONArray tag = (JSONArray) utxos.get(i);

                if(utxoAutoTags.containsKey((String) tag.get(0)) && !utxoAutoTags.get((String) tag.get(0)).contains((String) tag.get(1))) {
                    utxoAutoTags.get((String) tag.get(0)).add((String) tag.get(1));
                }
                else     {
                    List<String> tags = new ArrayList<String>();
                    tags.add((String) tag.get(1));
                    utxoAutoTags.put((String) tag.get(0), tags);
                }

            }
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public JSONArray toJSON_notes() {

        JSONArray utxos = new JSONArray();
        for(String key : utxoNotes.keySet()) {
            JSONArray note = new JSONArray();
            note.put(key);
            note.put(utxoNotes.get(key));
            utxos.put(note);
        }

        return utxos;
    }

    public void fromJSON_notes(JSONArray utxos) {

        utxoNotes.clear();

        try {
            for(int i = 0; i < utxos.length(); i++) {
                JSONArray note = (JSONArray)utxos.get(i);
                utxoNotes.put((String)note.get(0), (String)note.get(1));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public JSONArray toJSON_scores() {

        JSONArray utxos = new JSONArray();
        for(String key : utxoScores.keySet()) {
            JSONArray score = new JSONArray();
            score.put(key);
            score.put(utxoScores.get(key));
            utxos.put(score);
        }

        return utxos;
    }

    public void fromJSON_scores(JSONArray utxos) {

        utxoScores.clear();

        try {
            for(int i = 0; i < utxos.length(); i++) {
                JSONArray score = (JSONArray)utxos.get(i);
                utxoScores.put((String)score.get(0), (int)score.get(1));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static AddressTypes getAddressType(String address) {


        if (FormatsUtil.getInstance().isValidBech32(address)) {
            // is bech32: p2wpkh BIP84
            return AddressTypes.SEGWIT_NATIVE;
        } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
            // is P2SH wrapped segwit BIP49
            return AddressTypes.SEGWIT_COMPAT;
        } else {
            return AddressTypes.LEGACY;
        }
    }

    /*
{ "type": "tx", "ref": "f91d0a8a78462bc59398f2c5d7a84fcff491c26ba54c4833478b202796c8aafd", "label": "Transaction" }
{ "type": "addr", "ref": "bc1q34aq5drpuwy3wgl9lhup9892qp6svr8ldzyy7c", "label": "Address" }
{ "type": "pubkey", "ref": "0283409659355b6d1cc3c32decd5d561abaac86c37a353b52895a5e6c196d6f448", "label": "Public Key" }
{ "type": "input", "ref": "f91d0a8a78462bc59398f2c5d7a84fcff491c26ba54c4833478b202796c8aafd:0", "label": "Input" }
{ "type": "output", "ref": "f91d0a8a78462bc59398f2c5d7a84fcff491c26ba54c4833478b202796c8aafd:1", "label": "Output" }
{ "type": "xpub", "ref": "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8", "label": "Extended Public Key" }     */
    public JSONArray toBIP329() {

        JSONArray utxos = new JSONArray();

        List<String> keys = new ArrayList<String>();
        for (String key : utxoAutoTags.keySet()) {
            keys.add(key);
        }
        for (String key : utxoNotes.keySet()) {
            if(!keys.contains(key)) {
                keys.add(key);
            }
        }

        try {
            for (String key : keys) {
                JSONObject obj = new JSONObject();
                obj.put("type", "output");
                obj.put("ref", key.replace("-", ":"));
                if(utxoAutoTags.containsKey(key)) {
                    List<String> tags = utxoAutoTags.get(key);
                    obj.put("label", utxoAutoTags.get(tags.get(0)));
                    if(tags.size() > 1) {
                        for(int i = 1; i < tags.size(); i++)    {
                            JSONObject _obj = new JSONObject();
                            _obj.put("type", "output");
                            _obj.put("ref", key.replace("-", ":"));
                            _obj.put("label", utxoAutoTags.get(tags.get(i)));
                            utxos.put(_obj);
                        }
                    }
                }
                if(utxoNotes.containsKey(key)) {
                    obj.put("note", utxoNotes.get(key));
                }
                if(BlockedUTXO.getInstance().containsAny(key)) {
                    obj.put("blocked", BlockedUTXO.getInstance().get(key.replace("-", ":")));
                }

                utxos.put(obj);
            }

        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }

        return utxos;
    }

    public void fromBIP329(JSONArray utxos) {

        utxoAutoTags.clear();
        utxoNotes.clear();

        try {
            for (int i = 0; i < utxos.length(); i++) {

                JSONObject obj = (JSONObject) utxos.get(i);

                String ref = null;
                if(!obj.has("ref")) {
                    continue;
                }
                else {
                    ref = obj.getString("ref").replace(":", "-");
                }

                if(obj.has("label")) {
                    if(!utxoAutoTags.containsKey(ref)) {
                        List<String> tags = new ArrayList<String>();
                        utxoAutoTags.put(ref, tags);
                    }
                    utxoAutoTags.get(ref).add(obj.getString("label"));
                }
                if(obj.has("note")) {
                    utxoNotes.put(ref, obj.getString("note"));
                }
                if(obj.has("blocked") && obj.getLong("blocked") > 0L) {
                    BlockedUTXO.getInstance().add(ref, obj.getLong("blocked"));
                }

            }
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}

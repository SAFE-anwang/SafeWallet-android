package org.consenlabs.tokencore.wallet;

import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.consenlabs.tokencore.foundation.utils.MnemonicUtil;
import org.consenlabs.tokencore.foundation.utils.NumericUtil;
import org.consenlabs.tokencore.wallet.address.AddressCreatorManager;
import org.consenlabs.tokencore.wallet.address.EthereumAddressCreator;
import org.consenlabs.tokencore.wallet.keystore.IMTKeystore;
import org.consenlabs.tokencore.wallet.keystore.Keystore;
import org.consenlabs.tokencore.wallet.keystore.V3Keystore;
import org.consenlabs.tokencore.wallet.keystore.WalletKeystore;
import org.consenlabs.tokencore.wallet.model.ChainType;
import org.consenlabs.tokencore.wallet.model.KeyPair;
import org.consenlabs.tokencore.wallet.model.Messages;
import org.consenlabs.tokencore.wallet.model.Metadata;
import org.consenlabs.tokencore.wallet.model.MnemonicAndPath;
import org.consenlabs.tokencore.wallet.model.Network;
import org.consenlabs.tokencore.wallet.model.TokenException;
import org.consenlabs.tokencore.wallet.transaction.EOSKey;
import org.consenlabs.tokencore.wallet.validators.PrivateKeyValidator;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nullable;


public class WalletManager {
  private static Hashtable<String, IMTKeystore> keystoreMap = new Hashtable<>();
  private static final String LOG_TAG = WalletManager.class.getSimpleName();

  public static KeystoreStorage storage;
//
//  static {
//    try {
//      scanWallets();
//    } catch (IOException ignored) {
//    }
//  }

  static Wallet createWallet(IMTKeystore keystore) {
    File file = generateWalletFile(keystore.getId());
    writeToFile(keystore, file);
    keystoreMap.put(keystore.getId(), keystore);
    return new Wallet(keystore);
  }

  public static void changePassword(String id, String oldPassword, String newPassword) {
    IMTKeystore keystore = mustFindKeystoreById(id);
    IMTKeystore newKeystore = (IMTKeystore) keystore.changePassword(oldPassword, newPassword);
    flushWallet(newKeystore, true);
  }

  public static String getAddressFromPrivateKey(Metadata metadata, String prvKeyHex, String password) {
    IMTKeystore keystore = V3Keystore.create(metadata, password, prvKeyHex);
//    Wallet wallet = flushWallet(keystore, overwrite);
//    Identity.getCurrentIdentity().addWallet(wallet);
    return keystore.getAddress();
  }


  public static Wallet findWalletByKeystore(String chainType, String keystoreContent, String password) {
    WalletKeystore walletKeystore = validateKeystore(keystoreContent, password);

    byte[] prvKeyBytes = walletKeystore.decryptCiphertext(password);
    String address = new EthereumAddressCreator().fromPrivateKey(prvKeyBytes);
    return findWalletByAddress(chainType, address);
  }


  static Wallet findWalletById(String id) {
    IMTKeystore keystore = keystoreMap.get(id);
    if (keystore != null) {
      return new Wallet(keystore);
    } else {
      return null;
    }
  }

  public static Wallet mustFindWalletById(String id) {
    IMTKeystore keystore = keystoreMap.get(id);
    if (keystore == null) throw new TokenException(Messages.WALLET_NOT_FOUND);
    return new Wallet(keystore);
  }


  static File generateWalletFile(String walletID) {
    return new File(getDefaultKeyDirectory(), walletID + ".json");
  }


  static File getDefaultKeyDirectory() {
    File directory = new File(storage.getKeystoreDir(), "wallets");
    if (!directory.exists()) {
      directory.mkdirs();
    }
    return directory;
  }

  static boolean cleanKeystoreDirectory() {
    return deleteDir(getDefaultKeyDirectory());
  }

  private static IMTKeystore findKeystoreByAddress(String type, String address) {
    if (address == null || address.isEmpty()) return null;

    for (IMTKeystore keystore : keystoreMap.values()) {

      if (keystore.getAddress() == null || keystore.getAddress().isEmpty()) {
        continue;
      }

      if (keystore.getMetadata().getChainType().equals(type) && keystore.getAddress().equals(address)) {
        return keystore;
      }
    }

    return null;
  }

  public static Wallet findWalletByAddress(String type, String address) {
    IMTKeystore keystore = findKeystoreByAddress(type, address);
    if (keystore != null) {
      return new Wallet(keystore);
    }
    return null;
  }


  private static Wallet flushWallet(IMTKeystore keystore, boolean overwrite) {

    IMTKeystore existsKeystore = findKeystoreByAddress(keystore.getMetadata().getChainType(), keystore.getAddress());
    if (existsKeystore != null) {
      if (!overwrite) {
        throw new TokenException(Messages.WALLET_EXISTS);
      } else {
        keystore.setId(existsKeystore.getId());
      }
    }

    File file = generateWalletFile(keystore.getId());
    writeToFile(keystore, file);
    keystoreMap.put(keystore.getId(), keystore);
    return new Wallet(keystore);
  }

  private static void writeToFile(Keystore keyStore, File destination) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      objectMapper.writeValue(destination, keyStore);
    } catch (IOException ex) {
      throw new TokenException(Messages.WALLET_STORE_FAIL, ex);
    }
  }

  private static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (String child : children) {
        boolean success = deleteDir(new File(dir, child));
        if (!success) {
          return false;
        }
      }
    }
    return dir.delete();
  }

  private static V3Keystore validateKeystore(String keystoreContent, String password) {
    V3Keystore importedKeystore = unmarshalKeystore(keystoreContent, V3Keystore.class);
    if (importedKeystore.getAddress() == null || importedKeystore.getAddress().isEmpty() || importedKeystore.getCrypto() == null) {
      throw new TokenException(Messages.WALLET_INVALID_KEYSTORE);
    }

    importedKeystore.getCrypto().validate();

    if (!importedKeystore.verifyPassword(password))
      throw new TokenException(Messages.MAC_UNMATCH);

    byte[] prvKey = importedKeystore.decryptCiphertext(password);
    String address = new EthereumAddressCreator().fromPrivateKey(prvKey);
    if (address == null || address.isEmpty() || !address.equalsIgnoreCase(importedKeystore.getAddress())) {
      throw new TokenException(Messages.PRIVATE_KEY_ADDRESS_NOT_MATCH);
    }
    return importedKeystore;
  }

  private static IMTKeystore mustFindKeystoreById(String id) {
    IMTKeystore keystore = keystoreMap.get(id);
    if (keystore == null) {
      throw new TokenException(Messages.WALLET_NOT_FOUND);
    }

    return keystore;
  }

  private static <T extends WalletKeystore> T unmarshalKeystore(String keystoreContent, Class<T> clazz) {
    T importedKeystore;
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
      importedKeystore = mapper.readValue(keystoreContent, clazz);
    } catch (IOException ex) {
      throw new TokenException(Messages.WALLET_INVALID_KEYSTORE, ex);
    }
    return importedKeystore;
  }

  private WalletManager() {
  }
}

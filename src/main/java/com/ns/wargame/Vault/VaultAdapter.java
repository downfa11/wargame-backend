package com.ns.wargame.Vault;
/*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

@Component
public class VaultAdapter {
    private final AESProvider encryptor;

    @Autowired
    public VaultAdapter(VaultTemplate vaultTemplate) {
        VaultKeyValueOperations ops = vaultTemplate.opsForKeyValue("kv-v1/data/ecrypt", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);
        VaultResponse response = ops.get("dbkey");

        //kv-v1
        // ecrypt/data/dbkey
        // QgOklUDRhvNfCex0/BllsQ==
        // path "kv-v1/*" { capabilities = ["create", "read", "update", "delete", "sudo", "list"] }
        // vault write auth/token/create policies=default

        if (response != null) {
            String key = (String) response.getData().get("key");
            this.encryptor = new AESProvider(key);
        } else {
            throw new RuntimeException("Vault key 'dbkey' not found");
        }
    }


    public String encrypt(String plainText) {
        try {
            return encryptor.encrypt(plainText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            return encryptor.decrypt(encryptedText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}*/

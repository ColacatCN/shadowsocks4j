package com.life4ever.shadowsocks4j.proxy.cipher.impl;

import com.life4ever.shadowsocks4j.proxy.cipher.AbstractCipherFunction;
import com.life4ever.shadowsocks4j.proxy.exception.Shadowsocks4jProxyException;

import javax.crypto.spec.GCMParameterSpec;
import java.security.spec.AlgorithmParameterSpec;

import static com.life4ever.shadowsocks4j.proxy.constant.CipherConfigConstant.AES;

public class AesCipherFunctionImpl extends AbstractCipherFunction {

    private static final int TAG_BIT_LENGTH = 128;

    public AesCipherFunctionImpl(String password, String salt, int secretKeyLength, String mode) throws Shadowsocks4jProxyException {
        super(AES, password, salt, secretKeyLength, mode);
    }

    @Override
    protected AlgorithmParameterSpec createParameterSpecForEncryption(byte[] nonce) {
        return new GCMParameterSpec(TAG_BIT_LENGTH, nonce);
    }

    @Override
    protected AlgorithmParameterSpec createParameterSpecForDecryption(byte[] encryptedContent) {
        return new GCMParameterSpec(TAG_BIT_LENGTH, encryptedContent, 0, NONCE_LENGTH);
    }

}

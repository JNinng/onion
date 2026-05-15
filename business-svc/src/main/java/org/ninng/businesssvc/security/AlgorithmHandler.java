package org.ninng.businesssvc.security;

public interface AlgorithmHandler {

    String encrypt(Object plain) throws Exception;

    String decrypt(String cipherText) throws Exception;

    String support();
}

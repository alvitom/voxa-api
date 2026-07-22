package com.voxa.api.service;

import com.voxa.api.config.HashProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

@Service("hs256")
@RequiredArgsConstructor
public class HmacSha256Service implements HashService {
    private final HashProperties hashProperties;

    @SneakyThrows
    @Override
    public String hash(String data) {
        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKey secretKey = new SecretKeySpec(hashProperties.secretKey().getBytes(), "HmacSHA256");

        mac.init(secretKey);

        byte[] result = mac.doFinal(data.getBytes());

        return HexFormat.of().formatHex(result);
    }
}

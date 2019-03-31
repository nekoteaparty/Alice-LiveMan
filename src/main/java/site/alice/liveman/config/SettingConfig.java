/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package site.alice.liveman.config;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.alice.liveman.dataobject.ExternalAppSecretDO;
import site.alice.liveman.model.LiveManSetting;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class SettingConfig {

    private static final File settingFile = new File("setting.bin");

    @Bean
    public LiveManSetting getLiveManSetting() throws Exception {
        LiveManSetting liveManSetting;
        if (settingFile.exists()) {
            liveManSetting = readSetting();
            if (liveManSetting.getServers() == null) {
                liveManSetting.setServers(new CopyOnWriteArraySet<>());
            }
            if (liveManSetting.getExternalAppSecretDOS() == null) {
                liveManSetting.setExternalAppSecretDOS(new CopyOnWriteArraySet<>());
            }
        } else {
            liveManSetting = new LiveManSetting();
            liveManSetting.setAccounts(new CopyOnWriteArraySet<>());
            liveManSetting.setChannels(new CopyOnWriteArraySet<>());
            liveManSetting.setServers(new CopyOnWriteArraySet<>());
            liveManSetting.setExternalAppSecretDOS(new CopyOnWriteArraySet<>());
            liveManSetting.setBannedKeywords(new String[0]);
            liveManSetting.setBannedYoutubeChannel(new String[0]);
            liveManSetting.setTempPath("liveManTemp");
            saveSetting(liveManSetting);
        }
        return liveManSetting;
    }

    public synchronized LiveManSetting readSetting() throws Exception {
        try (InputStream is = new FileInputStream(settingFile)) {
            byte[] data = IOUtils.toByteArray(is);
            long keyTimestamp = settingFile.lastModified();
            log.info("settingFile lastModified = " + keyTimestamp);
            Cipher cipher = getCipher(keyTimestamp / 1000 + "", Cipher.DECRYPT_MODE);
            byte[] decodedData = cipher.doFinal(data);
            String settingJson = new String(decodedData, StandardCharsets.UTF_8);
            return JSON.parseObject(settingJson, LiveManSetting.class);
        }
    }

    public synchronized void saveSetting(LiveManSetting liveManSetting) throws RuntimeException {
        try {
            new File("./keys/").mkdirs();
            long keyTimestamp = System.currentTimeMillis();
            String settingJson = JSON.toJSONString(liveManSetting);
            byte[] data = settingJson.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = getCipher(keyTimestamp / 1000 + "", Cipher.ENCRYPT_MODE);
            byte[] encodedData = cipher.doFinal(data);
            File tempFile = new File(settingFile.toString() + ".tmp");
            tempFile.getAbsoluteFile().getParentFile().mkdirs();
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                IOUtils.write(encodedData, fileOutputStream);
            }
            if (tempFile.setLastModified((keyTimestamp / 1000) * 1000) && new File("./keys/" + keyTimestamp + ".key").createNewFile()) {
                settingFile.delete();
                tempFile.renameTo(settingFile);
            }
        } catch (Exception e) {
            throw new RuntimeException("配置文件保存失败", e);
        }
    }

    private Cipher getCipher(String key, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(key.getBytes());
        keygen.init(128, secureRandom);
        SecretKey originalKey = keygen.generateKey();
        byte[] raw = originalKey.getEncoded();
        SecretKey secretKey = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode, secretKey);
        return cipher;
    }
}

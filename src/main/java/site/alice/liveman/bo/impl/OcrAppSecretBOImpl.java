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

package site.alice.liveman.bo.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.bo.OcrAppSecretBO;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.dataobject.OcrAppSecretDO;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OcrAppSecretBOImpl implements OcrAppSecretBO {

    @Autowired
    private LiveManSetting liveManSetting;
    @Autowired
    private SettingConfig  settingConfig;

    @Override
    public OcrAppSecretDO getOcrAppSecret(String type) {
        try {
            List<OcrAppSecretDO> ocrAppSecretDOS = liveManSetting.getOcrAppSecretDOS().stream().filter(ocrAppSecretDO -> type.equals(ocrAppSecretDO.getType())).peek(ocrAppSecretDO -> {
                Date nextResumeTime = ocrAppSecretDO.getNextResumeTime();
                if (nextResumeTime != null && nextResumeTime.getTime() <= System.currentTimeMillis()) {
                    ocrAppSecretDO.getLimit().set(ocrAppSecretDO.getTotalLimit());
                    ocrAppSecretDO.setNextResumeTime(new Date(nextResumeTime.getYear(), nextResumeTime.getMonth(), nextResumeTime.getDate() + 1));
                }
            }).sorted((o1, o2) -> o2.getLimit().get() - o1.getLimit().get()).collect(Collectors.toList());
            if (!ocrAppSecretDOS.isEmpty()) {
                OcrAppSecretDO ocrAppSecretDO = ocrAppSecretDOS.get(0);
                ocrAppSecretDO.getLimit().incrementAndGet();
                log.info("返回OcrAppSecret：" + ToStringBuilder.reflectionToString(ocrAppSecretDO));
                return ocrAppSecretDO;
            }
            return null;
        } finally {
            settingConfig.saveSetting(liveManSetting);
        }
    }

    @Override
    public void insert(OcrAppSecretDO ocrAppSecretDO) {
        Date date = new Date();
        ocrAppSecretDO.setNextResumeTime(new Date(date.getYear(), date.getMonth(), date.getDate() + 1));
        ocrAppSecretDO.setLimit(new AtomicInteger(ocrAppSecretDO.getTotalLimit()));
        try {
            liveManSetting.getOcrAppSecretDOS().add(ocrAppSecretDO);
        } finally {
            settingConfig.saveSetting(liveManSetting);
        }
    }
}

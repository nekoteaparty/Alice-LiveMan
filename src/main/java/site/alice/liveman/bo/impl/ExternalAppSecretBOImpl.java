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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.alice.liveman.bo.ExternalAppSecretBO;
import site.alice.liveman.config.SettingConfig;
import site.alice.liveman.dataobject.ExternalAppSecretDO;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.jenum.ExternalServiceType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExternalAppSecretBOImpl implements ExternalAppSecretBO {

    @Autowired
    private LiveManSetting liveManSetting;
    @Autowired
    private SettingConfig  settingConfig;

    @Override
    public ExternalAppSecretDO getAppSecret(ExternalServiceType type) {
        try {
            List<ExternalAppSecretDO> externalAppSecretDOS = liveManSetting.getExternalAppSecretDOS().stream().filter(ocrAppSecretDO -> type.getCode().equals(ocrAppSecretDO.getType())).peek(ocrAppSecretDO -> {
                Date nextResumeTime = ocrAppSecretDO.getNextResumeTime();
                if (nextResumeTime != null && nextResumeTime.getTime() <= System.currentTimeMillis()) {
                    ocrAppSecretDO.getLimit().set(ocrAppSecretDO.getTotalLimit());
                    ocrAppSecretDO.setNextResumeTime(new Date(nextResumeTime.getYear(), nextResumeTime.getMonth(), nextResumeTime.getDate() + 1));
                }
            }).sorted((o1, o2) -> o2.getLimit().get() - o1.getLimit().get()).collect(Collectors.toList());
            if (!externalAppSecretDOS.isEmpty()) {
                ExternalAppSecretDO externalAppSecretDO = externalAppSecretDOS.get(0);
                externalAppSecretDO.getLimit().decrementAndGet();
                log.info("返回OcrAppSecret：" + ToStringBuilder.reflectionToString(externalAppSecretDO));
                return externalAppSecretDO;
            }
            log.info("没有找到可用的AppSecret，请求的Type:" + type.getCode());
            return null;
        } finally {
            settingConfig.saveSetting(liveManSetting);
        }
    }

    @Override
    public void insert(ExternalAppSecretDO externalAppSecretDO) {
        if (externalAppSecretDO.getNextResumeTime() == null) {
            Date date = new Date();
            externalAppSecretDO.setNextResumeTime(new Date(date.getYear(), date.getMonth(), date.getDate() + 1));
        }
        if (externalAppSecretDO.getLimit() == null) {
            externalAppSecretDO.setLimit(externalAppSecretDO.getTotalLimit());
        }
        try {
            liveManSetting.getExternalAppSecretDOS().add(externalAppSecretDO);
        } finally {
            settingConfig.saveSetting(liveManSetting);
        }
    }

    @Override
    public List<ExternalAppSecretDO> selectForList() {
        CopyOnWriteArraySet<ExternalAppSecretDO> externalAppSecretDOS = liveManSetting.getExternalAppSecretDOS();
        if (liveManSetting.getExternalAppSecretDOS() == null) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(externalAppSecretDOS);
        }
    }

    @Override
    public int update(ExternalAppSecretDO externalAppSecretDO) {
        AtomicInteger count = new AtomicInteger();
        liveManSetting.getExternalAppSecretDOS().forEach(appSecretDO -> {
            if (appSecretDO.equals(externalAppSecretDO)) {
                BeanUtils.copyProperties(externalAppSecretDO, appSecretDO);
                count.getAndIncrement();
            }
        });
        if (count.get() > 0) {
            settingConfig.saveSetting(liveManSetting);
        }
        return count.get();
    }

    @Override
    public int remove(ExternalAppSecretDO externalAppSecretDO) {
        boolean result = liveManSetting.getExternalAppSecretDOS().removeIf(appSecretDO -> appSecretDO.equals(externalAppSecretDO));
        if (result) {
            settingConfig.saveSetting(liveManSetting);
        }
        return result ? 1 : 0;
    }
}

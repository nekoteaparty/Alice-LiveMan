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

package site.alice.liveman.dataobject;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ExternalAppSecretDO {
    private String        type;
    private String        appId;
    private String        appKey;
    private String        secretKey;
    private AtomicInteger limit;
    private int           totalLimit;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    private Date          nextResumeTime;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public AtomicInteger getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = new AtomicInteger(limit);
    }

    public int getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(int totalLimit) {
        this.totalLimit = totalLimit;
    }

    public Date getNextResumeTime() {
        return nextResumeTime;
    }

    public void setNextResumeTime(Date nextResumeTime) {
        this.nextResumeTime = nextResumeTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalAppSecretDO that = (ExternalAppSecretDO) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(appId, that.appId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, appId);
    }
}

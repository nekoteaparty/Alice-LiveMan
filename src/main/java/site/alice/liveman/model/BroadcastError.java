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

package site.alice.liveman.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class BroadcastError {
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date   errTime;
    private String errMsg;

    public BroadcastError() {
    }

    public BroadcastError(String errMsg) {
        this.errTime = new Date();
        this.errMsg = errMsg;
    }

    public BroadcastError(Date errTime, String errMsg) {
        this.errTime = errTime;
        this.errMsg = errMsg;
    }

    public Date getErrTime() {
        return errTime;
    }

    public void setErrTime(Date errTime) {
        this.errTime = errTime;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
}

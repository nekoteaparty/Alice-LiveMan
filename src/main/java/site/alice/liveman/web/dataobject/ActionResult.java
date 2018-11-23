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

package site.alice.liveman.web.dataobject;

public class ActionResult<T> {

    private int     code;
    private boolean success;
    private T       data;
    private String  message;

    public static <T> ActionResult<T> getSuccessResult(T data) {
        ActionResult<T> actionResult = new ActionResult<>();
        actionResult.setSuccess(true);
        actionResult.setCode(0);
        actionResult.setData(data);
        return actionResult;
    }

    public static <T> ActionResult<T> getSuccessResult(T data, String message) {
        ActionResult<T> actionResult = new ActionResult<>();
        actionResult.setSuccess(true);
        actionResult.setCode(0);
        actionResult.setData(data);
        actionResult.setMessage(message);
        return actionResult;
    }

    public static <T> ActionResult<T> getErrorResult(String message) {
        ActionResult<T> actionResult = new ActionResult<>();
        actionResult.setSuccess(false);
        actionResult.setCode(-1);
        actionResult.setMessage(message);
        return actionResult;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

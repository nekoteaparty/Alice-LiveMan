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

package site.alice.liveman.job;

import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.uploadFile.OneUploadFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.alice.liveman.model.LiveManSetting;
import site.alice.liveman.model.MediaHistory;
import site.alice.liveman.service.MediaHistoryService;
import site.alice.liveman.utils.OneDriveUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RecordAndCleanUpJob {

    private static final Logger              LOGGER                    = LoggerFactory.getLogger(RecordAndCleanUpJob.class);
    private static final long                LAST_MODIFIED_TIME_MILLIS = 15 * 60 * 1000;
    private static final long                MAX_RECORD_FILE_SIZE      = 4 * 1024 * 1024 * 1024L;
    @Autowired
    private              MediaHistoryService mediaHistoryService;
    @Autowired
    private              LiveManSetting      liveManSetting;
    @Autowired
    private              OneDriveUtil        oneDriveUtil;

    @Scheduled(cron = "0 0/5 * * * ?")
    public void recordAndCleanUpJob() {
        LOGGER.info("开始查找需要上传录像和清理的媒体文件...");
        if (StringUtils.isEmpty(liveManSetting.getOneDriveToken())) {
            LOGGER.warn("尚未配置OneDrive授权，将不会上传录像文件。");
        }
        String mediaTempPath = liveManSetting.getTempPath();
        File mediaTempDir = new File(mediaTempPath);
        File[] listDir = mediaTempDir.listFiles();
        if (listDir != null) {
            for (File mediaDir : listDir) {
                File[] pathList = mediaDir.listFiles(file -> System.currentTimeMillis() - file.lastModified() >= LAST_MODIFIED_TIME_MILLIS);
                if (pathList != null) {
                    for (File sourcePath : pathList) {
                        LOGGER.info("开始上传[videoId=" + sourcePath.getName() + "]的录像数据...");
                        switch (mediaDir.getName()) {
                            case "m3u8": {
                                if (concatListFile(sourcePath)) {
                                    uploadDir(sourcePath, sourcePath.getName());
                                }
                                break;
                            }
                            case "mp4": {
                                uploadDir(sourcePath, sourcePath.getName());
                                break;
                            }
                            case "broadcast": {
                                FileUtils.deleteQuietly(sourcePath);
                                break;
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("上传录像和清理任务已完成！");
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void freeSpaceMasterJob() {
        String mediaTempPath = liveManSetting.getTempPath();
        File mediaTempDir = new File(mediaTempPath);
        if (mediaTempDir.exists() && mediaTempDir.getFreeSpace() < 1073741824) {
            LOGGER.warn("磁盘可用空间严重不足，强制清除所有录像缓存数据！当前可用空间:" + mediaTempDir.getFreeSpace() + "字节");
            FileUtils.deleteQuietly(mediaTempDir);
        }
    }

    private void uploadDir(File sourceFile, final String videoId) {
        String[] videoIdSplits = videoId.split("\\.");
        MediaHistory mediaHistory;
        if (videoIdSplits.length > 1) {
            mediaHistory = mediaHistoryService.getMediaHistory(videoIdSplits[0]);
        } else {
            mediaHistory = mediaHistoryService.getMediaHistory(videoId);
        }
        boolean success = false;
        if (sourceFile.isDirectory()) {
            LOGGER.info("uploadDir:" + sourceFile.getAbsoluteFile());
            File[] children = sourceFile.listFiles();
            if (children != null && children.length > 0) {
                for (File child : children) {
                    uploadDir(child, videoId);
                }
            }
            success = true;
        } else if (mediaHistory != null) {
            if (sourceFile.length() == 0) {
                LOGGER.info("文件[" + sourceFile + "]的长度为0，跳过上传!");
                success = true;
            } else if (!StringUtils.isEmpty(liveManSetting.getOneDriveToken())) {
                try {
                    OneFolder recordFolder = oneDriveUtil.getOneFolder("Record");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH_mm_ss");
                    String fileName = mediaHistory.getChannelName();
                    if (StringUtils.isEmpty(fileName) || fileName.equals("null")) {
                        fileName = "手动推流";
                    }
                    fileName = fileName.trim();
                    fileName += "\\/" + dateFormat.format(mediaHistory.getDatetime());
                    fileName += "\\/" + timeFormat.format(mediaHistory.getDatetime()) + "_" + mediaHistory.getVideoId() + "_" + replaceFileName(String.valueOf(mediaHistory.getVideoTitle()).trim());
                    fileName += "\\/" + sourceFile.getName();
                    LOGGER.info("开始上传录像[" + sourceFile.getAbsoluteFile() + "]到[" + fileName + "]...");
                    OneUploadFile oneUploadFile = recordFolder.uploadFile(sourceFile, fileName);
                    oneUploadFile.startUpload();
                    LOGGER.info("录像文件[" + sourceFile.getAbsoluteFile() + "]上传已完成！");
                    success = true;
                } catch (Throwable throwable) {
                    LOGGER.error("录像保存失败[" + sourceFile.getAbsoluteFile() + "]", throwable);
                }
            } else {
                success = true;
            }
        }
        if (sourceFile.exists()) {
            if (success) {
                sourceFile.delete();
            } else if (sourceFile.getFreeSpace() < MAX_RECORD_FILE_SIZE) {
                LOGGER.warn("磁盘可用空间不足4GB，当前可用空间[" + sourceFile.getFreeSpace() + "]，强制删除上传失败的录像文件[" + sourceFile + "]");
                sourceFile.delete();
            }
        }
    }

    private String replaceFileName(String fileName) {
        Pattern pattern = Pattern.compile("[#\\\\/:\\*\\?\\\"<>\\|]");
        Matcher matcher = pattern.matcher(fileName);
        return matcher.replaceAll("");
    }

    private boolean concatListFile(File m3u8Path) {
        try {
            m3u8Path.mkdirs();
            String[] seqFiles = m3u8Path.list((dir, name) -> name.endsWith(".ts"));
            if (seqFiles.length > 0) {
                List<Integer> seqList = new LinkedList<>();
                for (String fileName : seqFiles) {
                    try {
                        seqList.add(Integer.parseInt(FilenameUtils.getBaseName(fileName)));
                    } catch (NumberFormatException ignored) {
                        LOGGER.info(fileName + " is not a seq file skipped.");
                    }
                }
                if (!seqList.isEmpty()) {
                    seqList.sort(Comparator.naturalOrder());
                    int i = 0;
                    long totalSize = 0;
                    OutputStream os = null;
                    for (Integer integer : seqList) {
                        File tsFile = new File(m3u8Path + "/" + integer + ".ts");
                        FileInputStream is = new FileInputStream(tsFile);
                        if (os == null || totalSize >= MAX_RECORD_FILE_SIZE) {
                            File outputFile = null;
                            while (outputFile == null || (outputFile.exists() && outputFile.length() >= MAX_RECORD_FILE_SIZE)) {
                                outputFile = new File(m3u8Path + "/index_" + i++ + ".ts");
                            }
                            if (os != null) {
                                os.close();
                            }
                            os = new FileOutputStream(outputFile, true);
                            totalSize = outputFile.length();
                        }
                        totalSize += IOUtils.copyLarge(is, os);
                        is.close();
                        tsFile.delete();
                    }
                }
                return true;
            }
            return false;
        } catch (Throwable e) {
            LOGGER.error("合并TS文件失败", e);
            return false;
        }
    }
}

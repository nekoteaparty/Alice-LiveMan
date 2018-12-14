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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RecordAndCleanUpJob {

    private static final Logger              LOGGER                    = LoggerFactory.getLogger(RecordAndCleanUpJob.class);
    private static final long                LAST_MODIFIED_TIME_MILLIS = 15 * 60 * 1000;
    @Autowired
    private              MediaHistoryService mediaHistoryService;
    @Autowired
    private              LiveManSetting      liveManSetting;
    @Autowired
    private              OneDriveUtil        oneDriveUtil;

    @Scheduled(cron = "0 0/1 * * * ?")
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
                                createConcatListFile(sourcePath);
                                uploadDir(sourcePath, sourcePath.getName());
                                break;
                            }
                            case "mp4": {
                                uploadDir(sourcePath, sourcePath.getName());
                                break;
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("上传录像和清理任务已完成！");
    }

    private void uploadDir(File sourceFile, final String videoId) {
        MediaHistory mediaHistory = mediaHistoryService.getMediaHistory(videoId);

        if (sourceFile.isDirectory()) {
            LOGGER.info("uploadDir:" + sourceFile.getAbsoluteFile());
            File[] children = sourceFile.listFiles();
            if (children != null && children.length > 0) {
                Arrays.stream(children).parallel().forEach(file -> uploadDir(file, videoId));
            }
        } else if (mediaHistory != null && !String.valueOf(mediaHistory.getChannelName()).equals("null")) {
            if (sourceFile.length() == 0) {
                LOGGER.info("文件[" + sourceFile + "]的长度为0，跳过上传!");
            } else if (!StringUtils.isEmpty(liveManSetting.getOneDriveToken())) {
                for (int i = 0; i < 3; i++) {
                    try {
                        OneFolder recordFolder = oneDriveUtil.getOneFolder("Record");
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH_mm_ss");
                        String fileName = mediaHistory.getChannelName().trim();
                        fileName += "\\/" + dateFormat.format(mediaHistory.getDatetime());
                        fileName += "\\/" + timeFormat.format(mediaHistory.getDatetime()) + "_" + mediaHistory.getVideoId() + "_" + replaceFileName(String.valueOf(mediaHistory.getVideoTitle()).trim());
                        fileName += "\\/" + sourceFile.getName();
                        LOGGER.info("开始上传录像[" + sourceFile.getAbsoluteFile() + "]到[" + fileName + "]...");
                        OneUploadFile oneUploadFile = recordFolder.uploadFile(sourceFile, fileName);
                        oneUploadFile.startUpload();
                        LOGGER.info("录像文件[" + sourceFile.getAbsoluteFile() + "]上传已完成！");
                        break;
                    } catch (Throwable throwable) {
                        LOGGER.error("录像保存失败[" + sourceFile.getAbsoluteFile() + "]，重试(" + (i + 1) + "/3)", throwable);
                    }
                }
            }
        }
        sourceFile.delete();
    }


    private String replaceFileName(String fileName) {
        Pattern pattern = Pattern.compile("[#\\\\/:\\*\\?\\\"<>\\|]");
        Matcher matcher = pattern.matcher(fileName);
        return matcher.replaceAll("");
    }

    private static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            LOGGER.info("deleteDir:" + dir);
            String[] children = dir.list();
            for (String aChildren : children) {
                deleteDir(new File(dir, aChildren));
            }
        }
        dir.delete();
    }


    private boolean createConcatListFile(File m3u8Path) {
        try {
            m3u8Path.mkdirs();
            File[] seqFiles = m3u8Path.listFiles((dir, name) -> name.endsWith(".ts"));
            File m3u8File = new File(m3u8Path + "/list.txt");
            if (seqFiles.length > 0 && !m3u8File.exists()) {
                List<Integer> seqList = new LinkedList<>();
                for (File file : seqFiles) {
                    seqList.add(Integer.parseInt(FilenameUtils.getBaseName(file.getName())));
                }
                seqList.sort(Comparator.naturalOrder());

                StringBuilder sb = new StringBuilder();
                for (Integer seq : seqList) {
                    sb.append("file ");
                    sb.append(seq).append(".ts\n");
                }
                FileUtils.write(m3u8File, sb);
                FileUtils.write(new File(m3u8Path + "/合并分片[Windows].cmd"), "ffmpeg -f concat -i list.txt -c copy index.mkv");
                FileUtils.write(new File(m3u8Path + "/合并分片[Linux].sh"), "ffmpeg -f concat -i list.txt -c copy index.mkv");
                FileUtils.write(new File(m3u8Path + "/播放预览[Windows].cmd"), "ffplay -f concat -i list.txt");
                FileUtils.write(new File(m3u8Path + "/播放预览[Linux].sh"), "ffplay -f concat -i list.txt");
                return true;
            }
            return false;
        } catch (Throwable e) {
            LOGGER.error("创建TS文件列表失败", e);
            return false;
        }
    }
}

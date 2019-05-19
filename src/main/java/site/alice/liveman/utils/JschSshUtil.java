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

package site.alice.liveman.utils;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import site.alice.liveman.model.ServerInfo;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;

@Slf4j
public class JschSshUtil implements Closeable {
    private String  username;
    private String  password;
    private String  host;
    private Integer port;
    private Session session;

    private InputStream  in;
    private OutputStream out;
    private OutputStream err;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public JschSshUtil(ServerInfo serverInfo) {
        this.username = serverInfo.getUsername();
        this.password = serverInfo.getPassword();
        this.host = serverInfo.getAddress();
        this.port = serverInfo.getPort();
    }

    public InputStream getIn() {
        return in;
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    public OutputStream getOut() {
        return out;
    }

    public void setOut(OutputStream out) {
        this.out = out;
    }

    public OutputStream getErr() {
        return err;
    }

    public void setErr(OutputStream err) {
        this.err = err;
    }

    /**
     * 开启session
     *
     * @return
     * @throws JSchException
     */
    public Session openSession() throws JSchException {
        JSch jsch = new JSch();
        if (session == null || !session.isConnected()) {
            Session session = jsch.getSession(username, host, port);
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            session.setInputStream(in);
            session.setOutputStream(out);
            session.setConfig(sshConfig);
            session.setPassword(password);
            session.connect(2000);
            this.session = session;
        }
        return session;
    }

    public void transferFile(String sourceFile, String distFile) throws JSchException, SftpException {
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) openSession().openChannel("sftp");
            log.info("transferFile local " + sourceFile + " to remote@" + channel.getSession().getHost() + " " + distFile);
            channel.connect(2000);
            mkdirs(channel, FilenameUtils.getFullPath(distFile));
            channel.put(sourceFile, FilenameUtils.getName(distFile));
            channel.chmod(509, FilenameUtils.getName(distFile));
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void mkdirs(ChannelSftp sftp, String distPath) throws SftpException {
        sftp.cd("/");
        String[] folders = distPath.split("/");
        for (String folder : folders) {
            if (folder.length() > 0) {
                try {
                    sftp.cd(folder);
                } catch (SftpException e) {
                    sftp.mkdir(folder);
                    sftp.cd(folder);
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString("/usr/bin/ffmpeg".split("/")));
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                session.disconnect();
            }
        } catch (Throwable ignore) {

        }
    }
}

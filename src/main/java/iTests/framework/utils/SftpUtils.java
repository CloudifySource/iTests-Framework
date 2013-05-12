/**
 * 
 */
package iTests.framework.utils;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHExec;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;


/**
 * @author sagib
 *
 */
public class SftpUtils {

	private static final long SSH_COMMAND_TIMEOUT = 25000;
	private static final int CONNECTION_TEST_INTERVAL_MILLIS = 5000;
	private static final int CONNECTION_TEST_CONNECT_TIMEOUT_MILLIS = 10000;



	/*******
	 * Checks if a tcp connection to a remote machine and port is possible.
	 * 
	 * @param ip
	 *            remote machine ip.
	 * @param port
	 *            remote machine port.
	 * @param retries
	 *            number of retries.
	 * @return true if connection succeeded, false otherwise.
	 */
	public static boolean checkConnection(final String ip, final int port,
			final int retries) {

		InetSocketAddress addr = new InetSocketAddress(ip, port);
		for (int i = 0; i < retries; ++i) {
			final Socket sock = new Socket();
			try {
				LogUtils.log("Checking connection to: " + addr);
				sock.connect(addr, CONNECTION_TEST_CONNECT_TIMEOUT_MILLIS);

				return true;
			} catch (final IOException e) {
				// ignore
			} finally {
				if (sock != null) {
					try {
						sock.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}

			try {
				Thread.sleep(CONNECTION_TEST_INTERVAL_MILLIS);
			} catch (final InterruptedException e) {
				// ignore
			}
		}

		return false;

	}

	/****
	 * Copies files from local dir to remote dir.
	 * 
	 * @param host
	 *            host name or ip address of remote machine.
	 * @param username
	 *            ssh username of remote machine.
	 * @param password
	 *            ssh password of remote machine.
	 * @param srcDir
	 *            local directory.
	 * @param toDir
	 *            remote directory.
	 * @param keyFile an ssh key file if unused put null
	 * @throws java.io.IOException
	 *             in case of an error during file transfer.
	 */
	public static void copyFiles(final String host, final String username,
			final String password, final String srcDir, final String toDir,
			String keyFile) throws IOException {

		final FileSystemOptions opts = new FileSystemOptions();

		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(
				opts, "no");

		if (keyFile != null && keyFile.length() > 0) {
			final File temp = new File(keyFile);
			if (!temp.exists()) {
				throw new IllegalArgumentException(
						"KeyFile "
						+ keyFile
						+ " that was passed in the installation Details does not exist");
			}
			SftpFileSystemConfigBuilder.getInstance().setIdentities(opts,
					new File[] { temp });
		}

		final FileSystemManager mng = VFS.getManager();

		final FileObject localDir = mng.resolveFile("file:" + srcDir);

		String scpTarget = null;
		if (password != null && password.length() > 0) {
			scpTarget = "sftp://" + username + ":" + password + "@" + host
			+ toDir;
		} else {
			scpTarget = "sftp://" + username + "@" + host + toDir;
		}
		final FileObject remoteDir = mng.resolveFile(scpTarget, opts);

		LogUtils.log("Copying files to: " + scpTarget + " from local dir: "
                + localDir.getName().getPath());
		try {
			remoteDir.copyFrom(localDir, new FileSelector() {

				public boolean includeFile(final FileSelectInfo fileInfo)
				throws Exception {
					final FileObject remoteFile = mng.resolveFile(
							remoteDir,
							localDir.getName().getRelativeName(
									fileInfo.getFile().getName()));
					if (remoteFile.exists() && remoteFile.isHidden()) {//this is meant to not copy .svn folders
						LogUtils.log(fileInfo.getFile().getName().getBaseName()
                                + " is an hidden file");
						return false;

					}
					else return true;

					/*if (fileInfo.getFile().getType().equals(FileType.FILE)) {
						final long remoteSize = remoteFile.getContent()
								.getSize();
						final long localSize = fileInfo.getFile().getContent()
								.getSize();
						final boolean res = (localSize != remoteSize);
						if (res) {
							LogUtils.log(fileInfo.getFile().getName()
									.getBaseName()
									+ " different on server");
						}
						return res;
					}
					return false;*/

				}

				public boolean traverseDescendents(final FileSelectInfo fileInfo)
				throws Exception {
					return false;
				}
			});
		} finally {

			mng.closeFileSystem(remoteDir.getFileSystem());

		}
	}

	public static String createMacString(final String[] macs) {
		final StringBuilder builder = new StringBuilder();
		for (final String mac : macs) {
			builder.append(mac).append(';');
		}
		return builder.toString();
	}

	public static Process executeScriptProcess(final String scriptLocation,
			final String macString, final String myIp) throws IOException,
			InterruptedException {
		LogUtils.log("Executing script: " + scriptLocation + " " + myIp + " "
                + macString);
		final ProcessBuilder pb = new ProcessBuilder(scriptLocation, myIp,
				macString);
		final Process p = pb.start();
		p.waitFor();
		return p;
	}

	public static void sshCommand(final String host, final String command, final String username, final String password,
			final String keyFile) {

		final SSHExec task = new SSHExec();
		task.setCommand(command);
		task.setHost(host);
		task.setTrust(true);
		task.setUsername(username);

		task.setTimeout(SSH_COMMAND_TIMEOUT);
		if (keyFile != null) {
			task.setKeyfile(keyFile);
		}
		if (password != null) {
			task.setPassword(password);
		}

		try {
			LogUtils.log("Executing command: " + command + " on " + host);
			task.execute();
		} catch (final BuildException e) {
			// There really should be a better way to check that this is
			// a timeout
			if ((e.getMessage() != null) && e.getMessage().contains("Timeout")) {
				LogUtils.log("ssh execution ends with timeout: " + e.getCause());
			} else {
				throw new IllegalStateException("Command " + command + " failed to execute: " + e.getMessage(), e);
			}

		}
	}
}

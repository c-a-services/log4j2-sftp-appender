package io.github.c_a_services.log4j2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.PublicKey;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteFile.RemoteFileOutputStream;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

/**
 *
 */
@Plugin(name = "DailyFileSftpAppender", category = "Core", elementType = "appender", printObject = true)
public class DailyFileSftpAppender extends AbstractAppender {

	private static final StatusLogger LOGGER = StatusLogger.getLogger();

	private String userName;
	private String privateKeyResource;
	private String passPhrase;
	private String filePattern;
	private String hostName;
	private String pathName;

	private String publicKeyResource;

	private transient SFTPClient sftpClient;

	private transient SSHClient ssh;

	/**
	 * @param aPublicKeyResource TODO
	 *
	 */
	protected DailyFileSftpAppender(String aName, Filter aFilter, Layout<? extends Serializable> aLayout, boolean ignoreExceptions, String aUserName,
			String aPublicKeyResource, String aPrivateKeyResource, String aFilePattern, String aHostName, String aPathName, String aPassPhrase) {
		super(aName, aFilter, aLayout, ignoreExceptions);
		userName = aUserName;
		publicKeyResource = aPublicKeyResource;
		privateKeyResource = aPrivateKeyResource;
		filePattern = aFilePattern;
		hostName = aHostName;
		pathName = aPathName;
		passPhrase = aPassPhrase;

	}

	@PluginFactory
	public static DailyFileSftpAppender createAppender(@PluginAttribute("name") String name, @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
			@PluginElement("Layout") Layout<? extends Serializable> aLayout, //
			@PluginElement("Filters") Filter filter, //
			@PluginElement("Strategy") RolloverStrategy strategy, //
			@PluginAttribute("userName") String aUserName, //
			@PluginAttribute("publicKeyResource") String aPublicKeyResource, //
			@PluginAttribute("privateKeyResource") String aPrivateKeyResource, //
			@PluginAttribute("passPhrase") String aPassPhrase, //
			@PluginAttribute("filePattern") String aFilePattern, //
			@PluginAttribute("hostName") String aHostName, //
			@PluginAttribute("pathName") String aPathName) {

		if (name == null) {
			LOGGER.error("No name provided for StubAppender");
			return null;
		}

		Layout<? extends Serializable> tempLayout;
		if (aLayout == null) {
			tempLayout = PatternLayout.createDefaultLayout();
		} else {
			tempLayout = aLayout;
		}
		return new DailyFileSftpAppender(name, filter, tempLayout, ignoreExceptions, aUserName, aPublicKeyResource, aPrivateKeyResource, aFilePattern,
				aHostName, aPathName, aPassPhrase);
	}

	/**
	 *
	 */
	@Override
	public void append(LogEvent aEvent) {
		Serializable tempString = getLayout().toSerializable(aEvent);
		LocalDate tempLocalDateTime = Instant.ofEpochMilli(aEvent.getTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
		String tempFileName = tempLocalDateTime + "-" + filePattern;

		try {
			RemoteFile tempFile = getRemoteFile(tempFileName);
			try (RemoteFileOutputStream tempOut = tempFile.new RemoteFileOutputStream(tempFile.length())) {
				tempOut.write(tempString.toString().getBytes("UTF-8"));
			}
			tempFile.close();
		} catch (IOException e) {
			logError("Retry", e);
			sftpClient = null;
			ssh = null;
			try {
				RemoteFile tempFile = getRemoteFile(tempFileName);
				try (RemoteFileOutputStream tempOut = tempFile.new RemoteFileOutputStream(tempFile.length())) {
					tempOut.write(tempString.toString().getBytes("UTF-8"));
					tempOut.flush();
				}
				tempFile.close();
			} catch (IOException e2) {
				logError("Failure", e2);
				throw new RuntimeException(e2);
			}
		}

	}

	/**
	 * @param aFileName
	 * @throws IOException
	 *
	 */
	private RemoteFile getRemoteFile(String aFileName) throws IOException {
		if (sftpClient == null) {
			if (ssh == null) {
				ssh = new SSHClient();
				//			ssh.loadKnownHosts();
				ssh.addHostKeyVerifier(new HostKeyVerifier() {

					@Override
					public boolean verify(String aHostname, @SuppressWarnings("unused") int aPort, @SuppressWarnings("unused") PublicKey aKey) {
						return aHostname.equals(hostName);
					}
				});
				ssh.connect(hostName);
				KeyProvider tempKey = loadKey(ssh);

				ssh.authPublickey(userName, tempKey);
			}
			try {

				sftpClient = ssh.newSFTPClient();
			} catch (IOException | RuntimeException e) {
				logError("Error", e);
				throw new RuntimeException(e);
			}
		}
		Set<OpenMode> tempOpenModes = new HashSet<>();
		tempOpenModes.add(OpenMode.WRITE);
		tempOpenModes.add(OpenMode.APPEND);
		tempOpenModes.add(OpenMode.CREAT);
		RemoteFile tempFile = sftpClient.open(pathName + aFileName, tempOpenModes);
		return tempFile;
	}

	/**
	 * @param aSsh
	 * @throws IOException
	 *
	 */
	private KeyProvider loadKey(SSHClient aSsh) throws IOException {
		File tempTempPrivateFile = createTempKeyFile(privateKeyResource, null);
		String tempPublicKeyExtension = publicKeyResource.substring(privateKeyResource.length());
		// see net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil.detectKeyFileFormat(File)
		File tempTempPubFile = createTempKeyFile(publicKeyResource, new File(tempTempPrivateFile.getAbsolutePath() + tempPublicKeyExtension));
		if (LOGGER.isDebugEnabled()) {
			logDebug("tempTempPub=" + tempTempPubFile + " for " + publicKeyResource);
		}
		KeyProvider tempKey;
		if (passPhrase == null) {
			tempKey = aSsh.loadKeys(tempTempPrivateFile.getAbsolutePath());
		} else {
			tempKey = aSsh.loadKeys(tempTempPrivateFile.getAbsolutePath(), passPhrase);
		}
		// private file needs to be available during logon.
		//tempTempPrivateFile.delete();
		tempTempPubFile.delete();
		if (LOGGER.isDebugEnabled()) {
			logDebug("Loaded " + tempKey + " for " + privateKeyResource);
		}
		return tempKey;
	}

	/**
	 *
	 */
	private void logDebug(String aString) {
		LOGGER.debug("DailyFileSftpAppender:" + aString);
	}

	/**
	 * @param aResource
	 * @throws IOException
	 *
	 */
	private File createTempKeyFile(String aResource, File aTargetFile) throws IOException {
		File tempFile = aTargetFile;
		if (tempFile == null) {
			tempFile = File.createTempFile("DailyFileSftpAppender", aResource.replace('/', '_'));
		}
		tempFile.deleteOnExit();
		InputStream tempIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(aResource);
		if (tempIn == null) {
			throw new IllegalArgumentException(privateKeyResource + " not found.");
		}
		try (FileOutputStream tempOut = new FileOutputStream(tempFile)) {
			byte[] tempBuff = new byte[1024 * 16];
			int tempRead = tempIn.read(tempBuff);
			while (tempRead > 0) {
				tempOut.write(tempBuff, 0, tempRead);
				tempRead = tempIn.read(tempBuff);
			}
			tempIn.close();
		}
		return tempFile;
	}

	/**
	 *
	 */
	private void logError(String aString, Exception aE) {
		LOGGER.error("DailyFileSftpAppender:" + aString, aE);
	}

	/**
	 * TODO call on Shutdown.
	 *
	 * @throws IOException
	 *
	 */
	public void shutdown() {
		if (sftpClient != null) {
			try {
				sftpClient.close();
			} catch (IOException e) {
				logError("Ignore", e);
			}
		}
		sftpClient = null;
		if (ssh != null) {
			try {
				ssh.close();
			} catch (IOException e) {
				logError("Ignore", e);
			}
		}
		ssh = null;
	}

	// TOOD ssh.disconnect()

}

package io.github.c_a_services.log4j2;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoField;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 *
 */
@Plugin(name = "DailyFileSftpAppender", category = "Core", elementType = "appender", printObject = true)
public class DailyFileSftpAppender extends AbstractAppender {

	private String privateKeyResource;
	private String filePattern;
	private String hostName;

	/**
	 * @param aPrivateKeyResource
	 * @param aFilePattern
	 *
	 */
	protected DailyFileSftpAppender(String aName, Filter aFilter, Layout<? extends Serializable> aLayout, boolean ignoreExceptions, String aPrivateKeyResource,
			String aFilePattern, String aHostName) {
		super(aName, aFilter, aLayout, ignoreExceptions);
		privateKeyResource = aPrivateKeyResource;
		filePattern = aFilePattern;
		hostName = aHostName;

	}

	@PluginFactory
	public static DailyFileSftpAppender createAppender(@PluginAttribute("name") String name, @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
			@PluginElement("Layout") Layout<? extends Serializable> aLayout, //
			@PluginElement("Filters") Filter filter, //
			@PluginAttribute("privateKeyResource") String aPrivateKeyResource, //
			@PluginAttribute("filePattern") String aFilePattern, //
			@PluginAttribute("hostName") String aHostName) {

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
		return new DailyFileSftpAppender(name, filter, tempLayout, ignoreExceptions, aPrivateKeyResource, aFilePattern, aHostName);
	}

	/**
	 *
	 */
	@Override
	public void append(LogEvent aEvent) {
		Serializable tempString = getLayout().toSerializable(aEvent);
		LocalDate tempLocalDateTime = LocalDate.ofEpochDay(Instant.ofEpochMilli(aEvent.getTimeMillis()).get(ChronoField.EPOCH_DAY));
		String tempFileName = tempLocalDateTime + "-" + getFilePattern();
		System.out.println("tempFileName=" + tempFileName);
		System.out.println("tempString=" + tempString);
	}

	/**
	 * @see #filePattern
	 */
	public String getFilePattern() {
		return filePattern;
	}

	/**
	 * @see #filePattern
	 */
	public void setFilePattern(String aFilePattern) {
		filePattern = aFilePattern;
	}

}

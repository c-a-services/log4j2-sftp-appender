## log4j2-sftp-appender
Append the log-output from log4j2.xml to sftp server

### Configuration

`log4j2.xml` file:

Add the **import** to the root element:

```xml
<Configuration status="warn" packages="io.github.c_a_services.log4j2">
```

Add the **appender**:

```xml
	<Appenders>
		<DailyFileSftpAppender
			name="sftpAppender"
			filePattern="${date:HHmmssSSS}-mule.log"
			hostName="ftp.yourdnsservername.com"
			pathName="/${sys:stage.name:-nostage}/"
			userName="ftpUserName"
			publicKeyResource="secret/ftpUserName.pub"
			privateKeyResource="secret/ftpUserName"
			passPhrase="yourPassPhrase">
			<PatternLayout pattern="%d %-5p %c{1} - %m [%t]%n" />
         <DefaultRolloverStrategy max="10"/>
		</DailyFileSftpAppender>
	</Appenders>
```
* **name**: will needs to be referenced via `<AppenderRef ref="sftpAppender" />` later in the file
* **filepatter**: suffix of the files (date will be placed in the beginning always)
* **hostName**: ftp hostname
* **pathName**: path on that host (here prefixing with system property `stage.name`)
* **userName**: userName to connect with
* **publicKeyResource**: resource in your classpath (can be generated via `ssh-keygen -f ftpUserName -t rsa -b 4092 -N yourPassPhrase`)
* **privateKeyResource**: resource in your classpath
* **PatternLayout**: Any patternlayout
* **DefaultRolloverStrategy**: Days to keep logfiles.

for above example the resourepath looks like:
  `/your-mule-application/src/main/resources/secret/ftpUserName`
  `/your-mule-application/src/main/resources/secret/ftpUserName.pub`

Further details, see [log4j2-configuration](https://logging.apache.org/log4j/2.x/manual/configuration.html)

#### Dependencies

Dependencies are marked `provided` and so either needs to be available in your server or added manually:

##### this:

		<dependency>
			<groupId>io.github.c-a-services</groupId>
			<artifactId>log4j2-sftp-appender</artifactId>
			<version>2018.11.1</version>
		</dependency>
(for newest Version see [pom.xml](https://github.com/c-a-services/log4j2-sftp-appender/blob/master/pom.xml))

##### log4j2:

```xml
		<dependency>
			<groupId>org.apache.logging.log4j</groupId>
			<artifactId>log4j-core</artifactId>
			<version>2.11.1</version>
		</dependency>
```
(for newest Version see [log4j2-maven-artifacts](https://logging.apache.org/log4j/2.x/maven-artifacts.html))

##### sshj:

```xml
		<dependency>
			<groupId>com.hierynomus</groupId>
			<artifactId>sshj</artifactId>
			<version>0.26.0</version>
		</dependency>
```
(for newest Version see [sshj](https://github.com/hierynomus/sshj))

---
---
For changing this file see [Markdown-Cheatsheet](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)
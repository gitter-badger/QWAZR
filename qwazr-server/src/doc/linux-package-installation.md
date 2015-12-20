Linux installation
==================

This document describes installing QWAZR on a GNU/Linux operating system.

Downloading
-----------

Pre-releases (nightly builds) are available there:
[download link](http://download.qwazr.com)


Each build contains the following archives:
- qwazr-server-1.0.1-1.noarch.rpm: package for CentOS, Fedora, RedHat
- qwazr-server-1.0.1-SNAPSHOT.deb: package for Ubuntu and Debian
- qwazr-server-1.0.1-SNAPSHOT-exec.jar: Executable JAR

Installation
------------

Java™ 1.8 must be installed.

On a RPM-based distribution:

```shell
rpm -ivh qwazr-server-1.0.1-1.noarch.rpm
```

On a Debian/Ubuntu-based distribution:

```shell
dpkg -i qwazr-server-1.0.1-SNAPSHOT.deb
```

What gets installed
-------------------

A user named **qwazr** is created. The daemon will be executed by this user.
You should never start **qwazr** as a root user.

The following files and directories are created:
- /etc/init.d/qwazr : the init script,
- /etc/qwazr.conf : the main configuration file,
- /usr/share/qwazr : contains the executable JAR file,
- /var/log/qwazr: contains the log files,
- /var/lib/qwazr: the directory that contains the user data.

Configuration
-------------

This configuration file is located in /etc/qwazr.conf.

Refer to the embedded comments to get the latest information.
You can specify within the IP address and TCP ports that the server will listen to.

The server uses two TCP ports with the following default value:
- 9091 is used for the web service,
- 9090 for the web application server.

You may edit this file to change this according to your requirements.

```shell
# The directory containing the data
QWAZR_DATA=/var/lib/qwazr

# The hostname or IP address the server listen to
LISTEN_ADDR=0.0.0.0

# the public hostname or IP address used for external and nodes communication
PUBLIC_ADDR=

# The TCP port used by the web applications
WEBAPP_PORT=9090

# The TCP port used by the web services
WEBSERVICE_PORT=9091

# Any JAVA option. This part is often used to allocate more memory
JAVA_OPTS="-Xms1G -Xmx1G -XX:+UseG1GC \
    -Djava.net.preferIPv4Stack=true -XX:NewSize=512m -XX:MaxNewSize=512m -XX:SurvivorRatio=6"
```

Start & stop
------------

The init.d script has been installed in /etc/init.d/qwazr.
Usually, you will use the **service** command to manage qwazr.

```shell
    service qwazr start
```

Have a look at the log (/var/log/qwazr/server.out) to check that everything is okay.

You can also check the Cluster web service:

```
http://localhost:9091/cluster
```
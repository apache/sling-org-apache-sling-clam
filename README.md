[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-clam/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-clam/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-clam/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-clam/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-clam&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-clam)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-clam&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-clam)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.clam.svg)](https://www.javadoc.io/doc/org.apache.sling/org-apache-sling-clam)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.clam/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.clam%22) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Clam

This module is part of the [Apache Sling](https://sling.apache.org) project.

This module provides support for Clam in Sling.


## Finding data to scan for malware

`NodeDescendingJcrPropertyDigger` starts descending from a given root path, digs properties based on type, path and length and creates scan jobs.

`NodeObservingJcrPropertyDigger` observes Oak's NodeStore, digs properties based on type, path and length and creates scan jobs.

**NOTE**: Ensure to exclude scan jobs in `/var/eventing` and scan results in `/var/clam/results` from scanning.


## Scanning data

`JcrPropertyScanJobConsumer` processes scan jobs by reading property values from JCR, sends data to Clam service for scanning and invokes optional scan result handlers.

The service requires read-only access to all paths to be scanned which can be allowed by adding the service user mapping `org.apache.sling.clam=sling-readall`.


## Handling of scan results

`EventPublishingScanResultHandler` publishes scan results via OSGi Event Admin Service.

`ResourcePersistingScanResultHandler` persists scan results via ResourceResolver in JCR. The result handler requires write access to a configurable root path for subservice `result-writer`.


## HTTP API

Scanning all binaries and strings in AEM Assets:

    curl -v -u username:password -F path=/content/dam -F pattern=^/.*$ -F propertyTypes[]=Binary -F propertyTypes[]=String http://localhost:4502/system/clam-jcr-scan

Listening to Sling Clam events:

    curl -v -u username:password http://localhost:4502/system/clam-events


## Useful Patterns

    ^\/(?!.*\/rep:principalName)(.*)$


## Integration Tests

Integration tests require a running Clam daemon. By default a Docker container (`mk0x/docker-clamav:alpine`) is started via [Testcontainers](https://www.testcontainers.org/) and local [Docker](https://www.docker.com/) Engine to provide the Clam daemon.


### Use external Clam daemon

To disable *Testcontainers* and use an external Clam daemon set `clamd.testcontainer` to `false`:

    mvn clean install -Dclamd.testcontainer=false

To override default Clam daemon host `localhost` and port `3310` set `clamd.host` and `clamd.port`:

    mvn clean install -Dclamd.testcontainer=false -Dclamd.host=localhost -Dclamd.port=3310

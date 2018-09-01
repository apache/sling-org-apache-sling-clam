[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

[![Build Status](https://builds.apache.org/buildStatus/icon?job=sling-org-apache-sling-clam-1.8)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-clam-1.8) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-clam-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-clam-1.8/test_results_analyzer/) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.clam/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.clam%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.clam.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.clam) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Clam

This module is part of the [Apache Sling](https://sling.apache.org) project.

This module provides support for Clam in Sling.


## Finding data to scan for malware

`JcrPropertyDigger` observes Oak's NodeStore, digs properties based on type, path and length and creates scan jobs.

**NOTE**: Ensure to exclude scan jobs in `/var/eventing` and scan results in `/var/clam/results` from scanning.


## Scanning data

`JcrPropertyScanJobConsumer` processes scan jobs by reading property values from JCR, sends data to Clam service for scanning and invokes optional scan result handlers.

The service requires read-only access to all paths to be scanned which can be allowed by adding the service user mapping `org.apache.sling.clam=sling-readall`.


## Handling of scan results

`EventPublishingScanResultHandler` publishes scan results via OSGi Event Admin Service.

`ResourcePersistingScanResultHandler` persists scan results via ResourceResolver in JCR. The result handler requires write access to a configurable root path for subservice `result-writer`.


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


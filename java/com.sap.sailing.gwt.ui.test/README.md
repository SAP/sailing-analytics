# Sailing GWT UI tests

Run `Refresher Tests.launch` in Eclipse to execute the refresher regression tests on Java 8.
The launch uses the client-side `gwt-user.jar` and the required project outputs. It excludes
`gwt-dev.jar` and `gwt-servlet.jar`, whose GWT 2.12 `GWTBridge` class requires Java 11 and is
not needed for these plain-JVM tests. `MarkedAsyncCallback` already supports running without
the GWT bridge.

The tests control completion of each asynchronous load explicitly, covering overlapping
recipient requests, deduplication, registration snapshots, cache reuse, reentrant reloads,
and recovery after failure.

Before the fix, seven tests fail: overlapping requests lose recipients, a registered
additional recipient can be skipped, identical recipients can be filled twice, fill-all
uses registrations at completion instead of request time, and a failed load prevents retries.
The reentrant-reload test passes both before and after the fix as a baseline.

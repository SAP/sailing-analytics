# Sailing GWT UI tests

These regression tests are for local Eclipse execution only. This project intentionally has
no Maven POM and is not listed in `java/pom.xml`, so the Maven/CI build does not compile or run it.

Import this directory using **File > Import > General > Existing Projects into Workspace**
alongside the Sailing Analytics projects. Configure a Java 8 JDK for the `JavaSE-1.8` execution
environment, then right-click `Refresher Tests.launch` and select **Run As > JUnit Test**.
The launch uses the client-side `gwt-user.jar` and the required project outputs. It excludes
`gwt-dev.jar` and `gwt-servlet.jar`, whose GWT 2.12 `GWTBridge` class requires Java 11 and is
not needed for these plain-JVM tests. With this restricted classpath, `MarkedAsyncCallback`
uses its fallback for an unavailable GWT runtime. Use the supplied launch configuration:
the Java 8 Tycho/OSGi test runtime includes the incompatible bridge and fails with
`UnsupportedClassVersionError`, which that fallback does not handle.

The tests control completion of each asynchronous load explicitly, covering overlapping
recipient requests, deduplication, registration snapshots, cache reuse, reentrant reloads,
and recovery after failure.

Before the fix, seven tests fail: overlapping requests lose recipients, a registered
additional recipient can be skipped, identical recipients can be filled twice, fill-all
uses registrations at completion instead of request time, and a failed load prevents retries.
The reentrant-reload test passes both before and after the fix as a baseline.

# Updating Jetty in the Target Platform

## Background

The target platform references Jetty bundles via a p2 repository. Historically
this was mirrored from the official Eclipse-hosted site at
`https://download.eclipse.org/jetty/updates/jetty-bundles-9.x/` using the
`createAndUploadJettyP2Repository.sh` script.

When a new Jetty release is **not yet published** to that Eclipse p2 site (as
happened with 9.4.58), the scripts described below let you build an equivalent
p2 repository yourself, directly from the artifacts on **Maven Central**.

This works because the Jetty jars on Maven Central are already proper OSGi
bundles (`Bundle-SymbolicName`, `Bundle-Version`, etc.) and their `-sources.jar`
files carry `Eclipse-SourceBundle` headers.

## Prerequisites

| Tool | Purpose |
|------|---------|
| `eclipse` on `PATH` | Provides the p2 publisher (`org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher`). Any Eclipse IDE or Equinox SDK installation will have this. |
| `curl` | Downloads jars from Maven Central. |
| `wget` | Used by the upload script to fetch JSP bundles into `target-base`. |
| `ssh` / `scp` | Uploads the finished p2 repository to `sapsailing.com`. |

## Step-by-step guide

### 1. Determine the new version string

Find the latest 9.4.x release on Maven Central:

```
curl -s https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-server/ \
  | grep -o '9\.4\.[0-9]*\.v[0-9]*' | sort -V | tail -1
```

The version string looks like `9.4.58.v20250814`.

### 2. Build the p2 repository

```bash
cd java/com.sap.sailing.targetplatform/scripts
./createJettyP2RepoFromMavenCentral.sh 9.4.58.v20250814
```

This will:

1. Download all 41 Jetty-versioned bundles + their source jars from Maven
   Central into a temporary staging directory.
2. Download 4 static (non-Jetty-versioned) bundles that also ship with the
   official Eclipse Jetty p2 repository (see
   [What's in the p2 repository](#what-s-in-the-p2-repository) below).
3. Generate two Eclipse features wrapping these bundles:
   - `org.eclipse.jetty.bundles.f` — the main feature
   - `org.eclipse.jetty.bundles.f.source` — the source feature
4. Run the Eclipse p2 `FeaturesAndBundlesPublisher` to produce a valid p2
   repository.

The result is at `/tmp/jetty-p2-repo-<VERSION>/repository`.

> **Tip:** You can test with the local repo before uploading. Point the
> `<repository location="..."/>` in your `.target` file to
> `file:/tmp/jetty-p2-repo-<VERSION>/repository` and reload the target
> platform.

### 3. Upload to https://download.eclipse.org/sailing-analytics/p2/

There is a Github Actions workflow at .github/workflows/build-jetty-p2.yml which runs the process described
above, using the createJettyP2RepoFromMavenCentral.sh, and uploads the p2 repo content,
using the necessary secrets from repository / organization secrets from
https://github.com/eclipse-sailing-analytics to that folder.

### 4. Activate

```bash
./activateJettyP2Repo.sh 9.4.58.v20250814
```

This will:

1. Update the Jetty version and repository URL in both target definition files
   (`race-analysis-p2-remote.target`, `race-analysis-p2-local.target`).
2. Update the Jetty version in the feature files
   (`com.sap.sse.feature.runtime/feature.xml`,
   `com.sap.sailing.targetplatform.base/features/target-base/feature.xml`).
3. Replace the `apache-jsp` and `jetty-osgi-boot-jsp` jars (+ sources) in
   `com.sap.sailing.targetplatform.base/plugins/target-base` with the new
   version and stage them in git.

### 5. Rebuild and upload the base p2 repository

```bash
./createLocalBaseP2repository.sh
./uploadRepositoryToServer.sh
```

### 6. Reload the target platform

In Eclipse, open the `.target` file and click **Reload** / **Set as Active
Target Platform**.

## What's in the p2 repository

The generated p2 repository contains **86 JARs** (bundles + source bundles)
and is a **strict superset** of the 81-JAR repository that the official Eclipse
Jetty p2 site publishes.

The bundles fall into two categories:

### Jetty-versioned bundles (41 bundles, each with a source jar = 82 JARs)

These are downloaded from Maven Central and versioned with the Jetty release
(e.g. `9.4.58.v20250814`).  They are defined in the `BUNDLES` variable in the
script.

| Category | Bundles |
|----------|---------|
| Core | `jetty-server`, `jetty-servlet`, `jetty-servlets`, `jetty-http`, `jetty-io`, `jetty-util`, `jetty-util-ajax`, `jetty-xml`, `jetty-security`, `jetty-webapp`, `jetty-deploy`, `jetty-client`, `jetty-continuation`, `jetty-proxy`, `jetty-rewrite` |
| Auth | `jetty-jaas`, `jetty-jaspi` (BSN: `security.jaspi`), `jetty-annotations`, `jetty-jmx`, `jetty-jndi`, `jetty-plus` |
| ALPN / HTTP/2 | `jetty-alpn-client`, `jetty-alpn-server`, `http2-client`, `http2-http-client-transport` (BSN: `http2.client.http`), `http2-common`, `http2-hpack`, `http2-server` |
| WebSocket | `websocket-api`, `websocket-client`, `websocket-common`, `websocket-server`, `websocket-servlet`, `javax-websocket-client-impl` (BSN: `websocket.javax.websocket`), `javax-websocket-server-impl` (BSN: `websocket.javax.websocket.server`) |
| OSGi integration | `jetty-osgi-boot`, `jetty-osgi-boot-warurl`, `jetty-osgi-boot-jsp`, `jetty-httpservice` (BSN: `osgi.httpservice`), `jetty-osgi-alpn` (BSN: `osgi.alpn.fragment`) |
| JSP | `apache-jsp` (BSN: `jetty.apache-jsp`) |

### Static bundles (4 JARs)

These ship with the official Eclipse Jetty p2 repository but have their own
fixed version, independent of the Jetty release.  They are defined in the
`STATIC_BUNDLES` variable in the script and are downloaded from the previous
Jetty p2 repository on `sapsailing.com`.

| Bundle | Version | Notes |
|--------|---------|-------|
| `javax.security.auth.message` | `1.0.0.v201108011116` | JASPIC 1.0 API (Eclipse Orbit). Not imported by any project code. |
| `javax.security.auth.message.source` | `1.0.0.v201108011116` | Source for the above. |
| `org.eclipse.jetty.osgi-servlet-api` | `3.1.0.M3` | Jetty's repackaged Servlet 3.1 API. Also in `target-base/plugins`. |
| `org.eclipse.jetty.osgi-servlet-api.source` | `3.1.0.M3` | Source for the above. |

These bundles are **optional** — if the download fails (e.g. because the old
p2 repo URL is no longer available), the script continues with a warning.  Both
`javax.security.auth.message` and `osgi-servlet-api` are also provided by
`target-base/plugins`, so the target platform will still resolve correctly.

### Comparison with the official Eclipse Jetty p2 repository

The old Eclipse-hosted repo (e.g. the 9.4.57 version mirrored at
`https://p2.sapsailing.com/p2/jetty-9.4.57.v20241219/`) contained **81 JARs**.
The new repo contains **86 JARs** — a strict superset.  The 5 extras are source
or main JARs that the old repo inconsistently omitted:

| Extra JAR in new repo | Explanation |
|-----------------------|-------------|
| `org.eclipse.jetty.apache-jsp` | Old repo only shipped the `.source`; the main jar was in `target-base`. |
| `org.eclipse.jetty.osgi.boot.jsp` | Same — old repo only had `.source`; main jar was in `target-base`. |
| `org.eclipse.jetty.jaas.source` | Old repo shipped `jaas` without its source. |
| `org.eclipse.jetty.osgi.alpn.fragment.source` | Old repo shipped the fragment without its source. |
| `org.eclipse.jetty.security.jaspi` | Old repo only shipped the `.source`. |

Having both the main and source jar for every bundle is cleaner and harmless.

## Applying to future versions

Replace `9.4.58.v20250814` with the new version string in steps 2 and 3. Both
scripts accept the version as their first argument and default to
`9.4.58.v20250814` if omitted.

If the new Jetty release adds or removes bundles, edit the `BUNDLES` list at the
top of `createJettyP2RepoFromMavenCentral.sh`. Each line has the format:

```
<maven-group-path>|<artifact-id>|<osgi-bundle-symbolic-name>
```

Note that the Maven artifact ID and the OSGi `Bundle-SymbolicName` often differ.
You can verify the OSGi symbolic name of any jar with:

```bash
unzip -p some-jetty-bundle.jar META-INF/MANIFEST.MF | grep Bundle-SymbolicName
```

## Script overview

| Script | What it does |
|--------|--------------|
| `createJettyP2RepoFromMavenCentral.sh` | Downloads Jetty bundles from Maven Central and builds a local p2 repository. |
| `activateJettyP2Repo.sh` | Updates all version references in the workspace. |
| `createAndUploadJettyP2Repository.sh` | *(legacy)* Mirrors an existing Eclipse-hosted p2 site. Use this when the version **is** available at `download.eclipse.org`. |
| `createLocalBaseP2repository.sh` | Builds the local `target-base` p2 repository via Tycho. |
| `uploadRepositoryToServer.sh` | Uploads the `target-base` p2 repository to `sapsailing.com`. |

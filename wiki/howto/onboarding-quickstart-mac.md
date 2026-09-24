# Onboarding Quickstart (for Mac)

A **self-contained, step-by-step** setup guide for macOS. Work through it top to bottom.

This is a condensed guide meant to get you set up without looking elsewhere. The full [[Onboarding|wiki/howto/onboarding]] document remains the authoritative reference for all platforms and holds the background, edge cases and troubleshooting left out here; where that extra detail helps, individual steps link into the matching section. If anything here ever lags behind that document, the full onboarding document wins.

> **⚠️ This quickstart targets the SAPJVM 8 + x86_64 Eclipse setup.** SAPJVM 8 is only available for **Intel (x86_64)**, so Eclipse must also be the **x86_64** build. On Apple Silicon that build runs through **Rosetta 2**.
>
> **Important for Apple Silicon Macs:** Apple is phasing out Rosetta 2. It remains available through macOS 26 and macOS 27, but from **macOS 28** onward it is reduced to a limited subset (mainly older games). **From macOS 28 this SAPJVM + x86_64 path may therefore no longer work out of the box.** If you are on such a machine, use the native alternative instead: **SDKMAN + Amazon Corretto 8** with the native (aarch64) Eclipse build (see steps 1–2).

## 1. Eclipse

Install **Eclipse IDE for Eclipse Committers, version "2026-06"** — <https://www.eclipse.org/downloads/packages/release/2026-06/r/eclipse-ide-eclipse-committers>. If you are on a Mac and want to use SAPJVM, this has to be the **x86_64** version, because SAPJVM is not available for Apple Silicon Macs and Eclipse's architecture must match the JVM's.

## 2. JDK 1.8 (Java SE 8)

Ideally the **SAPJVM 8**: go to <https://tools.eu1.hana.ondemand.com/#cloud>, scroll to **SAP JVM**, select macOS, and extract the downloaded `.zip` to a location of your choice.

- On Apple Silicon you can instead use [SDKMAN!](https://sdkman.io/) with Amazon Corretto 8:
  ```bash
  curl -s "https://get.sdkman.io" | bash    # then restart your shell
  sdk install java 8.0.472-amzn
  ```
- Set `JAVA8_HOME` to this JDK (required by several build scripts). Optionally set `JAVA_HOME` too if you want it as your default JDK.
- For Gradle builds (currently Gradle 7.6, e.g. the Android build) also install **Java 17** and set `JAVA17_HOME`.

Add to your `~/.zshrc`, e.g.:

```bash
export JAVA8_HOME="$HOME/.sdkman/candidates/java/8.0.472-amzn"   # or your SAPJVM path
export JAVA17_HOME="$HOME/.sdkman/candidates/java/17.0.12-amzn"
```

## 3. Git

macOS usually ships with git; otherwise install it via Homebrew (`brew install git`) or from <https://git-scm.com>.

## 4. Configure git

```bash
git config --global core.autocrlf false           # required, see .gitattributes
git config --global user.name "Your Name"
git config --global user.email your.email@sap.com
```

You will also set up two clearly-named remotes/branches when you clone (step 11) so you never accidentally merge SAP downstream changes into the Eclipse upstream. Background: [[Git repository configuration essentials|wiki/howto/onboarding#git-repository-configuration-essentials]].

## 5. MongoDB

Install the **MongoDB Community Server (at least Release 6.0)** — this is the actual database (`mongod`) that must be running later — together with the `mongosh` shell:

```bash
brew tap mongodb/brew
brew install mongodb-community mongosh
```

Start it as a background service (or run `mongod` manually — see step 15):

```bash
brew services start mongodb-community
```

Optionally, install **Compass** (<https://www.mongodb.com/try/download/compass>) as a graphical client. Note that Compass and `mongosh` are only _clients_ — they connect to the server but do not replace it.

## 6. RabbitMQ

```bash
brew install rabbitmq
```

## 7. Maven

Install **Maven 3.1.1 (or higher)**:

```bash
brew install maven
```

## 8. Ant

```bash
brew install ant
```

Required for building GWT (step 15).

## 9. Forked GWT SDK

Download the **forked GWT SDK 2.12.4** and unpack it to a location of your choice (e.g. `/opt/gwt`). You will register it with Eclipse in step 13.
<https://github.com/eclipse-sailing-analytics/gwt-forward-serialization-rpc/releases/download/gwt-2.12.4/gwt-2.12.4.zip>

## 10. Standalone Android SDK (optional — only for Android app development)

Install the standalone Android SDK and set the `ANDROID_HOME` environment variable to its location. Optionally install Android Studio or IntelliJ IDEA. Details: [[Using Android Studio for App Development|wiki/howto/onboarding#using-android-studio-for-app-development-only-if-youre-working-on-mobile-apps]].

## 11. Clone the repository

```bash
git clone git@github.com:SAP/sailing-analytics.git
cd sailing-analytics
# set up both remotes and clearly-named local branches (see step 4):
git remote add eclipse git@github.com:eclipse-sailing-analytics/sailing-analytics
git remote add sap git@github.com:SAP/sailing-analytics
git fetch eclipse && git fetch sap
git checkout -b sap-main sap/main
git checkout -b eclipse-main eclipse/main
```

Day-to-day work happens against the **upstream** (`eclipse`) repo; the SAP downstream is only for branding/SAP-specific changes.

## 12. Automatic Eclipse plugin installation

For a newly unzipped "2026-06":

- Open Eclipse and select your cloned repository folder as the **workspace**.
- **File ⇒ Import ⇒ Install ⇒ Install Software from File**, click **Next**.
- Click **Browse...** and select `configuration/pluginsForEclipse2026-06.p2f` from the cloned repository.
- Select all selectable plugins (if _Lucene_ can't be selected, ignore it) and click **Next**.
- In the next dialog, choose the radio button **"Update my installation to be compatible with the items being installed"**, then **Next**.
- Skip the installation details (**Next**).
- Accept the licence agreements and click **Finish**.
- In the pop-up shown next (possibly in the background), trust all plugins: click **Select All** then **Trust Selected**.
- Wait for the install to finish (progress is shown bottom-right; this may take several minutes).
- When it finishes, the "Restart Eclipse IDE to apply the software update?" dialog appears — click **Restart Now**.
- _Optional:_ to use the AssistAI plugin, add its MCP server configuration to your coding assistant (e.g. Claude Code in `~/.claude.json`). See <https://github.com/gradusnikov/eclipse-chatgpt-plugin>.

> On Apple Silicon the SAP JVM Profiler plugin can block this installation. If it does, use the Eclipse **x86_64** installer — see [[Java on ARM based Mac troubleshooting|wiki/howto/onboarding#java-on-arm-based-mac-troubleshooting]].

## 13. Tuning the Eclipse installation

- **File ⇒ Import ⇒ General ⇒ Preferences**, click **Browse...**, select `configuration/eclipse-preferences.epf`, click **Finish**, then **Restart**.
- Then go to **Eclipse ⇒ Settings...** and change the following:
  - **GWT ⇒ GWT Settings ⇒ Add…** — select the GWT SDK folder you unpacked in step 9, choose a display name, **OK**, select the added GWT, **Apply and Close**.
  - _Only if you did step 10 (Android SDK):_ **Java ⇒ Build Path ⇒ Classpath Variables** — create a variable `ANDROID_HOME` set to your Android SDK location.
  - **Java ⇒ Code Style ⇒ Formatter ⇒ Import...** — select `java/CodeFormatter.xml` from the cloned repository.
  - **Java ⇒ Installed JREs ⇒ Add... ⇒ Standard VM ⇒ Next** — click **Directory...**, select your Java 8 folder (the `sapjvm_8` folder from step 2, or your Corretto 8 install), **Finish**, select it, **Apply**.
  - **Java ⇒ Installed JREs ⇒ Execution Environments** — select **JavaSE-1.8**, tick your Java 8 JRE, **Apply** (if the JRE isn't listed, open and close the preferences window once).

The full list of optional settings and launch-configuration variables (compiler compliance 1.8, Google Maps, YouTube, ChargeBee, tokens, …) is in [[Tuning the Eclipse Installation|wiki/howto/onboarding#tuning-the-eclipse-installation]].

## 14. Configure Maven to use the correct JRE

- Copy `configuration/maven-settings.xml` (or `configuration/maven-settings-proxy.xml` inside the SAP VPN) **and** the top-level `toolchains.xml` into your `~/.m2` directory.
- In `~/.m2`, rename `maven-settings.xml` to `settings.xml`.
- In `~/.m2/toolchains.xml`, set the paths to your JDKs depending on where you installed them.

Details: [[Maven Setup|wiki/howto/onboarding#maven-setup]].

## 15. Build and run the project

1. **Import the projects:** **Import ⇒ General ⇒ Projects from Folder or Archive** — choose the `java/` subdirectory of your cloned repo as the source. Make sure to enable **"Search for nested projects"**, and explicitly **do not** use "Smart Import".
2. **Window ⇒ Preferences ⇒ Plug-in Development ⇒ Target Platform** — select **Race Analysis Target** (`com.sap.sailing.targetplatform/definitions/race-analysis-p2-remote.target`) and wait until it fully resolves.
3. **Project ⇒ Clean** to run a clean build.
4. One-time, run each of these launch configurations once and stop them after a successful start: **GWT Dashboards SDM**, **GWT Security SDM**, **GWT xdStorage Sample SDM**.
5. **Run:** make sure MongoDB and RabbitMQ are running, then run **GWT Sailing SDM** followed by the back-end **Sailing Server (no Proxy)**. Open the `AdminConsole.html` entry in the "Development Mode" tab in Chrome or Firefox (step 17) → <http://127.0.0.1:8888/gwt/AdminConsole> (the first attempt often fails — just reload). Log in with **admin** / **admin**.

Full sequence, including how to start MongoDB and how to import a TracTrac test event: [[Steps to build and run the Sailing Analytics|wiki/howto/onboarding#steps-to-build-and-run-the-sailing-analytics]].

## 16. Build for deployment (only when needed)

Not part of the daily workflow. In a bash shell at the repo root:

```bash
./configuration/buildAndUpdateProduct.sh build
```

Details and options (skip tests, single permutation, proxy): [[Build for deployment|wiki/howto/onboarding#build-for-deployment]].

## 17. Browser

Install **Chrome or Firefox** (needed to open the AdminConsole in step 15). Safari does not work reliably with this project.

## Contributing to this guide

Made it through the setup? Great, please help keep this guide useful for the next person. If you spotted anything unclear or outdated, found a better way to do a step, or worked out the **native Apple Silicon (arm64) path** in more detail, you are very welcome to improve this document and open a **pull request**. Contributions that flesh out the arm64 variant are especially valuable, since that path will matter more as Rosetta 2 is phased out (see the note at the top).

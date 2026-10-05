# Setting up Smartphone Tracking for your Event

This guide is for **event hosts**: you want to run a regatta, a club series or a media event and track the boats with the smartphones of the sailors instead of dedicated tracking hardware. It walks through the whole way in the order in which you will actually do things, from the first login to the moment the race shows up live on the map.

If you only want to *join* an event as a sailor, you do not need this guide. Install **Sail Insight**, scan the QR code you were sent and tap *Join Race* (see [[Getting started with Sail Insight|wiki/howto/tutorials/sailinsight/getting-started]]).

[[_TOC_]]

## 1. What you need

* A user account on the server that will host your event, with the permission to create events (see [[Signing up for a user account|wiki/howto/tutorials/sailinganalytics/sign-up]]).
* A server that the phones can reach from the water. In practice this means the public landscape, e.g. [my.sapsailing.com](https://my.sapsailing.com/gwt/Home.html), or a local server that is reachable from the phones' network.
* One phone per boat running **Sail Insight**
  ([iOS](https://apps.apple.com/us/app/sail-insight-powered-by-sap/id1495355086), [Android](https://play.google.com/store/apps/details?id=org.sailyachtresearch.sailinsight)).
  The phone of the event manager can double as a tracking device.
* Optional: phones for the marks (also running Sail Insight), or the **Buoy Pinger** app ([Android](https://play.google.com/store/apps/details?id=com.sap.sailing.android.buoy.positioning.app)) to record the positions of the marks.

Open regattas, where sailors register themselves, are only supported by the new **Sail Insight powered by SAP** app. The Admin Console warns you if your server is still configured for the old Sail InSight 1.

## 2. Create the event structure

Log in to the Admin Console and create an event as described in [[Create a simple event|wiki/howto/tutorials/sailinganalytics/simple-event-creation]]. When you are done you should have

* an **Event**,
* a **Leaderboard Group** attached to the event,
* a **Regatta** linked to the event, and
* a **Leaderboard** in that group, with all the race columns (and fleets, if you sail split fleets) you need.

The wizard that creates the event normally sets all of this up for you. For more complex set-ups see [[Create complex regattas|wiki/howto/tutorials/sailinganalytics/create-regattas]] and [[Set up multiple regattas|wiki/howto/tutorials/sailinganalytics/set-up-regattas]].

## 3. Denote the races for smartphone tracking

Everything that follows happens in the Admin Console under **Connectors** → **Smartphone Tracking**. Select your leaderboard in the table at the top. The icons in its *Actions* column are the entry points for the next steps. Hover over an icon to see its name.

Click **Shortcut to denote all races for racelog tracking** (green arrow). A dialog titled **Choose a name** opens. This is the *race name prefix* dialog, and it decides how the tracked races will be called later on:

| Option | Resulting names |
|---|---|
| **Default** | leaderboard name, race column and fleet, e.g. `My Regatta R1 Yellow` |
| **Own Prefix** | your prefix followed by a running number. The text field is pre-filled with `R`, so you get `R1`, `R2`, `R3`, … |

The line **Your name looks like:** below the options shows an example of what you are going to get. Check it before you confirm. In most events a short own prefix is the better choice because the names appear in many places on the website.

After confirming you will see **All races have been denoted for racelog tracking**. Races in a race column with several fleets are denoted so that each fleet can have its own set of competitors.

You can also denote a single race instead: select the race in the list of races below the leaderboard and use **Denote for RaceLog tracking** in its action column. Use **Remove Denotation** to take it back.

## 4. Register the competitors

Click **Competitor Registrations** in the leaderboard's actions. Select existing competitors or create new ones. For one-design events with fixed boats you only need to do this once for the whole leaderboard. If boats change from race to race, a **Boat Registrations** action is additionally available.

For split-fleet racing, competitors can also be registered per race (**Register Competitors on Race**). Be aware that races with their own registrations no longer follow the registrations of the regatta.

## 5. Define the course

Select the first race and click **Define Course Layout**. Create the course by defining the marks first and then the waypoints (start, marks, finish). Use **Copy course and competitors to other races** to copy the result to all other races of the day. Most events use the same course layout for the whole series.

There are two ways to give a mark a position:

* **Tracker:** a phone on the mark, see step 6.
* **Fixed position:** with the Buoy Pinger app, with *Ping position* in Sail Insight, or manually in the course layout dialog.

If you use the Race Manager app in the same event, make sure it uses the **By Marks** course designer. The *By Name* designer can overwrite a course you defined in the Admin Console with an empty one.

To get the Buoy Pinger app to the people on the mark boats, use **Invite Buoy Tenders** in the leaderboard's actions, enter one or several e-mail addresses (separated by commas), and send. The mail contains a QR code that opens the app.

## 6. Bring the phones in

There are three ways to connect a sailor's phone to a competitor. Pick one per event.

### 6a. You hand out the codes: *Map Devices to Competitors, Boats and Marks*

Best when you know the competitors and can meet them ashore, e.g. at a coaching session or for a small event.

1. In the leaderboard's actions click **Map Devices to Competitors, Boats and Marks**.
2. Click **Add**, pick the competitor, boat or mark, and select the event.
3. A **QR code** is shown. The sailor scans it with Sail Insight. The app installs itself first if necessary.
4. Repeat for every boat and every tracked mark.

Existing mappings are listed on the same screen, where you can also delete them.

### 6b. Invite by e-mail (closed regatta)

Best when you have the e-mail addresses of the competitors, e.g. from the entry list. Competitors need valid addresses for this.

1. Go to **Leaderboard Configuration**, select your leaderboard and open the competitors of the leaderboard (**Edit Competitors**).
2. Select the competitors and click **Invite selected competitors** (the button at the top of the list is called **Invite competitors**).
3. Select the event the invitation is for.

Each competitor receives an e-mail with a QR code and a link that binds the phone to the competitor. Competitors without an address are reported as **Not all competitors provide an E-Mail**.

### 6c. Self-registration (open regatta)

Best when you do not know the sailors beforehand, e.g. for a club evening or a public event. The regatta has a **Competitor Registration Type**:

| Type | Meaning |
|---|---|
| **closed** | only competitors you register or invite take part |
| **open moderated** | sailors register themselves through the registration link, the organiser stays in control |
| **open unmoderated** | anybody with the registration link or QR code can register |

To publish the link go to **Regattas**, select the regatta and click **Share** below the regatta table. The dialog **Registrationlink for Open Regatta** shows the QR code (which you can download for use on a poster or in an invitation) and the registration link with a **Copy to clipboard** button.

The link contains a **Secret**. If you click **Generate** to create a new one, all links and QR codes that are already published stop working.

> **Tip:** A QR code on a poster only works if the registration type is open and the code is really published. For events with a fixed entry list, 6a or 6b are the safer choice.

## 7. Before the start

* Ask every sailor to open Sail Insight and press **Start Tracking** *before* the race starts. A running timer shows that tracking is active.
* Tracker phones on the marks have to be started manually, too.
* Tracking runs for several hours, so the phones should start with a full battery. Background on the power consumption of the app can be found on [[Power consumption of Android apps|wiki/info/mobile/energy-consumption]].

## 8. Start tracking in the Admin Console

1. Go back to **Connectors** → **Smartphone Tracking** and select the leaderboard.
2. In the list of races select the race (or several races) and use **Set start time** and, if required, **Set tracking times**.
3. Leave **Track Wind** and **Correct Wind Bearing by Declination** checked unless you have a reason not to. Both are on by default.
4. Click **Start tracking**. If competitor registrations are missing for a selected race, you are asked to confirm first.

Once tracking has started, click the race in the list to open the map in a new tab. In the bottom right you can edit mark positions and mark passings manually, on the left you choose the competitors you want to see. To end tracking click **Stop tracking**.

## 9. Typical problems

| Problem | Reason and remedy |
|---|---|
| A boat does not show up | The sailor forgot to press *Start Tracking*, or the phone has no mobile data. Check the mappings in **Map Devices to Competitors, Boats and Marks**. |
| One device is mapped to two competitors | Fix it as described in [[Fix device assignment|wiki/howto/tutorials/sailinganalytics/race-fixes]]. |
| *Start tracking* is not offered for a race | The race may not be denoted yet. A race that was tracked before and then stopped is still linked to its tracked race, which has to be removed first. |
| Wrong or empty course after using the Race Manager app | The *By Name* course designer was used, see step 5. |
| Tracking works, but the marks are in the wrong place | Fix the positions in the map: [[Adjust mark position|wiki/howto/tutorials/sailinganalytics/adjust-mark-position]], [[Set mark position|wiki/howto/tutorials/sailinganalytics/set-mark-position]]. |

## 10. Checklist for the event day

- [ ] Event, regatta, leaderboard group and leaderboard exist
- [ ] Races denoted for racelog tracking, with a sensible prefix
- [ ] Competitors registered
- [ ] Course defined and copied to all races
- [ ] Mark positions set (tracker or fixed position)
- [ ] Every boat has a device mapped, invited or registered
- [ ] Sailors know to press *Start Tracking*
- [ ] Start time set, tracking started, race visible on the map

## See also

* [[Managing Events with the AdminConsole|wiki/howto/adminconsoleinstructions]]
* [[QR codes in SAP Sailing|wiki/howto/misc/qr-codes]]: how the QR codes and links work technically
* [[Sail Insight tutorials|wiki/howto/tutorials/home]]
* [[Operating Igtimi WindBots|wiki/howto/eventmanagers/windbot-operations]]: wind measurement

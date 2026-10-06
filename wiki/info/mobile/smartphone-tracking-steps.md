> **Note:** A newer step-by-step guide for event hosts is available: [[Setting up Smartphone Tracking for your Event|wiki/howto/eventmanagers/smartphone-tracking]]. This page is kept for reference.

# Steps to setup Smartphone TRacking

## prerequisitites
First of all, you need either an online server landscape which is accessible from outside, or a local one with a working setup allowing your smartphone or tablet to establish a connection to.

## event setup
Log on to the Administration Console and make sure you have a working Event setup.
For further information on how to achieve this, have a look at [this walkthrough](/wiki/info/mobile/event-tracking/event-tracking).
After completing these steps up until the point "Add Device Mappings" or by simply following the step-by-step wizard, you should have the following setup:

* an Event
* a Leaderboard Group
* a Regatta linked to your event
* A Leaderboard linked to your Leaderboard Group

_Hint:_ You can link Leaderboards to a Leaderboard Group by selecting your group under the tab "Leaderboards" on the left hand side and then selecting one or multiple Leaderboards on the right hand side and adding them by using the "<-" arrow.
Usually this step is carried out by the wizard automatically.

* Competitors in your Regatta
* races denoted for racelog tracking
* defined course layouts for these races

## pinging buoys and other race marks
To map positions of all of the required marks, you need to invite one or multiple people as Buoy Tenders. Do so by navigating to "Connectors->Smartphone Tracking" in the Admin Console, selecting your Regatta from the list, and then clicking the Mail Icon under "Actions" within the entry.

Those invited people then need to download the [SAP Sailing Buoy Pinger](https://play.google.com/store/apps/details?id=com.sap.sailing.android.buoy.positioning.app) App - a link is also provided in the mail they received. Once installed, they will see all marks related to this regatta listed within the app, enabling them to ping their exact positions via GPS.
You can later on adjust these positions within the app, or manually in the Admin Console once Tracking has started.

## start tracking using an Android phone or tablet
At the same location where you can invite Buoy Pingers, you'll also find the option to map devices to competitors and marks. It is the small smartphone icon next to the mail icon. Once clicked, you will see a screen with potentially earlier created device mappings.

**To add a new device**

* click "Add" in the top left corner, leading you to another screen
* Within that screen, you can select a mark or a competitor you'd like to track
* You possibly have to select the correct event if there is more than one
* you will then be provided with a QR code which you will be asked for in the [SAP Sail InSight](https://play.google.com/store/apps/details?id=com.sap.sailing.android.tracking.app) App
* get the app, scan the code and hit "Start Tracking".

Once tracking has started, you should see a running timer.

## follow trackers on the web interface
Back in the Admin Console, you can click on the race you just started tracking. This will open a new tab showing the map of the course.

In the bottom right corner, you can edit mark positions and (possibly missed) passings manually, on the left hand side, there is the option to select the competitors you want to see on the map.

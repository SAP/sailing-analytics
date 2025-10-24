This articel provides a short guidance of how to synchronize races with their YouTube Videos.

# 1. Open the video on YouTube
  1. Check first race covered by the video.
  2. Seek to race start. Ideally, the video shows a start countdown for this race (see figure 1).
  3. Note the video time for the start
  4. Take the Youtube URL to the clip board
  5. Check last race covered

![Figure 1: Most races provide a race start indicator](https://s3-eu-west-1.amazonaws.com/media.sapsailing.com/wiki/how%20to/linking%20race%20videos/start_race_indicator.png)
**Figure 1: Most races provide a countdown which indicates when the race is going to start**

# 2. Open Admin Console
  1. This step is necessary to elevate you as admin user
  2. Open sapsailing.com
  3. Go to event home page
  4. In the URL, replace "http://`server name>`/gwt/Home.html/...." by "http://`server name`/gwt/AdminConsole.html". _Remember to access the -master url!_
  5. Enter login credentials

# 3. In a new bowser tab
  1. Open the first race covered by the video. It is located under the _races_ tab and will redirect you to the raceboard.
  2. Click "Manage Media" (see figure 2, only visible when logged into Admin Console before)
  3. Click Add
  4. Paste the video’s Youtube URL
  5. Wait for the dialog field to fill
  6. Adjust the “Name” field --> pattern, e.g.: <short title> Day <X> - <event short name> <year> (Race <first race>..<last race>). E.g.:
    1. “Live Replay – Day One – ESS Nice 2014 (Race 8..13)”
    1. “3D Graphics Only – Day One – ESS Nice 2014 (Race 8..13)”
  7. Click OK
  8. A video window will pop up with the video a time 0:00.

![Figure 2: Manage media button is only visible when logged in as admin](https://s3-eu-west-1.amazonaws.com/media.sapsailing.com/wiki/how%20to/linking%20race%20videos/manage-media-btn.png)
**Figure 2: Manage media button is only visible when logged in as admin**

# 4. In the video window
  1. Click “Edit” (--> decouples video from race)
  2. Seek video to start of first race (see figure 3, if available, a count down overlay on the video is very helpful)
  3. Click “Preview”
  4. Start the race --> the video should start running
  5. Jump race to first mark rounding --> the video should show leader round first mark
  6. Check a few other distinct race points (mark rounding, finish, …). Be cautious with sections of 3D graphics embedded into live video: they have a delay and don't suit for alignment.
  7. For fine tuning press “Edit” again, use +/- buttons and confirm with “Preview”
  8. Click “Save”. If you forget to save, changes will not be stored on our server!
  9. Done. The video is linked to first race and synchronized.
 10. While subsequent races covered by the same video should already be synchronized, they still need to be linked explicitly

![Figure 3: Synchronize the tracked race with the Live-Stream](https://s3-eu-west-1.amazonaws.com/media.sapsailing.com/wiki/how%20to/linking%20race%20videos/sync_races.png)

**Figure 3: Synchronize the tracked race with the Live-Stream**

# 5. Go to Admin Console
  1. Open tab “Races/Manage Media” (see figure 4)
  2. Search the recently added video ( use search box)
  3. For this video, click column “Linked Race” (showing first race only)
  4. In the “Linked Races” dialog select all races covered by the video
  5. Open each race and check a few distinct points (start, finish)
  6. If a race doesn’t match the video
  7. decouple this race and all subsequent races (most likely, the video has been cut)
  8. start again from step 3. of chaper 1

![Figure 4: Link all races covered by the video](https://s3-eu-west-1.amazonaws.com/media.sapsailing.com/wiki/how%20to/linking%20race%20videos/multi_sync.png)
**Figure 4: Link all races covered by the video**

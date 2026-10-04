# Setup locally hosted 360°
[[_TOC_]]

## Installing the Webserver

* Download the zip from [https://static.sapsailing.com/webserver-nginx/nginx-videohost.zip](https://static.sapsailing.com/webserver-nginx/nginx-videohost.zip).
![](nginx-1.JPG)
<br><br>
* Extract the zip into some easily reachable folder. (eg the Desktop) using the inbuild extractor of your operating system.
![](nginx-2.JPG)
<br><br>
* The extracted folder should look like this:
![](nginx-3.JPG)


### Windows specific

* If you are running windows, double click the start_windows.bat<br>
![](nginx-4.JPG)
<br><br>
* Upon this a security dialog appears, confirm it <br>
![](nginx-5-win.JPG)
<br><br>
* After this a cmd.exe window should open that looks similar to this one<br>
![](nginx-6.JPG)
<br><br>

### Mac specific

* If you are running MacOSX, execute start_maxos.command
* This script will start nginx with the correct configuration.
* If nginx is not already installed, it will be installed using homebrew
* If homebrew is not installed, it will also be installed prior
* If there are any dialogs requesting to install required software, confirm them.

### Linux specific

If you are on Linux you can:<br>
* Try to start the supplied 64bit ubuntu static build ./nginx_ubuntu <br>
* if it crashes/ does not work use your systems packagemanager to download nginx and start it with nginx -c movieHost.conf<br>
* or use the supplied shell script "build_linux.sh" to download the required sources and compile nginx for your system.<br>

## Testing the Server

* Browse to [http://127.0.0.1:8080/](http://127.0.0.1:8080/) the following website should appear. If you are on a different computer, replace the ip address with one the server can be reached with.
![](nginx-7.JPG)
<br><br>
* To add new videos go to the folder place\_videos\_here
![](nginx-8.JPG)
<br><br>
* And copy your files into it
![](nginx-9.JPG)
<br><br>
* Following a refresh in the browser (F5) the video file should appear in the list:
![](nginx-a.JPG)
<br><br>
* Click on it to verify it is reachable, note that depending on the browser 360° videos will look distorted.
![](nginx-b.JPG)
<br><br>

## adding a 360 Video

* After navigating into the RaceBoard
* Click manage Media, if this button does not appear, check that you are signed in (upper right corner)
![](managefiles8.JPG)
<br><br>
* Click add at the bottom of the newly opened Media Manage dialog
* You should now see the following dialog:<br>
![](addMedia8.JPG)
<br><br>
* Copy the url of a video from the former part<br>
![](nginx-c.JPG)
* And paste it into the URL textbox<br>
![](addMedia10.JPG)
<br><br>
* Wait for the autodetection. The autodetection will prefill starttime and mimeType with information that can be derived from the video itself, please check that:<br> 
the MimeType is set to mp4panorama in case of a 360 video, or mp4 for a normal 2D video <br> 
Ensure a proper starttime is set, this should be the time, when the recording of the video was started. <br>With the Set to default button, the starttime of the video can be set to the start of the race. <br> It is later possible to properly synchronize the video, if a small delay exists, so this does not have to be perfect, but the fine synchronization is easier if it is close.<br><br>
![](addMedia12.JPG)
<br><br>
* Close the Dialog with OK, a small video player should appear (it's content might be black, this is ok), if it does not appear please try reloading the page
* Using the edit button and the various +&- increment buttons, the video can be fine synchronized if required, do not forget to click save if adjusted<br><br>
![](VideoWorking13.JPG)
* Once the timeSlider is within a valid range (determined by the starttime and duration of the video) the player will start and play concurrently with the map.



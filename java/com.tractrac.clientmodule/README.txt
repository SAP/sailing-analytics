********************************************
************* TracAPI **********************
********************************************

1) Content

This zip package contains 2 folders:

 - lib -> contains the Trac-API compiled library
 - src -> contains the source code of the API and some code examples  
 
The documentation can be retrieved online from http://tracdev.dk/maven-sites-clients/3.0.0/maven-java-parent/.

It contains also some files:

 - test.sh -> script that compiles the code in the src folder, creates the test.jar library and execute the code of the example.
 - Manifest.txt -> manifest used to create the test.jar file

********************************************
************* TracAPI 3.2.2 ****************
********************************************
This is a final version. It fixes bugs in the implementation and it adds a some features.
These features add methods to the API, but they keep the backward compatibility.
This version provides a new JavaDoc version.

Release date: 21/09/2016
Build number: 12927

1) Features

 - Adding the method ISubscriber.isRunning() to know if the thread created by the ISubscriber.start() method
 is running or not (Requested by Jorge Piera, 04/08/2016).

 - Added attribute in the IRaceCompetitor interface: statusTime. It will contain the time when an entry has changed
 its status to abandoned/retired.

 - Added new value to the RaceCompetitorStatusType enum: NO_COLLECT. This value means that the competitor didn't
 collected the tracker.

2) Bugs

 - When an static control is added (or updated), the timestamp has to be the event start time. At this moment
  it is using the timestamp when whe control was created (Reported by Steffen Wagner, 12/09/2016)


********************************************
************* TracAPI 3.2.1 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 13/07/2016
Build number: 12627

1) Bugs:

 - If the Internet connection goes down, some threads can be hung despite of
 the consumer application calls the ISubscriber.stop method (Reported by
 Axel Uhl, 13/07/2016)

********************************************
************* TracAPI 3.2.0 ****************
********************************************
This is a final version. New functionality added:

Release date: 05/06/2016
Build number: 12293

1) Features:

 - IRace adds the new property, status, returning a value of the enum RaceStatusType.

 - IRace property visibility has changed to return a value of the enum RaceVisibilityType.

 - IRaceCompetitor adds a new property: status of type RaceCompetitorStatusType.

 - Competitor updates during a race will be propagated to all subscriptors.

********************************************
************* TracAPI 3.1.6 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 22/02/2016
Build number: 11976

 1) Bugs
 
 - The bug of the previous release (3.1.2) was not fixed.
 
********************************************
************* TracAPI 3.1.5 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 17/02/2016
Build number: 11948

 1) Bugs
 
 - The bug of the previous release (3.1.2) was not fixed.
 
********************************************
************* TracAPI 3.1.4 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 15/02/2016
Build number: 11938

 1) Bugs
 
 - An infinity loop has been detected in the thread that sends the application
 messages (Repored by Axel Uhl, 12/02/2016) 

********************************************
************* TracAPI 3.1.3 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 25/01/2016
Build number: 11888

 1) Bugs
 
 - The bug of the previous release (3.1.2) was not fixed.

********************************************
************* TracAPI 3.1.2 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 21/01/2016
Build number: 11864 

 1) Bugs
 
 - The ISubscriber.stop() method never finishes due to a deadlock (it happens sometimes).
 There is a deadlock between one of the internal threads responsible to update the progress 
 and the thread that calls the stop method. The release 3.1.1 fixed a part of the bug but
 it continues happening due to a new deadlock (Reported by Axel Uhl, 20/01/2016) 

********************************************
************* TracAPI 3.1.1 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 18/01/2016
Build number: 11844

 1) Bugs
 
 - The ISubscriber.stop() method never finishes due to a deadlock (it happens sometimes).
 There is a deadlock between one of the internal threads responsible to update the progress 
 and the thread that calls the stop method (Reported by Axel Uhl, 16/11/2015) 

********************************************
************* TracAPI 3.1.0 ****************
********************************************
This is a final version. It fixes bugs in the implementation and it adds some new features. 
These features add methods to the API breaking the backward compatibility. These changes are:

 - The IRaceCompetitorListener interface implements a new method removeOffsetPositions. If 
 your app implements this interface, you have to implement this extra method.  
 
This version provides a new JavaDoc version.

Release date: 11/12/2015
Build number: 11801

 1) Features
 
   - Adding the method IRaceCompetitorListener.removeOffsetPositions that is invoked
  to invalidate a set of positions. It is used when the event administrator detects 
  an error in the snapping algorithm (in route sports) and he want to remove the wrong
  positions (Requested by Chris Terkelsen, 26/11/2015)
 
  - Adding the method IControl.getMapName() that contains the name that has to be used to
  display the control on the map (Requested by Chris Terkelsen, 09/12/2015)
    
 2) Bugs
 
  - The static controls send a position event using the race start time instead the 
  event start time. (Reported by Jerome Soussens, 11/12/2015)
   
********************************************
************* TracAPI 3.0.14 ***************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 29/09/2015
Build number: 11514

 1) Bugs
 
 - NullPointerException caused by a synchronization error (Reported by J�rome Soussens, 28/09/2015)
 
********************************************
************* TracAPI 3.0.13 ***************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 23/09/2015
Build number: 11448

 1) Features
 
 - The script that generates the final version also includes a jar with the source code of the APIs
 (Requested by Axel Uhl, 07/09/2015)

 2) Bugs
 
 - When a new race is updated, the IRace.getParamsURI() is updated to null. (Reported by Juan Salvador P�rez, 10/09/2015)
  
 - The IRace.getMetadata(), ICompetitor.getMetadata and IControl.getMetadata() methods are not refreshed in
 the model when the objects are updated in the event manager (Reported by Jorge Piera, 23/09/2015)
 
********************************************
************* TracAPI 3.0.12 ***************
********************************************
This is a final version. It fixes bugs in the implementation and it adds a some features. 
These features add methods to the API, but they keep the backward compatibility. 
This version provides a new JavaDoc version.

Release date: 07/08/2015
Build number: 11130

 1) Features
 
 - Adding the method ISubscriberFactory.setUserId(userId) used to configure a valid user with permissions
 to retrieve data from the TracTrac servers. (Requested by Jorge Piera, 18/06/2015)
 
 2) Bugs
 
 - If the consumer application is not subscribed to the events that manage the addition of new "objects" 
 in the system (e.g: add control, add competitor...), it is never going to receive the events related 
 with these "objects". It is an error of design in TracAPI because it has been designed "thinking"
 that the consumer application is always subscribed to receive all the events. The solution is simple: 
 TracAPI is always going to subscribe to all the events to guarantee that the static model is always 
 updated and to guarantee that if the consumer application is subscribed to a type of event it is always 
 going to receive all the events of this type. There is a secondary effect: the consumer application can 
 receive events with new objects. (Reported by Axel Uhl, 20/07/2015)
  
********************************************
************* TracAPI 3.0.11 ***************
********************************************
This is a final version. It fixes bugs in the implementation and it adds a some features. 
These features add methods to the API, but they keep the backward compatibility. 
This version provides a new JavaDoc version.

Release date: 27/05/2015
Build number: 10715

 1) Features 
 
 - Adding the method IRace.setInitialized(boolean) that allows to load control point positions despite of
 the race is not initialized (Requested by Jorge Piera, 17/05/2015)
 
 1) Bugs 
 
  - The subscription library sends (per each control point) an "ControlPointPosition" event with the positions 
 of the parameters with a time stamp equals to the tracking start time. If the control  point is static (it 
 doesn't have a tracker), it means that this control won't exist until the tracking start time arrives: it is 
 not possible to open a subscription connection before the tracking start time and see the control point. But 
 if the control is static,  it has to exist during all the event. Now when a control is static, the subscription 
 library sends a "ControlPointPosition" event with the event start time and then it sends other "ControlPointPosition"
 event with the tracking start time. It allows to connect with future races and see the static controls (Requested 
 by Jakob Odum, 25/02/2015)
   
 - It is possible to add two or more subscriptions for the same subscriber (Reported by Jorge Piera, 02/05/2015)
 
 - An initialized race is a race where the tracking is on. A not initialized race is a race where the tracking is off.
 If a race is in a not initialized state and it is loaded using the parameters file, the library assumes that the 
 consumer application wants to load the race and it changes its initialized  attribute to true (the tracking is on).
 This assumption is wrong. Now, if a race is not initialized it will continue being uninitialized until the event 
 administrator changes its state to initialized.
   When a race changes its state from not initialized to initialized, some events are thrown in the system:
     1. IRaceStartStopTimesChangeListener.gotTrackingStartStopTime(IRace, IStartStopData): where the IStartStopData object contains the new tracking interval.
     2. IRacesListener.startTracking(UUID): the UUID is the race identifier
     3. IRacesListener.updateRace(IRace): IRace is the new updated race where the IRace.isInitialized is true and it has values for both the getTrackingStartTime and the getTrackingEndTime methods.
   When a race changes its state from initialized to not initialized, the following events are thrown in the system:
     1. IRaceStartStopTimesChangeListener.gotTrackingStartStopTime(IRace, IStartStopData): where the IStartStopData object contains the null interval ([0, 0])
     2. IRacesListener.abandonRace(UUID): the UUID is the race identifier
     3. IRacesListener.updateRace(IRace): IRace is the new updated race where the IRace.isInitialized is false and it has null values for both the getTrackingStartTime and the getTrackingEndTime methods.
  (Reported by Axel Uhl, 17/05/2015)
 
********************************************
************* TracAPI 3.0.10 ***************
******************************************** 
This is a final version. Only fixes bugs in the implementation

Release date: 17/02/2015
Build number: 9938
 
 1) Bugs
 
 - It fixes the same bug described in the release 3.0.9 that it was not fixed. The release 3.0.9 fixed
 the synchronization issue between different threads but the ConcurrentModificationException happens
 when the same thread is accessing to the list. The synchronization at method level doesn't work
 in this case (Reported by Axel Uhl,07/02/2015) 
 
********************************************
************* TracAPI 3.0.9 ****************
******************************************** 
This is a final version. Only fixes bugs in the implementation

Release date: 13/02/2015
Build number: 9904
 
 1) Bugs
 
 - There is a ConcurrentModificationException in the IConnectionStatusListener subscription, when a thread
 is adding subscriptions/unsubscriptions and other thread is sending events through this listener (Reported 
 by Axel Uhl,07/02/2015)

********************************************
************* TracAPI 3.0.8 ****************
********************************************
This is a final version. It fixes bugs in the implementation and it adds a some features. 
These features add methods to the API, but they keep the backward compatibility. 
This version provides a new JavaDoc version.

Release date: 07/02/2015
Build number: 9826
 
 1) New features
 
 - Adding the method IEvent.getEventType that returns the type of the event (e.g: Sailing, Orienteering...).
 (Requested by Jorge Piera, 01/02/2014)
 
 2) Bugs
 
 - The changes in the static fields of the route (including the metadata) were not propagated through 
 the subscription library. Following the same approach used for the races, controls and competitors, 
 a new listener IRoutesListener has been added to manage these changes.  (Reported by Axel Uhl,
 06/02/2015)
 
********************************************
************* TracAPI 3.0.7 ****************
********************************************
This is a final version. It fixes bugs in the implementation and it adds a some features. 
These features add methods to the API, but they keep the backward compatibility. 
This version provides a new JavaDoc version.

Release date: 19/11/2014
Build number: 9436

1) New features

 - The IRace interface implements the method getStartTimeType that returns the type of start time. It is an enumerated 
 value with the values: Individual (if the competitors have an individual start time), RaceStart (if the race
 has a race start time) and FirstControl (if the start of the race happens when the competitor passes for
 the first control). The value by default is RaceStart. At this moment this value only can be managed at an event
 level (in the event manager) but in a future we will add this functionality at a race level (Requested by 
 J�rome Soussens, 14/10/2014)  
 - The method IControlPoint.getPosition is deprecated. Use the IControlPointPositionListener.gotControlPointPosition()
 to know the position of a control point (Reported by Jorge Piera, 28/10/2014)   
 - Adding the isNonCompeting() method to the ICompetitor interface. If a competitor is a non competing competitor
 doesn't receive control passings attached to it. It only receives positions (Requested by J�rome Soussens, 15/10/2014 and
 Axel Uhl, 22/10/2014) 

2) Bugs

 - It checks that the system of messages doesn't generates a BufferOverflow exception. We added a bug on the server
 sending wrong strings to the clients and this error thrown an exception on the clients that killed one of threads.
 Now the tracapi checks if there is an error encoding the strings and in this case, logs the exeption discarding
 the message but the thread, continues running. Anyway the error has been fixed in the server side. (Reported by 
 Jorge Piera, 30/09/2014)   
 - If there is a control point without coordinates the lat,lon values are not included in the parameters file
 and the library doesn't create the IControlPoint. Now, the library creates the IControlPoint and it returns
 the values Double.MAX_VALUE for both the lat and the lon. This position is not sent by the 
 IControlPointPositionListener.gotControlPointPosition(). (Reported by Jakob Odum, 28/10/2014)    
 - There is a synchronization bug registering and sending messages using the General Message System. The list
 that is used to register the listeners is not a synchronized list. This bug has been fixed (Reported by Axel Uhl, 19/11/2014,
 http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=2474)
 
********************************************
************* TracAPI 3.0.6 ****************
********************************************
This is a final version. It fixes bugs in the implementation and it adds a new feature. 
This new feature adds methods to the API, but it keeps the backward compatibility. 
This version provides a new JavaDoc version.

Release date: 11/08/2014

1) New features

 - The IRaceCompetitor implements the IMetadataContainer interface (Requested by Frank Mittag, 16/07/2014)  

2) Bugs

 - The race name and the visibility are not updated in the IRacesListener.update() method.
 (Reported by Jorge Piera, 06/08/2014)   
 - The IRaceCompetitor doesn't contains the associated IRoute object (Reported by Jorge Piera, 08/08/2014)
 - Changing the synchronization of the maps in the EventFactory (Reported by Axel Uhl, 09/08/2014)   
 
 
********************************************
************* TracAPI 3.0.5 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 05/08/2014

1) Bugs

 - The previous version 3.0.4 added only changes in the synchronization. The lists were synchronized by hand
 but the implementation continued using synchronized lists. The result was that we had a double
 synchronization for all the lists of the model: the synchronization by hand and the synchronization of
 the list. (Reported by Axel Uhl, 04/08/2014)   
 - The new subscription library sends the static positions (from the parameters file) as positions events.
 The problem here is that the subscription library retrieves the positions from the model of control 
 points and  when some races are loaded in parallel these values can be invalid (values loaded by 
 other race). Now the subscription library sends the static positions from the parameters file. 
 (Reported by Jorge Piera, 04/08/2014)  
  

********************************************
************* TracAPI 3.0.4 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 04/08/2014

1) Bugs

 - Some lists are not thread-safety. The model project exposes some lists based on a 
 CopyOnWriteArrayList implementation that is thread-safety if you get its iterator. 
 But if before to invoke the iterator() method of the list, other thread is editing the list,
 it is possible to get a list in an invalid status. (Reported by Axel Uhl, 31/07/2014)   
 

********************************************
************* TracAPI 3.0.3 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 25/07/2014

1) Bugs

 - When a race is reloaded more than one time it keeps the entries despite of they have been removed. The new entries
 are correctly added but the removed competitors are kept. (Reported by Axel Uhl, 25/07/2014)   
 
 
********************************************
************* TracAPI 3.0.2 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 24/07/2014

1) New features

 - The IRacesListener.updateRace() event is thrown when the live delay for a race has changed. The IRace.getLiveDelay() method
 returns the new value.

2) Bugs

 - The events IRacesListener.abandonRace() and IRacesListener.startTracking() have been reviewed. Now they are always sent
 when the races are updated either using the event manager or using the external JSON service "update_race_status"
 - The controls are not updated when they are retrieved a second time from a parameters file. The library always returns 
 the first control that was read the first time. (Reported by Axel Uhl, 24/07/2014)    
 
********************************************
************* TracAPI 3.0.1 ****************
********************************************
This is a final version. Only fixes bugs in the implementation

Release date: 09/06/2014

1) Bugs

 - Error reading JSON of races. 

********************************************
************* TracAPI 3.0.0 ****************
********************************************
This is a final version. 

Release date: 04/06/2014

1) New features

 - The positions of the static marks are sent like an event
 - The static start/end tracking times and the static start/end race times are sent like an events
 - The static course is sent like an event
 - The IConnectionListener.stopped() method uses an object as argument 
 - The start/end tracking times are 0l if the race has not been initialized
 - Added the IRaceListener.startTracking event
 - Added the IRace.getExpectedRaceStartDate()
 - Added the IRace.isInitialized()
 - Added the IRace.getVisibility()
 
2) Bugs

 - Error reading the race start time from the JSON
 - Synchonizating the locators used to retrieve objects using the Service Provider Interface

********************************************
********* TracAPI 3.0.0-SNAPSHOT ***********
********************************************

This is an SNAPSHOT version that means that is a version that has not been released (is under development).
 
1) Bugs

 - The route name is lost when the ControlRouteChange event is thrown
 - Adding synchronized lists in some objects of the model that were not synchronized
 - Fixing an error using a local parameters file with a local MTB file
 - Fixing several errors marshaling the objects then an static property has been changed
 - Fixing a bug parsing a JSON with races that has races without params_url (for Orienteering events)


 
 


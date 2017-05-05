//
//  RegattaController.swift
//  SAPTracker
//
//  Created by Raimund Wege on 05.07.16.
//  Copyright © 2016 com.sap.sailing. All rights reserved.
//

import UIKit

class RegattaController: NSObject {
        
    let checkIn: CheckIn
    
    var sendingBackgroundTask: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid
    var sendingDate: NSDate = NSDate()
    
    private (set) var isTracking: Bool = false
    
    init(checkIn: CheckIn) {
        self.checkIn = checkIn
        super.init()
        subscribeForNotifications()
    }
    
    deinit {
        unsubscribeFromNotifications()
    }
    
    // MARK: - Notifications
    
    private func subscribeForNotifications() {
        NSNotificationCenter.defaultCenter().addObserver(self,
                                                         selector:#selector(locationManagerUpdated(_:)),
                                                         name:LocationManager.NotificationType.Updated,
                                                         object: nil
        )
        NSNotificationCenter.defaultCenter().addObserver(self,
                                                         selector:#selector(batterySavingChanged(_:)),
                                                         name:Preferences.NotificationType.BatterySavingChanged,
                                                         object: nil
        )
    }
    
    private func unsubscribeFromNotifications() {
        NSNotificationCenter.defaultCenter().removeObserver(self)
    }
    
    @objc private func locationManagerUpdated(notification: NSNotification) {
        dispatch_async(dispatch_get_main_queue(), {
            guard self.isTracking else { return }
            guard let locationData = notification.userInfo?[LocationManager.UserInfo.LocationData] as? LocationData else { return }
            guard locationData.isValid else { return }
            let gpsFix = CoreDataManager.sharedManager.newGPSFix(self.checkIn)
            gpsFix.updateWithLocationData(locationData)
            CoreDataManager.sharedManager.saveContext()
            if self.sendingDate.compare(NSDate()) == .OrderedAscending {
                self.sendingDate = NSDate().dateByAddingTimeInterval(BatteryManager.sharedManager.sendingPeriod)
                self.beginGPSFixSendingInBackgroundTask()
            }
        })
    }
    
    @objc private func batterySavingChanged(notification: NSNotification) {
        dispatch_async(dispatch_get_main_queue(), {
            guard let batterySaving = notification.userInfo?[Preferences.UserInfo.BatterySaving] as? Bool else { return }
            if !batterySaving {
                self.sendingDate = NSDate() // Send next GPS fixes soon as possible
            }
        })
    }
    
    // MARK: - Background Task
    
    private func beginGPSFixSendingInBackgroundTask() {
        sendingBackgroundTask = UIApplication.sharedApplication().beginBackgroundTaskWithName("Send GPS Fixes", expirationHandler: {
            self.endGPSFixSendingInBackgroundTask()
        })
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), {
            self.gpsFixController.sendSlice({ (withSuccess) in
                self.endGPSFixSendingInBackgroundTask()
            })
        })
    }
    
    private func endGPSFixSendingInBackgroundTask() {
        UIApplication.sharedApplication().endBackgroundTask(sendingBackgroundTask)
        sendingBackgroundTask = UIBackgroundTaskInvalid
    }
    
    // MARK: - Update
    
    func update(completion: () -> Void) {
        // TODO: RegattaData(checkIn)
        let regattaData = RegattaData(
            serverURL: checkIn.serverURL,
            eventID: checkIn.event.eventID,
            leaderboardName: checkIn.leaderboard.name,
            competitorID: "", // TODO: regatta.competitorID,
            markID: "" // TODO: regatta.mark?.markID
        )
        requestManager.getRegattaData(regattaData,
                                      success: { (regattaData) in self.updateSuccess(regattaData, completion: completion) },
                                      failure: { (error) in self.updateFailure(completion) }
        )
    }
    
    func updateSuccess(regattaData: RegattaData, completion: () -> Void) {
        checkIn.event.updateWithEventData(regattaData.eventData)
        checkIn.leaderboard.updateWithLeaderboardData(regattaData.leaderboardData)

        // TODO:
        // if (regatta.competitor != nil) {
        //     regatta.competitor!.updateWithCompetitorData(regattaData.competitorData)
        //}

        CoreDataManager.sharedManager.saveContext()
        completion()
    }
    
    func updateFailure(completion: () -> Void) {
        completion()
    }
    
    // MARK: - Tracking
    
    func startTracking() throws {
        isTracking = false
        try LocationManager.sharedManager.startTracking()
        isTracking = true
    }
    
    func stopTracking() {
        isTracking = false
        LocationManager.sharedManager.stopTracking()
    }
    
    // MARK: - TeamImage
    
    func postTeamImageData(imageData: NSData,
                           competitorID: String,
                           success: (teamImageURL: String) -> Void,
                           failure: (error: RequestManager.Error) -> Void)
    {
        requestManager.postTeamImageData(imageData,
                                         competitorID: competitorID,
                                         success: success,
                                         failure: failure
        )
    }
    
    // MARK: - CheckOut
    
    func checkOut(completion: (withSuccess: Bool) -> Void) {
        requestManager.postCheckOut(checkIn,
                                    success: { () in completion(withSuccess: true) },
                                    failure: { (error) in completion(withSuccess: false) }
        )
    }
    
    // MARK: - Properties
    
    lazy var gpsFixController: GPSFixController = {
        let gpsFixController = GPSFixController(checkIn: self.checkIn)
        return gpsFixController
    }()
    
    lazy var requestManager: RequestManager = {
        let requestManager = RequestManager(baseURLString: self.checkIn.serverURL)
        return requestManager
    }()
    
}

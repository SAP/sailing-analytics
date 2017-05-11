//
//  CheckIn.swift
//  SAPTracker
//
//  Created by Raimund Wege on 05.05.17.
//  Copyright © 2017 com.sap.sailing. All rights reserved.
//

import Foundation
import CoreData

@objc(CheckIn)
class CheckIn: NSManagedObject {

    func initialize() {
        event = CoreDataManager.sharedManager.newEvent(checkIn: self)
        leaderboard = CoreDataManager.sharedManager.newLeaderboard(checkIn: self)
    }

    func updateWithCheckInData(checkInData: CheckInData) {
        serverURL = checkInData.serverURL
        event.updateWithEventData(eventData: checkInData.eventData)
        leaderboard.updateWithLeaderboardData(leaderboardData: checkInData.leaderboardData)
    }

    func eventURL() -> URL? {
        return URL(string: "\(serverURL)/gwt/Home.html?navigationTab=Regattas#EventPlace:eventId=\(event.eventID)")
    }
    
    func leaderboardURL() -> URL? {
        guard let name = leaderboard.nameWithQueryAllowedCharacters() else { return nil }
        return URL(string: "\(serverURL)/gwt/Leaderboard.html?name=\(name)&showRaceDetails=false&embedded=true&hideToolbar=true")
    }
    
}

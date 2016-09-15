//
//  Regatta.swift
//  SAPTracker
//
//  Created by Raimund Wege on 04.07.16.
//  Copyright © 2016 com.sap.sailing. All rights reserved.
//

import Foundation
import CoreData

@objc(Regatta)
class Regatta: NSManagedObject {

    func updateWithRegattaData(regattaData: RegattaData) {
        serverURL = regattaData.serverURL
        teamImageURL = regattaData.teamImageURL
        teamImageRetry = false
        event.updateWithEventData(regattaData.eventData)
        leaderboard.updateWithLeaderboardData(regattaData.leaderboardData)
        competitor.updateWithCompetitorData(regattaData.competitorData)
    }
    
    func eventURL() -> NSURL? {
        return NSURL(string: "\(serverURL)/gwt/Home.html?navigationTab=Regattas#EventPlace:eventId=\(event.eventID)")
    }
    
    func leaderboardURL() -> NSURL? {
        guard let name = leaderboard.nameWithQueryAllowedCharacters() else { return nil }
        return NSURL(string: "\(serverURL)/gwt/Leaderboard.html?name=\(name)&showRaceDetails=false&embedded=true&hideToolbar=true")
    }
    
}

//
//  Leaderboard.swift
//  SAPTracker
//
//  Created by Raimund Wege on 04.07.16.
//  Copyright © 2016 com.sap.sailing. All rights reserved.
//

import Foundation
import CoreData

@objc(Leaderboard)
class Leaderboard: NSManagedObject {

    func nameWithQueryAllowedCharacters() -> String? {
        return name.stringByAddingPercentEncodingWithAllowedCharacters(.URLQueryAllowedCharacterSet())
    }
    
}

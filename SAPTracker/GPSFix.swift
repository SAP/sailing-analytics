//
//  GPSFix.swift
//  SAPTracker
//
//  Created by computing on 21/10/14.
//  Copyright (c) 2014 com.sap.sailing. All rights reserved.
//

import Foundation
import CoreData

@objc(GPSFix)
class GPSFix: NSManagedObject {

    @NSManaged var bearingDeg: Double
    @NSManaged var deviceUuid: String
    @NSManaged var latDeg: Double
    @NSManaged var lonDeg: Double
    @NSManaged var speedMperS: Double
    @NSManaged var timeMillis: Int64
    @NSManaged var sent: Bool

    func initWithDictionary(dictionary: Dictionary<NSObject, AnyObject>) {
        deviceUuid = DeviceUDIDManager.UDID
        timeMillis = Int64((dictionary["timestamp"] as Double) * 1000)
        latDeg = dictionary["latitude"] as Double
        lonDeg = dictionary["longitude"] as Double
        speedMperS = dictionary["speed"] as Double
        bearingDeg = dictionary["course"] as Double
    }
}

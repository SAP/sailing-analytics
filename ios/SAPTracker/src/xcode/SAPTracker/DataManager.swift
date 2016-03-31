//
//  DataManager.swift
//  SAPTracker
//
//  Created by computing on 21/10/14.
//  Copyright (c) 2014 com.sap.sailing. All rights reserved.
//

import Foundation
import CoreData

public class DataManager: NSObject {
    
    var selectedCheckIn: CheckIn?
    
    public class var sharedManager: DataManager {
        struct Singleton {
            static let sharedManager = DataManager()
        }
        return Singleton.sharedManager
    }
    
    override init() {
        super.init()
        
        print(managedObjectContext!)
        
        // store new locations to database
        NSNotificationCenter.defaultCenter().addObserver(self, selector: "newLocation:", name: LocationManager.NotificationType.newLocation, object: nil)
        
        // save context when done tracking
        NSNotificationCenter.defaultCenter().addObserver(self, selector: "trackingStopped:", name: LocationManager.NotificationType.trackingStopped, object: nil)
    }
    
    deinit {
        NSNotificationCenter.defaultCenter().removeObserver(self)
    }
    
    // MARK: - notification callbacks
    
    /* New location detected, store to database if location is valid. */
    func newLocation(notification: NSNotification) {
        if notification == NSNull() {
            return
        }
        var dict = notification.userInfo!;
        if dict["isValid"] as! Bool {
            let gpsFix = NSEntityDescription.insertNewObjectForEntityForName("GPSFix", inManagedObjectContext: self.managedObjectContext!) as! GPSFix
            gpsFix.initWithDictionary(dict)
            if selectedCheckIn == nil {
                abort();
            }
            gpsFix.checkIn = selectedCheckIn!
            saveContext()
        }
    }
    
    /* Tracking stopped, save data to disk. */
    func trackingStopped(notification: NSNotification) {
        saveContext()
    }
    
    // MARK: - public database access
    func getCheckIn(eventId: String, leaderBoardName: String, competitorId: String)->CheckIn? {
        let fetchRequest = NSFetchRequest()
        fetchRequest.entity = NSEntityDescription.entityForName("CheckIn", inManagedObjectContext: self.managedObjectContext!)
        fetchRequest.predicate = NSPredicate(format: "eventId = %@ AND leaderBoardName = %@ AND competitorId = %@", eventId, leaderBoardName, competitorId)
        do {
            let results = try self.managedObjectContext!.executeFetchRequest(fetchRequest)
            if results.count == 0 {
                return nil
            } else {
                return results[0] as? CheckIn
            }
        } catch {
            print(error)
        }
        return nil
    }
    
    func newCheckIn()->CheckIn {
        return NSEntityDescription.insertNewObjectForEntityForName("CheckIn", inManagedObjectContext: self.managedObjectContext!) as! CheckIn
    }
    
    func newEvent(checkIn: CheckIn) -> Event {
        let event = NSEntityDescription.insertNewObjectForEntityForName("Event", inManagedObjectContext: self.managedObjectContext!) as! Event
        event.checkIn = checkIn
        return event
    }
    
    func newLeaderBoard(checkIn: CheckIn) -> LeaderBoard {
        let leaderBoard = NSEntityDescription.insertNewObjectForEntityForName("LeaderBoard", inManagedObjectContext: self.managedObjectContext!) as! LeaderBoard
        leaderBoard.checkIn = checkIn
        return leaderBoard
    }
    
    func newCompetitor(checkIn: CheckIn) -> Competitor {
        let competitor = NSEntityDescription.insertNewObjectForEntityForName("Competitor", inManagedObjectContext: self.managedObjectContext!) as! Competitor
        competitor.checkIn = checkIn
        return competitor
    }
    
    /* Get latest locations. Limited by the max number of objects that can be sent. */
    func latestLocations() -> [GPSFix] {
        let fetchRequest = NSFetchRequest()
        fetchRequest.entity = NSEntityDescription.entityForName("GPSFix", inManagedObjectContext: self.managedObjectContext!)
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "timestamp", ascending: false)]
        fetchRequest.fetchLimit = APIManager.Constants.maxSendGPSFix
        var results: [AnyObject]? = nil
        do {
            try results = self.managedObjectContext!.executeFetchRequest(fetchRequest)
        } catch {
            print(error)
        }
        return results as! [GPSFix]
    }
    
    func countCachedFixes() -> Int {
        let request = NSFetchRequest()
        request.entity = NSEntityDescription.entityForName("GPSFix", inManagedObjectContext: self.managedObjectContext!)
        request.includesSubentities = false
        var error: NSError? = nil
        let count = self.managedObjectContext!.countForFetchRequest(request, error:&error)
        if(count == NSNotFound) {
            //Handle error
        }
        return count
    }
    
    /* Get all check ins. */
    func checkInFetchedResultsController()->NSFetchedResultsController {
        let fetchRequest = NSFetchRequest(entityName: "CheckIn")
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "leaderBoardName", ascending: true)]
        return NSFetchedResultsController(fetchRequest: fetchRequest, managedObjectContext: self.managedObjectContext!, sectionNameKeyPath: nil, cacheName: nil)
    }
    
    public func deleteCheckIn(checkIn: CheckIn) {
        self.managedObjectContext!.deleteObject(checkIn)
    }
    
    // MARK: - Core Data stack
    
    lazy var applicationDocumentsDirectory: NSURL = {
        // The directory the application uses to store the Core Data store file. This code uses a directory named "com.sap.sailing.ios.CoreData" in the application's documents Application Support directory.
        let urls = NSFileManager.defaultManager().URLsForDirectory(.DocumentDirectory, inDomains: .UserDomainMask)
        return urls[urls.count-1] 
    }()
    
    lazy var managedObjectModel: NSManagedObjectModel = {
        // The managed object model for the application. This property is not optional. It is a fatal error for the application not to be able to find and load its model.
        let modelURL = NSBundle.mainBundle().URLForResource("CoreData", withExtension: "momd")!
        return NSManagedObjectModel(contentsOfURL: modelURL)!
    }()
    
    lazy var persistentStoreCoordinator: NSPersistentStoreCoordinator? = {
        // The persistent store coordinator for the application. This implementation creates and return a coordinator, having added the store for the application to it. This property is optional since there are legitimate error conditions that could cause the creation of the store to fail.
        // Create the coordinator and store
        var coordinator: NSPersistentStoreCoordinator? = NSPersistentStoreCoordinator(managedObjectModel: self.managedObjectModel)
        let url = self.applicationDocumentsDirectory.URLByAppendingPathComponent("CoreData.sqlite")
        var error: NSError? = nil
        
        // http://stackoverflow.com/a/8890373
        // Check if we already have a persistent store
        if (NSFileManager.defaultManager().fileExistsAtPath(url.path!)) {
            do {
                let existingPersistentStoreMetadata = try NSPersistentStoreCoordinator.metadataForPersistentStoreOfType(NSSQLiteStoreType, URL: url)
                //            if (existingPersistentStoreMetadata == nil) {
                //                // Something *really* bad has happened to the persistent store
                //                NSException.raise(NSInternalInconsistencyException, format: "Failed to read metadata for persistent store %@: %@", arguments:getVaList([url, error!]));
                //            }
                
                if (!self.managedObjectModel.isConfiguration(nil, compatibleWithStoreMetadata: existingPersistentStoreMetadata)) {
                    do {
                        try NSFileManager.defaultManager().removeItemAtURL(url)
                    } catch {
                        print("*** Could not delete persistent store, %@", error);
                    } // else the existing persistent store is compatible with the current model - nice!
                } // else no database file yet
            } catch {
                print(error)
            }
        }
        
        let options = [NSMigratePersistentStoresAutomaticallyOption: 1, NSInferMappingModelAutomaticallyOption: 1]
        var failureReason = "There was an error creating or loading the application's saved data."
        do {
            try coordinator!.addPersistentStoreWithType(NSSQLiteStoreType, configuration: nil, URL: url, options: options)
        } catch {
            print(error)
            coordinator = nil
            // Report any error we got.
            let dict = NSMutableDictionary()
            dict[NSLocalizedDescriptionKey] = "Failed to initialize the application's saved data"
            dict[NSLocalizedFailureReasonErrorKey] = failureReason
//            dict[NSUnderlyingErrorKey] = error
//            error = NSError(domain: "YOUR_ERROR_DOMAIN", code: 9999, userInfo: dict as [NSObject : AnyObject])
//            // Replace this with code to handle the error appropriately.
//            // abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
//            NSLog("Unresolved error \(error), \(error!.userInfo)")
//            //abort()
//            NSFileManager.defaultManager().removeItemAtURL(url, error:nil);
//            return nil
        }
        return coordinator
    }()
    
    public lazy var managedObjectContext: NSManagedObjectContext? = {
        // Returns the managed object context for the application (which is already bound to the persistent store coordinator for the application.) This property is optional since there are legitimate error conditions that could cause the creation of the context to fail.
        let coordinator = self.persistentStoreCoordinator
        if coordinator == nil {
            return nil
        }
        var managedObjectContext = NSManagedObjectContext()
        managedObjectContext.persistentStoreCoordinator = coordinator
        return managedObjectContext
    }()
    
    // MARK: - Core Data Saving support
    
    func saveContext () {
        if let moc = self.managedObjectContext {
            if (moc.hasChanges) {
                do {
                    try moc.save()
                } catch {
                    print(error)
                    abort()
                }
            }
        }
    }
    
}
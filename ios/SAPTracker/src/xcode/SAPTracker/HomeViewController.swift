//
//  ViewController.swift
//  SAPTracker
//
//  Created by computing on 17/10/14.
//  Copyright (c) 2014 com.sap.sailing. All rights reserved.
//

import UIKit
import CoreData

class HomeViewController: UIViewController, UITableViewDataSource, UITableViewDelegate, UIActionSheetDelegate, NSFetchedResultsControllerDelegate, QRCodeManagerDelegate {
    
    enum AlertView: Int {
        case NoCameraAvailable
    }

    struct Keys {
        static let acceptedTerms = "acceptedTerms"
    }
    
    @IBOutlet weak var bottomNote: PaddedLabel!
    @IBOutlet weak var btnScan: UIButton!
    @IBOutlet weak var btnNoCode: UIButton!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var tableViewHeight: NSLayoutConstraint!
    @IBOutlet weak var activityIndicatorView: UIActivityIndicatorView!
    
    var fetchedResultsController: NSFetchedResultsController?
    private var qrCodeManager: QRCodeManager?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // set QR manager, needed in case app is being open by custom URL
        qrCodeManager = QRCodeManager(delegate: self)
        
        // set up data source for list
        fetchedResultsController = DataManager.sharedManager.checkInFetchedResultsController()
        fetchedResultsController!.delegate = self
        do {
            try fetchedResultsController!.performFetch()
        } catch {
            print(error)
        }
        
        // register for open custom URL events
        NSNotificationCenter.defaultCenter().addObserver(self, selector: #selector(openUrl(_:)), name: AppDelegate.NotificationType.openUrl, object: nil)

        NSNotificationCenter.defaultCenter().addObserver(self, selector: #selector(checkEULA(_:)), name: UIApplicationWillEnterForegroundNotification, object: nil)

        checkEULA(NSNotification.init(name: "", object: nil))

        navigationItem.title = NSLocalizedString("Header", comment: "")
        bottomNote.text = NSLocalizedString("QR found", comment: "")
        btnScan.setTitle(NSLocalizedString("Scan Code", comment: ""), forState: .Normal)
        btnNoCode.setTitle(NSLocalizedString("No Code", comment: ""), forState: .Normal)

        // add logo to top left
        let imageView = UIImageView(image: UIImage(named: "sap_logo"))
        let barButtonItem = UIBarButtonItem(customView: imageView)
        navigationItem.leftBarButtonItem = barButtonItem
    }

    deinit {
        NSNotificationCenter.defaultCenter().removeObserver(self)
    }

    func checkEULA(n: NSNotification) {
        // check that user accepted terms
        if !NSUserDefaults.standardUserDefaults().boolForKey(Keys.acceptedTerms) {

            let alert = UIAlertController(title: NSLocalizedString("EULA_title", comment: ""),
                message: NSLocalizedString("EULA_content", comment: ""),
                preferredStyle: .Alert)

            let viewAction = UIAlertAction(title: NSLocalizedString("EULA_view", comment: ""),
                style: .Cancel,
                handler: { action in
                    UIApplication.sharedApplication().openURL(URLs.EULA)
            })

            let confirmAction = UIAlertAction(title: NSLocalizedString("EULA_confirm", comment: ""),
                style: .Default,
                handler: { action in
                    let preferences = NSUserDefaults.standardUserDefaults()
                    preferences.setBool(true, forKey:Keys.acceptedTerms)
                    preferences.synchronize()
            })

            alert.addAction(viewAction)
            alert.addAction(confirmAction)
            presentViewController(alert, animated: true, completion: nil)
        }
    }

    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        DataManager.sharedManager.selectedCheckIn = nil;
    }

    func openUrl(notification: NSNotification) {
        let url = notification.userInfo!["url"] as! String
        qrCodeManager!.parseUrl(url)
    }
    
    // MARK: - UIActionSheetDelegate
    
    @IBAction func showActionSheet(sender: AnyObject) {
        let actionSheet = UIActionSheet(title: nil, delegate: self, cancelButtonTitle: nil, destructiveButtonTitle: nil, otherButtonTitles: NSLocalizedString("Settings", comment: ""), NSLocalizedString("About", comment: ""), NSLocalizedString("Cancel", comment: ""))
        actionSheet.cancelButtonIndex = 3
        actionSheet.showInView(view)
    }
    
    func actionSheet(actionSheet: UIActionSheet, clickedButtonAtIndex buttonIndex: Int) {
        switch buttonIndex{
         case 0:
            performSegueWithIdentifier("Settings", sender: actionSheet)
            break
        case 1:
            performSegueWithIdentifier("About", sender: actionSheet)
            break
        default:
            break
        }
    }
    
    // MARK: - UITableViewDataSource
    
    func resizeTable() {
        let info = fetchedResultsController!.sections![0] 
        let rows = info.numberOfObjects
        if rows < 3 {
            tableView.removeConstraint(tableViewHeight)
            tableViewHeight = NSLayoutConstraint(item: tableView, attribute: NSLayoutAttribute.Height, relatedBy: NSLayoutRelation.Equal, toItem: nil, attribute: NSLayoutAttribute.Height, multiplier: 1.0, constant: CGFloat(22 + 44 * rows))
            tableView.addConstraint(tableViewHeight)
            tableView.scrollEnabled = false
        } else {
            tableView.removeConstraint(tableViewHeight)
            tableViewHeight = NSLayoutConstraint(item: tableView, attribute: NSLayoutAttribute.Height, relatedBy: NSLayoutRelation.Equal, toItem: nil, attribute: NSLayoutAttribute.Height, multiplier: 1.0, constant: CGFloat(22 + 44 * 3))
            tableView.addConstraint(tableViewHeight)
            tableView.scrollEnabled = true
        }
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let info = fetchedResultsController!.sections![section] 
        resizeTable()
        return info.numberOfObjects
    }
    
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return NSLocalizedString("Your Regattas", comment: "")
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier("Regatta") as UITableViewCell!
        configureCell(cell, atIndexPath: indexPath)
        return cell
    }
    
    func configureCell(cell: UITableViewCell, atIndexPath indexPath: NSIndexPath) {
        let checkIn = fetchedResultsController!.objectAtIndexPath(indexPath) as! CheckIn
        cell.textLabel?.text = checkIn.leaderBoardName
    }
    
    // MARK: - NSFetchedResultsControllerDelegate
    
    func controllerWillChangeContent(controller: NSFetchedResultsController) {
        tableView.beginUpdates()
    }
    
    func controller(controller: NSFetchedResultsController, didChangeObject object: AnyObject,  atIndexPath indexPath: NSIndexPath?,
        forChangeType type: NSFetchedResultsChangeType,
        newIndexPath: NSIndexPath?) {
            switch type {
            case .Insert:
                tableView.insertRowsAtIndexPaths([newIndexPath!], withRowAnimation: UITableViewRowAnimation.Automatic)
            case .Update:
                let cell = tableView.cellForRowAtIndexPath(indexPath!)
                if cell != nil {
                    configureCell(cell!, atIndexPath: indexPath!)
                    tableView.reloadRowsAtIndexPaths([indexPath!], withRowAnimation: UITableViewRowAnimation.Automatic)
                }
            case .Move:
                tableView.deleteRowsAtIndexPaths([indexPath!], withRowAnimation: UITableViewRowAnimation.Automatic)
                tableView.insertRowsAtIndexPaths([newIndexPath!], withRowAnimation: .Automatic)
            case .Delete:
                tableView.deleteRowsAtIndexPaths([indexPath!], withRowAnimation: UITableViewRowAnimation.Automatic)
            }
    }
    
    func controllerDidChangeContent(controller: NSFetchedResultsController) {
        tableView.endUpdates()
    }
    
    
    // MARK: - UITableViewDelegate
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        DataManager.sharedManager.selectedCheckIn = (fetchedResultsController!.objectAtIndexPath(indexPath) as! CheckIn)
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
        performSegueWithIdentifier("Regatta", sender: tableView)
    }
    
    // MARK: - Button actions
    
    @IBAction func scanButtonTap(sender: AnyObject) {
        if !UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.Camera) {
            let alertView = UIAlertView(title: NSLocalizedString("No camera available.", comment: ""), message: nil, delegate: nil, cancelButtonTitle: NSLocalizedString("Cancel", comment: ""))
            alertView.tag = AlertView.NoCameraAvailable.rawValue;
            alertView.show()
            return
        }
        
        if (!QRCodeManager.deviceCanReadQRCodes()) {
            let alertView = UIAlertView(title: NSLocalizedString("Cannot read QR codes with this device.", comment: ""), message: nil, delegate: nil, cancelButtonTitle: NSLocalizedString("Cancel", comment: ""))
            alertView.tag = AlertView.NoCameraAvailable.rawValue;
            alertView.show()
            return
        }
        performSegueWithIdentifier("Scan", sender: sender)
    }
    
    @IBAction func noQrCodeButtonTap(sender: AnyObject) {
        let alertView = UIAlertView(title:  NSLocalizedString("In order to use this app you need to check-in via QR code or email link. Please contact the racing committee if you need either.", comment: ""), message: nil, delegate: nil, cancelButtonTitle: NSLocalizedString("Cancel", comment: ""))
        alertView.tag = AlertView.NoCameraAvailable.rawValue;
        alertView.show()
    }

}


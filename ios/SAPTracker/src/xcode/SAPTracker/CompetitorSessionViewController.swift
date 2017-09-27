//
//  CompetitorSessionViewController.swift
//  SAPTracker
//
//  Created by Raimund Wege on 13.09.17.
//  Copyright © 2017 com.sap.sailing. All rights reserved.
//

import UIKit

class CompetitorSessionViewController: SessionViewController {
    
    struct CompetitorSessionSegue {
        static let EmbedCompetitor = "EmbedCompetitor"
    }
    
    weak var competitorCheckIn: CompetitorCheckIn!
    weak var competitorCoreDataManager: CoreDataManager!
    weak var competitorViewController: CompetitorViewController?
    
    // MARK: - OptionSheet
    
    func makeCompetitorOptionSheet() -> UIAlertController {
        let alertController = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        if let popoverController = alertController.popoverPresentationController {
            popoverController.barButtonItem = self.optionButton
        }
        alertController.addAction(self.makeActionCheckOut())
        //alertController.addAction(self.makeActionReplaceImage())
        //alertController.addAction(self.makeActionUpdate())
        alertController.addAction(self.makeActionSettings())
        alertController.addAction(self.makeActionInfo())
        alertController.addAction(self.makeActionCancel())
        return alertController
    }
    
    fileprivate func makeActionReplaceImage() -> UIAlertAction {
        return UIAlertAction(title: Translation.CompetitorView.OptionSheet.ReplaceImageAction.Title.String, style: .default) { [weak self] action in
            self?.competitorViewController?.showSelectImageAlert()
        }
    }
    
    // MARK: - Segues
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        super.prepare(for: segue, sender: sender)
        if (segue.identifier == CompetitorSessionSegue.EmbedCompetitor) {
            if let competitorViewController = segue.destination as? CompetitorViewController {
                competitorViewController.competitorCheckIn = competitorCheckIn
                competitorViewController.competitorSessionController = competitorSessionController
                competitorViewController.competitorCoreDataManager = competitorCoreDataManager
                self.competitorViewController = competitorViewController
            }
        }
    }
    
    // MARK: - Properties
    
    lazy var competitorSessionController: CompetitorSessionController = {
        return CompetitorSessionController(checkIn: self.competitorCheckIn, coreDataManager: self.competitorCoreDataManager)
    }()
    
}

//
//  TrainingCompetitorViewController.swift
//  SAPTracker
//
//  Created by Raimund Wege on 21.08.17.
//  Copyright © 2017 com.sap.sailing. All rights reserved.
//

import UIKit

class TrainingCompetitorViewController: CompetitorSessionViewController {
    
    struct Segue {
        static let EmbedTraining = "EmbedTraining"
    }
    
    weak var trainingViewController: TrainingViewController?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        delegate = self
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        refresh(false)
    }
    
    // MARK: - Setup
    
    fileprivate func setup() {
        setupLocalization()
        setupNavigationBar()
    }
    
    fileprivate func setupLocalization() {
        startTrackingButton.setTitle(Translation.CompetitorView.StartTrackingButton.Title.String, for: .normal)
    }
    
    fileprivate func setupNavigationBar() {
        navigationItem.titleView = TitleView(title: competitorCheckIn.event.name, subtitle: competitorCheckIn.leaderboard.name)
        navigationController?.navigationBar.setNeedsLayout()
    }
    
    // MARK: - Segues
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        super.prepare(for: segue, sender: sender)
        if (segue.identifier == Segue.EmbedTraining) {
            if let trainingViewController = segue.destination as? TrainingViewController {
                trainingViewController.delegate = self
                trainingViewController.trainingCheckIn = competitorCheckIn
                trainingViewController.trainingCoreDataManager = competitorCoreDataManager
                self.trainingViewController = trainingViewController
            }
        }
    }
    
}

// MARK: - SessionViewControllerDelegate

extension TrainingCompetitorViewController: SessionViewControllerDelegate {
    
    var checkIn: CheckIn { get { return competitorCheckIn } }
    
    var coreDataManager: CoreDataManager { get { return competitorCoreDataManager } }
    
    var sessionController: SessionController { get { return competitorSessionController } }
    
    func makeOptionSheet() -> UIAlertController {
        return makeCompetitorOptionSheet()
    }
    
    func refresh(_ animated: Bool) {
        competitorViewController?.refresh(animated)
        trainingViewController?.refresh(animated)
    }
    
}

// MARK: - TrainingViewControllerDelegate

extension TrainingCompetitorViewController: TrainingViewControllerDelegate {
    
    func trainingViewController(_ controller: TrainingViewController, startTrackingButtonTapped sender: Any) {
        super.startTrackingButtonTapped(sender)
    }
    
    func trainingViewController(_ controller: TrainingViewController, leaderboardButtonTapped sender: Any) {
        super.leaderboardButtonTapped(sender)
    }
    
    func trainingViewControllerDidFinishTraining(_ controller: TrainingViewController) {
        updatePessimistic()
    }
    
    func trainingViewControllerDidReactivateTraining(_ controller: TrainingViewController) {
        updatePessimistic()
    }
    
}

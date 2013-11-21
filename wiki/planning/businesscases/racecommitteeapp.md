# RaceCommittee App

Author: Simon Pamies

## Rationale
As SAP is aiming to make sailing visible and understandable for everyone and also wants to help series and regattas run better, it is crucial to have a direct connection to the race committee and/or umpires. Every decision they make, must in realtime be reflected into the Sailing Analytics application. This holds not only for time announcements but also for complex events like course changes or rule infringements.

With this information the public can closely stay in touch with events and the race itself. Especially when races take place far away from shore and there is no direct line of sight. In addition to that external partners (e.g. TV production) can be fed with accurate information about time frames of a race, data providers can improve their handling based on the stream of data that is produced by the race committee. Last but not least the application itself can rely on these data in order to make decisions on how to display/compute analytics and other useful information.

## Purpose

Provide the race committee with a native mobile implementation, that not only accepts input that is then fed into the analytics server but also helps users doing their job better by providing useful information about course layout, wind, competitors and time. This app should work for a professional race committee but also for a sailor executing a regatta on a sunday afternoon.

The app should exist in two version:

- The first one is dedicated to professionals that run big to mid-sized regattas. This version would heavily rely on tracking and wind measurement devices and the Sailing Analytics server. It would require professional support.

- The second one will be publicly available in the app store. This version will be independent from an advanced tracking infrastructure and will be restricted in it's features. It will still require support from SAP in terms of server infrastructure and general user support.

## Features

Display **general** information about
* Regattas
* Courses
* Races

For **each race** display information about
* Time
  * Start
  * Duration
* Flags
* Marks (including their position) that can be used to create a course
* Current true wind direction and speed
* Competitors with name and sail number and tracker state

For each race provide the user with **tools to edit**
* Status
  * Start
  * Recall
  * Abort
  * Special flags
* Time
  * Start
* Course
  * Mark positions
  * Layout
  * Legs
* Wind
  * Direction
  * Speed

## Description

As there currently exist an implementation that covers some of the features, the aim of this project is to re-design and re-implement some of the code of the prototype. This especially holds for the user interface that currently needs some revamp to be also accessible to the average user.

This project can be divided into three major stages:

- The first one is to design a user interface that one the one hand please the eye and is tailored to a mobile touch device. This design should take into account all of the desired features and provide a mock-up like interface for both versions. It has been agreed that this step needs to be taken out by an external agency that has a mobile background, the skills and the experience. This step relies on a document that is yet to be built that describes the desired features and the clustering of these features with regards to the two versions.

- The second step is about integrating the design with the Sailing Analytics server. This requires a major effort from the tech team and also team work between the external agency and our developers. Some work from the current prototype can be reused but a major part needs to be rewritten.

- The third stage is about to make sure that the infrastructure is ready to handle the traffic that follows a successful deployment to an app store. We cannot judge exactly how much interest there will be in the sailing community for such a solution but aiming for around 100 active users a day without active advertising feels like the right number at the start. Nevertheless we should be prepared to handle up to 1000 concurrent users a day during the first three months. In addition to that we will need to have some sort of support ready to react to user questions and feedback. This also includes an up to date documentation and FAQ that is linked to the app.

## Synergies

Providing the public with such an app could quickly raise synergies with other software providers that could want to integrate with our solution.

## Risks

It is currently unclear how much interest can be expected by the sailor community for the public version of the app. In contrast to that the efforts and risks that need to be taken into account developing such an app are big. Not only that we can't judge how much work such a complex application would require from the external agency, but also that the risks deploying and supporting an app that is downloadable by everyone can't just be numbered. 

In addition to that it is unknown if the feature set a first public version would have then, really would meet the expectations of the target group. In the worst case the community just won't use the app and that could damage the brand.

The development of the professional version has the same risk factors as the public version regarding the development of the interface. As the feature set is known the risks of failure are much lower than for the public version.

## Prototype

A working prototype has been developed and has also been used quite successful in regattas all over the world.

## Estimation

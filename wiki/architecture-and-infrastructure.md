# Architecture and Infrastructure

### Table of Contents

* [[Runtime Environment|wiki/runtime-environment]]
* [[Basic Architectural Principles|wiki/basic-architectural-principles]]
* [[Development Environment|wiki/development-environment]]
* [[Production Environment|wiki/production-environment]]
* [[Typical Development Scenarios|wiki/typical-development-scenarios]]

## Introduction, Project Background and History

The SAP Sailing Analytics are a technology show-case demonstrating SAP technologies, concepts, skills and values applied to the domain of regatta sailing. They started as a small tool primarily intended to support a commentator in his job by displaying a live leaderboard for a sailing regatta with data interesting for the commentary. GPS and wind data travel from sensors to the server where the application keeps it in memory. When a request for a leaderboard is received, the data is aggregated on the fly, performing geometric computations including wind projections and involving a virtual "advantage line" orthogonal to the wind direction.

The live leaderboard started as a web application with a Java back-end responsible for the connectivity with the sensors and providing the geometry engine, and a Python process rendering the Web UI for the client's browser. The Python process issued REST requests to the Java back-end which responded with JSON documents.

The solution was first shown at Kieler Woche 2011. At the time, it was capable of displaying a single leaderboard that showed a number of tracked races in numerical form, offering columns for overall rank, race rank, rank at a mark, and values for average speed, distance traveled, gap to leader in seconds, velocity made good (VMG), estimated time of arrival at the next mark and current speed over ground. It was prototypical in many regards but regardless was considered an improvement for the commentary. The sailors liked it too because for the first time they could see numerical evidence of their choices of speed over distance.

After Kieler Woche 2011, the architecture changed. We removed the Python engine and used the Google Web Toolkit instead to render the Web UI directly in the Java process. A first new live leaderboard with this approach was shown at the IDM Travemünde 2011 and later at the MdM Hamburg 2011 events. Over time, the solution learned to manage multiple leaderboards, combining historic race analysis with live tracking. Particularly the accumulation of historic race data will require changes in the architecture in the near future to support this use case better.
A Google Map visualization, originally intended primarily for debugging purposes, matured to a useful tool used by commentators and spectators alike, combined with charts showing wind and competitor data, and of course the traditional live leaderboard. The leaderboard itself received various enhancements over time, including data about maneuvers such as tacks, jibes and penalty circles, and additional figures such as the average cross-track error which under shifty wind conditions in some boat classes may be an indicator for the risk taken by a competitor. Some of these figures turned out to be quite expensive to compute. Therefore, in a few cases we deviated from the original approach where everything was computed on the fly upon receiving a request. Instead, the more expensive calculations in live mode now happen asynchronously in the background, and client requests are fulfilled with whatever the most current result for these figures is.

The REST/JSON APIs offered by the Java back-end have been exploited by at least two additional show-case scenarios. Already in 2011, Business Objects Dashboards displayed data extracted through these interfaces in various analytical views. In 2012, the interfaces started to be used for repeated extraction of data into a HANA database on top of which Experience UI technology is now used for visualization with sophisticated analyses.

In 2012, a mobile application to support the race committees in their functions has been developed using largely the same architecture. Although the server for this app currently runs in a separate process, it uses largely the same code base, versioning repository and build process. We plan to integrate it with the SAP Sailing Analytics soon. A first loose coupling will allow users of the mobile app to send wind data entered on a mobile device into the SAP Sailing Analytics back-end where it augments the wind-based calculations. Later, we plan to integrate the mobile app even closer so that it supports race officials in laying and moving marks, changing the course layout as well as detecting and announcing disqualifications.

The remainder of this document explains the key architectural principles on which the SAP Sailing Analytics have been developed. It is to be considered a snapshot of the status quo, as documented by the time stamp in the document's header.

See also this [[presentation|https://git.wdf.sap.corp:50000/git/?p=SAPSail/sapsailingcapture.git;a=blob;f=doc/SAPSailingAnalyticsArchitecture.pptx;h=f304746a0092410f0bf625292a7ae363d6d05f15;hb=8010ce3ebcd0df0d3fcd1651f87d79724f667377]] that provides an overview of the project's architecture and history.
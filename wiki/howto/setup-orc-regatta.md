# Setting up an ORC Regatta with Polar Curve Scoring (PCS) (formerly "Performance Curve Scoring")

[[_TOC_]]

## Introduction

The Offshore Racing Congress (ORC, see [https://orc.org](https://org.org)) has defined a process and set of [rules](https://www.orc.org/rules/ORC%20Rating%20Systems%202019.pdf) to measure, rate and rank boats in regattas that is based on the boats' performance as predicted by a so-called "[Velocity Prediction Program](https://orc.org/index.asp?id=21)" (VPP). The output of this program is a measurement certificate, as shown [here](https://data.orc.org/public/WPub.dll/CC/64934.pdf). Such a certificate contains predicted boat speeds for different true wind angles (TWAs) and wind speeds, displayed in table form, like this:

![Certificate Excerpt](/wiki/images/orc/certificate-excerpt.png)

With this, boats in a regatta can be ranked according to their performance relative to their performance predicted.

The key concept behind this so-called *Polar Curve Scoring* is for each boat in a regatta to derive a mathematical function---the polar curve---based on the certificate's data and the course to sail that maps the wind speed on the course to the time the boat is expected to require to sail the course. See the following figure for an example of such a polar curve.

<center><img alt="Polar Curve Example Graphics" src="https://data.orc.org/tools/ex1.png"/></center>

In doing so, the course can be modeled at different levels of detail, depending largely on how stable and/or predictable the wind conditions are or were while sailing the course. If rather stable wind conditions are met on a leg, the leg can be defined as a "constructed" course where the leg's distance and true wind angle are specified. For long legs with changing wind conditions, boat performance is averaged across a profile of different assumed wind directions on that leg, with variants called *circular random* and *coastal long distance*.

The polar curve function for each boat can also be inverted, mapping the time elapsed for a competitor while sailing the course to the wind speed this competitor would have required according to the prediction to sail that course in this time. This wind speed is called the *implied wind*.

## The two Different Versions of Polar Curve Scoring

The process of how polar curve scoring is to be applied has changed in 2015. Now, it is up to the discretion of a regatta to decide which version to use.

### Before 2015 - Individual Implied Wind

With the polar curve of each boat in the fleet, for each competitor the implied wind for the course sailed can be computed. These implied wind speeds are used to rank the competitors in the race: the higher the implied wind, the better the boat ranks.

In order to obtain a corrected time for all boats that is consistent with the ranking by implied wind, the monotonous polar curve function of a selected boat---called the *scratch boat*---is evaluated for the implied wind values of each competitor, putting each competitors performance into the "coordinate system" of the scratch boat selected.

Different strategies for selecting a scratch boat exist, and there don't seem to be strict prescriptions for the selection. Regularly, the boat with the least "general performance handicap" (GPH), making it the fastest boat in the field on average, is used as scratch boat, but in other cases the winning boat may be selected. It can also happen that the boat that would regularly be selected as the scratch boat is disqualified or does not score in the race for other reasons. In this case, another scratch boat must be selected for corrected times calculations, and it may be an arbitrary one picked by the race committee. The selection does not influence the overall ranking; it only affects the differences and absolute numbers of the corrected times calculated for all competitors.

### Since 2015 - Corrected Times with General Implied Wind

In 2015 the congress suggested a different way of ranking races with polar curve scoring. Instead of using the implied wind values of each boat as the ranking criterion, one implied wind value---usually the maximum of all implied wind values obtained for the fleet---is applied to all competitors' polar curve to determine the time in which they are expected to finish sailing the course given under the implied wind speed selected. The actual elapsed times of each boat can then be compared to their individual time predicted, and the difference is used for ranking the fleet.

The selection of the single implied wind speed used to determine all time allowances may also be overruled by the race committee, e.g., in case there is concern that even the maximum implied wind speed achieved during the race is not close to the actual wind speed on the course at the time of sailing. Alternatively, e.g., in case of separately ranking sub-groups of a larger fleet, a sub-group's implied wind speed used for ranking may come from the overall group of boats, assuming that in a larger group of competitors chances are better to achieve a maximum implied wind speed that is close to the actual wind speed on the course.

With this method, corrected times are originally provided as a relative difference only. If the highest implied wind speed achieved was used for ranking and the best-ranking boat stayed in the range between six and 20 knots of implied wind, that boat's relative corrected time would be 00:00:00 because its elapsed time led to the implied wind used again to compute the allowance, which---by definition of the inverted performance-curve function---has to provide the elapsed time again for this competitor.

In order to obtain absolute corrected times, similar to the scratch boat selection of the pre-2015 method, some baselining has to occur. Here, selecting a "scratch boat" makes that boat's elapsed time the baseline for applying all relative corrected time differences, simply by adding them. For example, if the boat winning the race is selected as this scratch boat and that boat stays within the 6-20kts implied wind speed range, that boat's relative corrected time will be 00:00:00 and all other boats' relative corrected time will be greater than this because they ranked worse, and hence the winning boat's corrected time under these circumstances would equal its elapsed time, whereas all other boat's corrected time results from adding their relative corrected time to the winning boat's elapsed time.

## Configuring ORC Polar Curve Scoring

The SAP Sailing Analytics support scoring regattas using the different flavors of ORC Polar Curve Scoring (PCS). This requires a number of configuration steps:

* selecting the variant of ORC PCS (see above)
* providing the measurement certificates for all boats in the fleet
    * for the entire regatta
    * individually overriding for single races in the regatta
* Optionally:
    * define the course legs explicitly instead of using tracking data for course geometry and TWAs
    * select a scratch boat explicitly
    * define the implied wind to use for ranking explicitly

### Creating a Regatta with Ranking Based on ORC Polar Curve Scoring

To create a regatta using ORC Polar curve scoring, pick one of the following three ranking metrics:

![New Ranking Metrics](/wiki/images/orc/ranking-metrics.png)

Also note the tool tips on the options as you hover your pointer over each one of them.

* ORC Polar Curve (>= 2015): This uses a common implied wind for the fleet (usually the maximum implied wind obtained from the race under consideration), calculates the time allowances for all boats in this wind and determines the relative corrected times which are then added to the elapsed time of a scratch boat.
* ORC Polar Curve with Individual Implied Wind (<2015): The ranking is based on the implied wind achieved by each individual boat; corrected times are based on mapping those implied wind values to a time allowance based on a scratch boat's polar curve. By default, the scratch boat is selected as the one with the least GPH value but can be overruled by setting a scratch boat explicitly.
* ORC Polar Curve (>=2015, leader as baseline): same as the previous one, only that by default the scratch boat is selected as the leader in the race. For implied wind values between six and twenty knots this will mean that the leader's corrected time equals its elapsed time.

### Managing Certificates

Before polar curve scoring does anything useful, measurement certificates need to be imported and assigned to the competitors in the regatta. It is also possible to handle per-race exceptions in case the race committee decides that for a boat different allowance shall apply in one race but not in others. Reasons could range from accounting for damages to short-term changes to the boat's configuration or a "regatta" that consists of races run across an entire season with new certificates being issued before all races of the "regatta" have completed.

To assign certificates to competitors for a regatta, go to the "Regatta" tab in the AdminConsole. For those regattas using an ORC ranking metric, an additional action is displayed:

![New Certificates Action](/wiki/images/orc/regatta-certificates-0.png)

Assuming that your regatta already has competitors and boats assigned, the boat list will now be displayed in a table on the left, whereas an empty "Certificates" tables is shown to the right:

![Certificates Dialog](/wiki/images/orc/regatta-certificates-2.png)

There are currently two ways and two formats supported for certificates import: documents in JSON and RMS format can either be uploaded or referenced by a set of URLs pointing at the documents available for download. Use the "Certificate download URLs" control to edit a set of URLs from where certificates can be obtained; use the "Choose Files" button to select RMS and / or JSON files containing ORC certificates from your file system. Then use the ``Import Certificates`` button which will upload any local files selected to the server and will instruct the server to download the certificate files referenced by URL. The combined set of certificates will be obtained and displayed in the "Certificates" table on the right.

You can also search for certificates by providing one or more search criteria in the "Search" section of the dialog. As you hit the "Search" button, all certificates found based on your criteria will be added to the table on the right.

You can also select one or more boats in the boats table on the left and then press the "Suggest certificates from ORC website for selected boats" button. This will perform an online look-up based on the boats' parameters, such as name, sail number and boat class. Again, all certificates found this way will be added to the table on the right. Should an exact match result for a boat that has no certificate assigned yet, that match will automatically be assigned to the boat.

[![Certificate Import](/wiki/images/orc/regatta-certificates-import-1-small.png)](/wiki/images/orc/regatta-certificates-import-1-small.mp4)

A good source for certificate files is of course the ORC web site. [https://www.orc.org/index.asp?id=44](https://www.orc.org/index.asp?id=44) has the current certificates listed by issuing country. Note, though, that the country authority issuing a certificate does not necessarily have to be the same as the nationality of the sail number. [https://data.orc.org/public/WPub.dll?action=SrchCert&xslp=scert.php](https://data.orc.org/public/WPub.dll?action=SrchCert&xslp=scert.php) offers a comprehensive search form, but you need to apply for an account which can be done [here](https://data.orc.org/public/register.php). While we plan (see also section [Outlook](/wiki/howto/setup-orc-regatta#setting-up-an-orc-regatta-with-performance-curve-scoring-pcs_outlook) and [#5128 (Support ORC certificate search and/or use of RefNo to construct a certificate download URL)](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5128)) to support a more automated way of searching and downloading matching certificates, for now dedicated certificates can be found via the ORC search form. A typical search result looks like this:

![Search Result](/wiki/images/orc/search-result.png)

Use the reference number, say ``FRA00013881`` to download the certificate, either in RMS format like this: ``http://data.orc.org/public/WPub.dll?action=DownBoatRMS&RefNo=FRA00013881`` or in JSON format like that: ``http://data.orc.org/public/WPub.dll?action=DownBoatRMS&RefNo=FRA00013881&ext=json``.

Next, each boat needs to have a certificate assigned to it. This works by first selecting the boat on the left, then selecting the certificate to assign to that boat on the right. Should this constitute an assignment *change* then a warning will pop up, so as to avoid accidentally overwriting previous assignments. Use the sorting and filtering capabilities to find the matches and to finally check that all boats have a certificate assigned.

[![Certificate Import](/wiki/images/orc/regatta-certificates-assignment-1-small.png)](/wiki/images/orc/regatta-certificates-assignment-1-small.mp4)

The same dialog is available at race level. See the following two screenshots for where to reach them.

![Certificates Dialog from Leaderboard Races](/wiki/images/orc/certificate-assignment-per-race-from-leaderboard.png)
![Certificates Dialog from Smartphone Tracking Races](/wiki/images/orc/certificate-assignment-per-race-from-smartphone-tracking.png)

### Managing Courses

To score a race using ORC Performance Curve Scoring (PCS), a definition of the course must be provided that for each leg specifies how the time allowances for that leg will be determined. The following choices need to be made:

* What is the (rhumb-line) length of the leg?
* Is the leg "constructed," with a defined true wind angle (*TWA*) in the sense that such a TWA can be defined as characteristic for this leg?
* Or is the leg of one of the pre-defined types (*WINDWARD_LEEWARD*, *LONG_DISTANCE*, *NON_SPINNAKER*, or *CIRCULAR_RANDOM*)?
* And if you'd like to rank and score by *WINDWARD_LEEWARD*, would you still want to use more precise upwind and downwind allowances based on the actual leg types inferred from the tracking data as long as competitors have not yet finished their race?

For *WINDWARD_LEEWARD*, *LONG_DISTANCE*, *NON_SPINNAKER*, and *CIRCULAR_RANDOM* a predefined mix of allowances is used along the leg, assuming a certain distribution of wind angles, such as 50% upwind and 50% downwind for the *WINDWARD_LEEWARD* type. For in-race ranking especially for shorter *WINDWARD_LEEWARD* courses with a more or less stable wind direction the averaging that takes place has the major disadvantage of not taking into account the performance differences of boats between upwind and downwind legs. For example, as long as all competitors are still in the first upwind beat, allowances will be calculated based on the average across upwind and downwind performance. Yet, modeling the course as a "constructed course" with according upwind and downwind legs may lead to slightly different results because the certificates list allowances rounded to one decimal place, for beat, run, as well as for the combined *WINDWARD_LEEWARD* allowances, such that the *WINDWARD_LEEWARD* value is sometimes not exactly the average of the beat and run allowances. This can lead to subtle, yet confusing deviations in final calculated times. You may consider using the *WINDWARD_LEEWARD_REAL_LIVE* leg type in such cases if reasonable tracking and wind data exist. In this case, for boats still in the race (also during replay) the allowances will be computed assuming the course was a "constructed" course, using the leg length provided, and the true wind angle obtained from the tracking data. Only as boats cross the finish line, the *WINDWARD_LEEWARD* averages will be used for all legs. While this may lead to a small "jump" in calculated times as a competitor crosses the finish line, particularly if some reaching offset legs are involved, we think it still represents a useful combination between good in-race ranking and accurate result reporting.

The following video demonstrates the definition of a course for a simple up-and-down race with a short offset leg after mark 1 where the tracking data provides measures for TWAs and leg distances which can be taken over into the "official" course definition. Note that here instead of picking *WINDWARD_LEEWARD* for all legs, the constructed / TWA type is used for more accurate results, specifically during live tracking and live ranking. In particular, the offset leg is modeled more accurately this way, and since allowance relations for upwind and downwind may vary across the fleet significantly, live ranking in the middle of an upwind leg or the middle of a downwind leg will be more realistic than if modeling the entire course with the *WINDWARD_LEEWARD* average. The video also shows how a desired total course distance of exactly 4.500 nautical miles is then defined by spreading this distance proportionately across the legs according to their tracked distance.

[![Course Modeling](/wiki/images/orc/course-modeling-1-small.png)](/wiki/images/orc/course-modeling-1-small.mp4)

If the course consists of several waypoints, thus splitting it into legs, and the entire course shall be scored as, say, a *CIRCULAR_RANDOM* scheme, all legs shall be modeled as *CIRCULAR_RANDOM*. If a total course distance is known but individual leg distances can only be approximated, the desired or defined total distance can be distributed proportionately across the legs based on their estimated, approximated length.

### Scratch Boat Selection

A so-called "scratch boat" is used in both, the pre-2015 and since-2015 variants in order to produce absolute corrected times for all competitors in a race. In the pre-2015 version the scratch boat provides the polar curve for the course sailed that is used to map all individual implied winds achieved by each competitor to a comparable absolute time. In the since-2015 version the role of the scratch boat is to provide an absolute elapsed time to which the relative corrected times can be added to obtain absolute corrected times which can be compared.

If no scratch boat is selected explicitly, one is found by a default rule. For the pre-2015 variant this is the boat that has the "physical lead" or the first ship home if a boat has already passed the finish line. For the since-2015 variant the scratch boat selection defaults to the boat with the least GPH (general performance handicap) value listed in the certificate (which obviously needs overriding in case this boat does not finish a race regularly). The "leader as baseline" variant of the since-2015 rule will, instead of using the GPH, pick the boat with the least relative corrected time. Please note that compared to picking the boat with the GPH this requires significantly more computation and can slow things down a bit during live operations. You may consider using the "least GPH" default while live, and if you feel necessary, switch to "leader as baseline" only after the race.

The following video illustrates the explicit selection of a scratch boat. Note how at the end of the video the explicit scratch boat selection is cancelled by holding the Ctrl-key while clicking on the previously selected scratch boat, this way de-selecting it and returning to the default way of inferring the scratch boat, as explained above.

[![Course Modeling](/wiki/images/orc/setting-scratch-boat-1-small.png)](/wiki/images/orc/setting-scratch-boat-1-small.mp4)

### Implied Wind Selection

For the ORC PCS variant in use since 2015, a single implied wind value is used to compute relative corrected times which in turn are the basis for ranking the competitors in the race. By default, the implied wind speed to use for this is the greatest implied wind speed in the race's fleet. However, we have seen two scenarios where this is not the value desired by the race committee.

* The actual wind on the course may have been significantly stronger than what the implied wind calculation came up with. A typical situation would be massive sea state with choppy waves which lets the boats sail far slower than predicted by the VPP. The race officials may then decide to score the race based on a greater implied wind speed.
* The set of competitors to score in a race is a subset of a larger fleet that races on the same course under the same conditions. If this overall race is modeled as a separate race with its own overall scoring, that race may lend itself as source for the implied wind for each of the sub-groups, too, because its maximum implied wind is guaranteed to be greater than or equal to the maximum implied wind of each of the sub-groups and therefore has a better chance of matching the real wind speed on the course.

The action buttons for setting the implied wind for the since-2015 ORC PCS variants can be found at the same level as the buttons for setting the scratch boat for a race explicitly. See the following video.

[![Course Modeling](/wiki/images/orc/setting-implied-wind-1-small.png)](/wiki/images/orc/setting-implied-wind-1-small.mp4)

## Outlook

Especially the course definition for ORC PCS and probably also the scratch boat and implied wind selection are good candidates for support in the *SAP Sailing Race Manager App*. They clearly fall into the same set of responsibilities addressed by the app so far.

Further feature requests related to ORC Polar Curve Scoring have already been noted and will be tackled as time and resources permit. See, in particular [#5147 (Offer contiguous scoring based on corrected time or implied wind for multi-fleet handicap regattas)](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5147), and [#5115 (Enable user to copy the leg data / course definition from one race to one or more others)](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5115).
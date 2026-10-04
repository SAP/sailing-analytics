# ORC Handicap/Scoring System

To understand how a race is scored by the ORC system, read the following articles:
https://bugzilla.sapsailing.com/bugzilla/attachment.cgi?id=146
https://orc.org/index.asp?id=31

---

## ORC Certificate Information

The link to obtain the certificates for a country is

  http://data.orc.org/public/WPub.dll?action=DownRMS&CountryId=GER
  
(in this case for three-letter IOC code "GER" for germany).

### Example Line

```
NATCERTN.FILE_ID SAILNUMB    NAME                    TYPE              BUILDER           DESIGNER          YEAR CLUB                                OWNER                               ADRS1                               ADRS2                               C_Type   D CREW DD_MM_yyYY HH:MM:SS  LOA   IMSL   DRAFT  BMAX   DSPL  INDEX    DA   GPH    TMF    ILCGA  PLT-O PLD-O   WL6    WL8    WL10   WL12   WL14   WL16   WL20   OL6    OL8    OL10   OL12   OL14   OL16   OL20   CR6    CR8    CR10   CR12   CR14   CR16   CR20   NSP6   NSP8   NSP10  NSP12  NSP14  NSP16  NSP20   OC6    OC8    OC10   OC12   OC14   OC16   OC20  UA6   UA8   UA10  UA12  UA14  UA16  UA20  DA6   DA8  DA10  DA12  DA14  DA16  DA20   UP6    UP8    UP10   UP12   UP14   UP16   UP20   R526   R528   R5210  R5212  R5214  R5216  R5220  R606   R608   R6010  R6012  R6014  R6016  R6020  R756   R758   R7510  R7512  R7514  R7516  R7520  R906   R908   R9010  R9012  R9014  R9016  R9020  R1106  R1108  R11010 R11012 R11014 R11016 R11020 R1206  R1208  R12010 R12012 R12014 R12016 R12020 R1356  R1358 R13510 R13512 R13514 R13516 R13520  R1506  R1508 R15010 R15012 R15014 R15016 R15020    D6     D8     D10    D12    D14    D16    D20 OTNLOW  OTNMED  OTNHIG  ITNLOW  ITNMED  ITNHIG DH_TOD DH_TOT  PLT-I PLD-I TMF-OF  PLT2H PLD2H    OSN ReferenceNo    CDL    DSPS     WSS    MAIN   GENOA     SYM    ASYM
GER140772GER5549 GER 5549    MOANA                   MARTEN 49 Mod     MARTEN MARINE     REICHEL/PUGH      2004 KYC                                 DR. HANNO ZIEHM                                                                                             INTL     R  900 21 04 2016 16:11:00 15.044 13.944 3.727  4.15   9704. 145.5   0.00  494.8 1.2435  542.8  0.862  46.1   808.5  651.3  568.7  527.0  505.2  483.4  445.9  769.3  622.2  550.8  513.9  489.7  468.2  443.0  677.6  548.7  480.5  440.9  415.6  397.2  369.2  739.4  592.8  513.1  465.4  434.6  413.2  384.9  834.5  645.6  540.0  475.7  433.1  401.8  354.6  42.9  41.8  39.8  38.0  37.3  36.4  36.1 141.9 143.3 147.4 152.4 151.7 148.1 142.1  832.5  678.5  609.2  579.2  562.1  546.1  536.7  542.7  449.4  420.0  409.5  403.5  397.8  389.9  508.1  429.9  406.8  397.2  391.0  386.5  374.1  478.1  416.9  393.7  377.9  368.4  361.9  350.3  478.8  418.0  393.2  370.5  351.3  339.6  328.2  488.5  414.1  392.5  376.2  359.5  344.0  304.5  507.0  420.3  387.7  367.2  350.6  335.9  302.4  567.4  453.3  407.7  379.9  349.5  318.8  282.1  679.3  540.5  457.6  410.6  388.3  364.3  307.5  784.4  624.1  528.3  474.9  448.4  420.6  355.1 1.2035  1.5321  1.7318  0.9248  1.2366  1.4217  487.4 1.2310  1.199 284.8 1.2439  0.873  56.8  482.4 GER20014417 13.620   11215    42.5    87.8    59.4     0.0   240.7
```

### Overview of Codes

```
| Code | Explanation |
| ----------------- | --------------------------------------- |
| **General** ||
| NATCERTN.FILE_ID  | National Certificate Number |
| SAILNUMB          | Sailnumber                  |
| TYPE              | Class                       |
| BUILDER           | |
| DESIGNER          | |
| YEAR              | |
| CLUB              | |
| OWNER             | |
| ADRS1             | |
| ADRS2             | |
| C_Type            | Certificate Type (Club or International) |
| DD_MM_yyYY        | Date of Issue |
| HH:MM:SS          | Time of Issue |
| ReferenceNo       | ORC Reference Number  | 
|||
| **Hull** ||
| D                 | IMS Regulation Division (R = Performance, S, U, C = Racer, Sailboat, ? Cruiser) |
| LOA               | Length Overall (in m)
| IMSL              | Freeboard
| CDL               | Class Division Length
| DRAFT             | Draft (in m)
| BMAX              | Maximum Beam (in m)
| DSPL              | Displacement (in kg)
| DSPS              | Displacement in sailing trim (in kg)
| CREW              | Minimum Declared Crew Weight (in kg) |
| WSS               | Wetted surface (in m^2)
| INDEX             | Stability Index
| DA                | Dynamic Allowance (in %)
|||
| **Sails** ||
| MAIN   | Maximum main sail area (in m^2)
| GENOA  | Maximum fore sail area (in m^2)
| SYM    | Area of symmetrical Spinnaker (in m^2)
| ASYM   | Area of asymmetrical Spinnaker (in m^2)
|||
| **Scoring** ||
| GPH               | General Purpose Handicap (in s/nm Allowance)
| TMF-OF            | Offshore Time-On-Time Value (in s/s)
| OSN               | Offshore Time-On-Distance Value (in s/nm)
| TMF               | Inshore Time-On-Time Value (in s/s)
| ILCGA             | Inshore Time-On-Distance Value (in s/nm)
| PLT-O             | Offshore Performance Line (PLT)
| PLD-O             | Offshore Performance Line (PLD)
| PLT-I             | Inshore Performance Line (PLT)
| PLD-I             | Inshore Performance Line (PLD)
| OTNLOW/OTNMED/OTNHIG  | Triple Number Offshore Low/Medium/High
| ITNLOW/ITNMED/ITNHIG  | Triple Number Inshore Low/Medium/High
| DH_TOD    | Double-Hand Time-On-Distance
| DH_TOT    | Double-Hand Time-On-Time
| PLT2H | Performance Line, Time-On-Time, Double Handed
| PLD2H | Performance Line, Time-On-Distance, Double Handed
|||
| WL[6,8,10,12,14,16,20] | *Selected Courses:* **Windward/Leeward** Time Allowances for 6..20kt of Wind (in s/nm)
| OL[6,8,10,12,14,16,20] | *Selected Courses:* ??? Time Allowances for 6..20kt of Wind (in s/nm)
| CR[6,8,10,12,14,16,20] | *Selected Courses:* **Circular Random** Time Allowances for 6..20kt of Wind (in s/nm)
| NS[6,8,10,12,14,16,20] | *Selected Courses:* **Non Spinnaker** Time Allowances for 6..20kt of Wind (in s/nm)
| OC[6,8,10,12,14,16,20] | *Selected Courses:* **Ocean for PCS** Time Allowances for 6..20kt of Wind (in s/nm)
|||
| UA[6,8,10,12,14,16,20] | **Beat Angles** for 6..20kt of Wind
| DA[6,8,10,12,14,16,20] | **Gybe Angles** for 6..20kt of Wind (in s/nm)
|||
| UP[6,8,10,12,14,16,20] | **Upwind** Time Allowances for 6..20kt of Wind (in s/nm)
| R[52,60,75,90,110,120,135][6,8,10,12,14,16,20] | Time Allowances for 52..135Â° TWA with 6..20kt of Wind(in s/nm)  
| D[6,8,10,12,14,16,20] | **Downwind** Time Allowances for 6..20kt of Wind (in s/nm)
||| 
```

---

# ORC / IRC / Big Boat Support

**Scoring software:**
***

Altura:
***

Cyber Altura is free Windows software covering all options of ORC scoring. It does not have any limit in fleet size, number of events, number of races etc. 
All outputs can be configured, adding or removing fields that appear in the listings. Boat data can be imported through RMS files which are immediately updated by every new certificate issued by any Rating Office. A fully functional version is available for download, but expect occasional updates or newer improved versions.

Cyber Altura can be downloaded for free from its website, where there are also all explanations and instructions.

[Cyber Altura](http://www.cyberaltura.com/orc/inicio/inicio_en.php)

Velum:
***
Velum software is fully-compatible with ISAF and ORC scoring rules and can import data from RMS files. It is based on 30 years experience in 300 software licenses worldwide, countless regattas, numerous high-ranking championships, and is used in some of the world's largest regattas (Nordsee-Woche, Kieler-Woche, Travemünder-Woche, etc.). Velum offers huge flexibility in defining the scoring groups: a yacht can be scored in many different ways and in different scoring groups that may also include other subgroups. Velum is available on its website for a certain fee depending on using the full version or just an update.

[Velum](http://www.velumng.com/index.html)

**Scoring:**
***
**Time on distance:**
***
Corrected time is calculated as follows:

Corrected time = Elapsed time – (ToD * Distance)

With Time-on-Distance (ToD) scoring, the coefficient of time allowance of one boat will not change with wind velocity, but will change with the length of the course. One boat will always give to another the same handicap in sec/mi, and it is easy to calculate the difference in elapsed time between two boats needed to determine a winner in corrected time.

A special ToD coefficient calculated with an average crew weight of 170 kg is available for double handed racing as well as one calculated for non-spinnaker racing.

Where is it shown on the certificate?
  	 
  		
![certificate](http://www.orc.org/images/certificates/2013/time%20on%20distance.png)
		
  	
Time on Distance scoring coefficients on simple scoring options on
ORC International and ORC Club certificates
	


**How is it calculated?**
***
Offshore Time on Distance scoring coefficient also known as OSN (offshore single number) is calculated as a weighted average of the predicted boat speeds in following conditions:

The resulting time allowances at wind of 8 knots will be accounted with 25 %, the one at 12 knots with 50% and that at 16 knots with 25%.

The above scheme takes into account more windward/leeward directions in light winds, which is gradually reduced to have more reaching as the wind increases. 

Double handed coefficient is calculated using the same OSN method with crew weight of 170 kg, while non-spinnaker rating is calculated for boat's performance without spinnaker.

 
**Inshore Time on Distance** scoring coefficients is calculated as the average of windward/leeward course (50 - 50 %) time allowances in three conditions multiplied by their respective weights: 
  
       - 25 % of Windward/Leeward at 8 knots 
       - 40 % of Windward/Leeward at 12 knots 
       - 35 % of Windward/Leeward at 16 knots 

**Time on time:**
Corrected time is calculated as follows:

**Corrected time = ToT * Elapsed time**

With Time-On-Time (ToT) scoring, the time allowance will increase progressively as the wind velocity increases. Course distance has no effect on the results and need not be measured. Corrected time will depend only on the elapsed time, and the difference between boats may be seen in seconds depending of the duration of the races. The longer the race in time, the larger the handicap.

Where is it shown in the certificate ?

![Certificate](http://www.orc.org/images/certificates/2013/time%20on%20time.png)

 **How is it calculated?**
 
    
  Offshore Time on Time scoring coefficient is calculated as: 
  
          600 / Offshore ToD 
    
  Inshore Time on Time scoring coefficient is calculated as: 
  
          675 / Inshore ToD 

**Performance Line:**
**Corrected time is calculated as follows:**

**Corrected time = (PLT * Elapsed time) – (PLD * Distance)**

With the time coefficient PLT and distance coefficient PLD, two boats may be rated differently in light or heavy wind conditions, and it is possible that one boat is giving a handicap to another in light wind conditions, while the opposite may be true in heavy wind conditions.
 
**Where is it shown in the certificate?**

![Certificate](http://www.orc.org/images/certificates/2013/performance%20line.png)

**How is it calculated?**
Performance Line Scoring is a simplified variation of Performance Curve Scoring, where the curve of time allowances as a function of seven wind speeds is simplified by a straight line intercepting the performance points at 8 and 16 knots of wind for a given course. This is shown as follows:

![Certficate](http://www.orc.org/images/certificates/2013/perfline.JPG)

Offshore Performance Line coefficients are calculated using time allowances for the Ocean type of pre-selected course. 
  
Inshore Performance Line coefficients are calculated using time allowances for the Windward/leeward type of pre-selected course. 


**Performance Curve Scoring:**
***
**Performance Curve Scoring:**is the most powerful engine of the ORC International (ORCi) rating system. It is this unique feature which makes this rule fundamentally different from any other handicap system, as it recognizes that yachts of varied design perform differently when conditions change. 

This means that yachts of different designs will have different time allowances in each race depending on the weather conditions and the course configuration for that particular race. For example, heavy under-canvassed boats are slow in light airs but fast in strong winds, boats with deep keels go well to windward, and light boats with small keels will go fast downwind.

**Where is it shown in the certificate**?

![Certificate](http://www.orc.org/images/certificates/2013/Performance%20Curve%20Scoring.png)

An ORCi certificate provides a range of ratings (time allowances expressed in s/NM) for wind conditions in the range of 6 - 20 knots of true wind speed, and at angles varying from an optimum VMG beat to 52, 60, 75, 90, 110, 120, 135, 150 degrees of true wind angle, as well as the optimum VMG run angle.
 
  
Windward/Leeward (up and down) is a conventional course around windward and leeward marks where the race course consists of 50% upwind and 50% downwind legs. 
  
Circular Random is a hypothetical course type in which the boat circumnavigates a circular island with the true wind direction held constant.   
  
Ocean for PCS is a composite course, the content of which varies progressively with true wind velocity from 30% Windward/Leeward, 70% Circular Random at 6 knots to 100% Circular Random at 12 knots and 20% Circular Random, 80% reach at 20 knots.  
 

**How is it calculated**?

The use of PCS is not as complicated as it may appear. It requires the Race Committee provide only a little more data in addition to their usual work of setting up the course, following the wind changes, making starts and taking finishing times. There are different varieties of Scoring software that will do all calculations, which enables results to be ready as soon as the elapsed times of the race are entered.


**Step 1: Define the curse**
 Course may be selected from one of 4 Pre-defined types above or simply constructed with following parameters for each leg: 

- distance 
- course bearing 
- wind direction  

![Course](http://www.orc.org/images/certificates/2013/course.JPG)          

Typical course definition. Distance and bearings of each leg are entered, as is the approximate wind direction. Note wind speed is not entered. 
 
Current velocity and direction can also be entered for each leg, if it is known 

**Step 2: Prepare the scratch sheet**

For any of the selected courses described above, the true wind angle is calculated as being the difference between the wind direction and compass bearing of each leg. With this information, a table is made for each boat that describes the theoretical speed of the boat over that course for a variety of wind conditions. With this data a curve can be plotted which represents the predicted optimum performance along a scale of wind speeds. This curve is called the Performance Curve, and for each yacht this curve is different for any different course sailed.


**Step 3: Calculate implied Wind**

Performance curve for each boat:

![Curve](http://www.orc.org/images/certificates/2013/pcs1.JPG)


![Curve](http://www.orc.org/images/certificates/2013/pcs2.JPG)


In a typical Performance Curve plot, the vertical axis represents the speed achieved in the race, expressed in seconds per mile. The horizontal axis represents the wind speed in knots. When the finishing time of Yacht A is known, its elapsed time is divided by the distance of the course to determine the average speed in seconds per mile. This number is represented by point A on the vertical axis. The computer then finds the point on the horizontal axis that corresponds for that course to the average speed obtained.

This results in point Aw, the so-called "Implied Wind." This means that the yacht has completed the course "as if" it has encountered that wind speed. The faster the boat has sailed, the higher the Implied Wind, which is the primary index used for scoring: the yacht with the highest Implied Wind wins the race. The Implied Wind can then be transformed into a corrected time.

The Implied Wind is intended as an interpolation between time allowances, not an extrapolation. This means that when the Implied Wind drops below 6 knots or raises above 20 knots, the time allowances used for calculating the corrected times will be those of 6 knots and 20 knots respectively. This does not mean that ORC races need to be stopped (or not started) with wind below 6 knots or above 20. When the "implied wind" results calculate to be lass than 6 knots or more than 20, the corrected time values at these wind speeds are then used.

In order to present the result of the race in a comprehensive format we use a "Scratch Boat" (Figure 2). In most cases this is the potentially fastest boat of the fleet, shown in the example as yacht B. Being the fastest, her Performance Curve is the lowest in the figure. From the point where the vertical line yacht A intersects with the curve of the Scratch Boat, a horizontal line is drawn to the left towards the vertical axis. This point, Ac, produces the corrected time when the seconds per mile are multiplied by the distance of the course in miles. The corrected time of the Scratch Boat is, by definition, the same as its elapsed time. This exercise produces corrected times, expressed in hours, minutes and seconds, a familiar format for most sailors.

"Implied Wind" for the winning boat is thus normally in the range of the actual average wind strength for the race. However, in cases where the "Implied Wind" does not fairly represent the real wind strength during a race, the Fixed Wind method may be used to enter the performance curve with the predominant wind speed at the horizontal axis, thus obtaining the appropriate Time allowance at the vertical axis. Such a time allowance can then be used as a single number Time-on-Distance coefficient.











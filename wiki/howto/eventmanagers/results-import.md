# Importing official results

This page describes how to import official regatta results from different external regatta management systems.

## Sailwave / CSV

The CSV file must be uploaded using SCP to scores@sapsailing.com:sailwave/ and have a name that is globally unique, ideally containing the event, regatta, and class name, which in turn should contain at least the year to keep editions of the same event in different years apart. Once uploaded, the result will show when importing with the "Sailwave" importer.

Generally, the import happens from .csv files, and hence this importer can basically be used from any tool that can export a CSV with the columns needed:
```
Rank;Nat;SailNo;Fleet;HelmName;CrewName;Q1;Q2;Q3;Q4;Q5;Q6;F1;F2;F3;F4;F5;F6;F7;Total;Nett;
3rd;USA;19781;Red;Luke Arnone;;(22.0);16.0;4.0;13.0;1.0;1.0;2.0;1.0;12.0;7.0;(45.0);;;124.0;57.0;
27th;USA;19629;Blue;Thomas Hall;;8.0;17.0;16.0;13.0;(33.0);9.0;35.0;27.0;(40.0);26.0;3.0;;;227.0;154.0;
16th;USA;18162;Red;Emma Kaneti;;(13.0);2.0;1.0;13.0;6.0;1.0;43.0;22.0;19.0;6.0;(65.0);;;191.0;113.0;
12th;USA;15112;Red;Ricky Welch;;7.0;9.0;4.0;9.0;2.0;(78.0 BFD);12.0;17.0;18.0;27.0;(66.0);;;249.0;105.0;
```
Discards may be indicated by the points appearing in parentheses, but this is not necessary because the leaderboard will compute the discards by itself anyway. It may only be helpful to reduce the number of differences displayed during the import process.

Penalties / IRM codes, such as the "BFD" above, appear after the points in the same semicolon-separated field. The points and the IRM code can be separated by either a space or a comma (",").

You can obtain this format, e.g., by using SailWave. Sailwave is a offline Windows software which can export regatta results in different formats.

Although a XRR-Format-export does exist (which would be the best solution) we use the .csv export as the XRR-Export does not export race results (only regatta results).

How to use the .csv export in SailWave (For this description the version 2.16.7 of Sailwave has been used):
Before we can start the .csv export we must change some settings to export the data in the right format:

### 1. Select data columns to export

<img src="/wiki/images/sailwave/columns.jpg" width="100%" height="100%" alt="" />

### 2. Set points/discard format

Make sure to press Tools > Rescore so that the penalty fields are correctly filled in the format `p,c`. If you see just the penalty and not the points it will _not_ work. The CSV needs to containt points and penalty codes.

<img src="/wiki/images/sailwave/discard_format.jpg" width="100%" height="100%" alt="" />
 
### 3. Set ranking format

Make sure the highlighted checkbox is **UNCHECKED**

<img src="/wiki/images/sailwave/general_options.jpg" width="100%" height="100%" alt="" />

### 4. Set column separator and export the data...
In the file menu use the 'Export series summary to windows clipboard...' menu item.

<img src="/wiki/images/sailwave/clipboard_options.jpg" width="100%" height="100%" alt="" />

## Manage2Sail

tbd.

## WinRegatta

tbd.
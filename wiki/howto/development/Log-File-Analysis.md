# Log File Analysis

[[_TOC_]]

## Log File Types

There are three different types of log files produced by the server landscape under the umbrella of http://www.sapsailing.com:

 - Java server logs (sailing0.log.0, gc.log.0.current, ...)

 - Apache log files (access_log, error_log, ...)

 - Amazon EC2 Elastic Load Balancer (ELB) logs (017363970217_elasticloadbalancing_eu-west-1_ELBGermanLeague_20151118T1320Z_52.18.137.128_18dpodfi.log) or more recently Application Load Balancer (ALB) logs.

### Java Server Logs

The Java server logs contain health, memory and general progress data as well as information about exceptions that occurred, including deadlock situations and important application events such as adding a wind sensor account, starting to track a race, creating a new regatta, etc. The format of a typical Java server log line is

<pre>
Nov 03, 2015 6:32:24 PM com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl loadControlPointWithTwoMarks
INFO: Migrating right Mark of ControlPointWithTwoMarks Gate White P / White S from old field GATE_RIGHT to CONTROLPOINTWITHTWOMARKS_RIGHT
</pre>

A typical garbage collection log from gc.log.0.current may look like this:

<pre>
      ...
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Free CSet: 2.2 ms]
   [Eden: 5406.0M(5406.0M)->0.0B(5398.0M) Survivors: 26.0M->32.0M Heap: 8156.2M(9216.0M)->2757.5M(9216.0M)]
 [Times: user=0.28 sys=0.01, real=0.19 secs]
</pre>

The `Heap: ...` section gives hints as to the current heap memory consumption before and after the last GC run as well as the maximum heap allocated so far.

### Apache Log Files

Our primary, central Apache web server largely acts as a reverse proxy that maps subdomain names to virtual host declarations by means of a configuration file located at `/etc/httpd/conf.d/001-events.conf` which uses all sorts of clever macros to keep these mapping definitions concise. Due to the large number of these virtual hosts and in order to be able to keep the logs apart, we use the "combined" log file format which lists the virtual host name used as the first column in the log entry. This lets an `access_log` entry look like this:

<pre>
dutchleague2015.sapsailing.com 94.209.86.140 - - [08/Nov/2015:10:47:31 +0000] "POST /gwt/service/sailing HTTP/1.1" 200 751 "http://dutchleague2015.sapsailing.com/gwt/RaceBoard.html&regattaName=Dutch%20Sailing%20League%202015%20-%20Almere" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/6.1.6 Safari/537.78.2"
</pre>

After the virtual host name comes the client's IP address, followed by two unused fields regarding authentication, then the time stamp, the request, response code, number of bytes delivered, the referrer URL and the user agent.

For load-balanced set-ups we have an Amazon Elastic Load Balancer (ELB) dispatch incoming requests to the instances linked to the ELB. The ELB can (and definitely should) be configured to write its own logs (see next section), but those are not in Apache log file format and contain no information about the referrer URL. Using an Apache server on the instances linked to the ELB partly solves this problem by generating logs in the same format that our central Apache server uses.

In addition to this, the Apache servers on these load-balanced instances perform the same URL rewriting and reverse proxying using the same set of macros that the central Apache server uses.

**However**, one key problem of these decentralized Apache log files is that the client IP address only identifies the load balancer or reverse proxy instance, not the original client itself. With this, any type of analysis that tries to count the number of visitors will deliver skewed results. Counting the plain number of hits, though, is OK, as is the identification of the most-frequently used referrer URLs which can, e.g., help to identify popular leaderboards and races.

Still, in order to provide the true client IP in the log files, we use the `X-Forwarded-For` HTTP header field which contains the comma-separated list of "hops" through which the request was reverse-proxied so far. The following combination of Apache `httpd.conf` entries make this work:

<pre>
  SetEnvIf X-Forwarded-For "^([0-9]*\.[0-9]*\.[0-9]*\.[0-9]*).*$" original_client_ip=$1
  LogFormat "%v %h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"" combined
  LogFormat "%v %{original_client_ip}e %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"" first_forwarded_for_ip
  CustomLog logs/access_log combined env=!original_client_ip
  CustomLog logs/access_log first_forwarded_for_ip env=original_client_ip
</pre>

This uses the SetEnvIf module, tries to match an IP address at the start of the `X-Forwarded-For` header field, and if found, assigns it to the `original_client_ip` variable. This variable, in turn, decides which of the two `CustomLog` directives is applied to the request. If the variable is set, instead of using the regular `%h` field for the client IP, the variable value is logged by `%{original_client_ip}`.

The disposable reverse proxies have Apache logs too, which go to /var/log/old/REVERSE_PROXIES/"public ip" after log rotation.

### Amazon EC2 Elastic / Application Load Balancer (ELB / ALB) Logs

An Amazon ELB or ALB can be configured to write log files to the S3 storage. The general format is [explained here](http://docs.aws.amazon.com/ElasticLoadBalancing/latest/DeveloperGuide/access-log-collection.html#access-log-entry-format). It contains in particular the client IP where the request originated and can tell timing parameters for request forwarding and processing that otherwise would not be available.

The format for these logs is

<pre>
timestamp elb client:port backend:port request_processing_time backend_processing_time response_processing_time elb_status_code backend_status_code received_bytes sent_bytes "request" "user_agent" ssl_cipher ssl_protocol
</pre>

and it has changed over time, mostly being extended at the end, also for the ALB logs.

It seems a good idea to always capture these logs for archiving purposes. They end up on an S3 bucket, and by default we use `sapsailing-access-logs`.

Content can be synced from there to a local directory using the following command:

``s3cmd sync s3://sapsailing-access-logs/elb-access-logs ./elb-access-logs/``

Our central web server at `www.sapsailing.com` does this periodically every day, sync'ing the logs to `/var/log/old/elb-access-logs/`. The corresponding cron job was defined in `/etc/cron.daily/syncEC2ElbLogs` which was a script also found in git in the `configuration/` folder.

We have now started to analyze those ALB logs using Amazon Athena. See [https://eu-west-1.console.aws.amazon.com/athena/home?force&region=eu-west-1#query](https://eu-west-1.console.aws.amazon.com/athena/home?force&region=eu-west-1#query) for details. CloudWatch and Lambda are being used to automatically trigger some form of log analysis every month. See the CloudWatch event rule ``RunALBLogAnalysis`` and how it triggers the Lambda named [RunALBLogAnalysis](https://eu-west-1.console.aws.amazon.com/lambda/home?region=eu-west-1#/functions/RunALBLogAnalysis?tab=configuration) and the [Lambda function](https://eu-west-1.console.aws.amazon.com/lambda/home?region=eu-west-1#/functions/RunALBLogAnalysis?tab=configuration) containing the queries and configuration.

In order to cope with the data volume in our logs, a technique called "partition projection" is required. Another annoyance is that AWS extended the ALB log format somewhen in 2019, adding six more fields (redirect_url, lambda_error_reason, target_port_list, target_status_code_list, classification, classification_reason) to each log entry. Therefore, table definitions that span this format change need to handle the additional six fields as optional. For each bucket containing ALB logs (usually one per region) a database table must be created, like this:
<pre>
CREATE EXTERNAL TABLE IF NOT EXISTS alb_log_partition_projection_old_and_new (
type string,
time string,
elb string,
client_ip string,
client_port int,
target_ip string,
target_port int,
request_processing_time double,
target_processing_time double,
response_processing_time double,
elb_status_code string,
target_status_code string,
received_bytes bigint,
sent_bytes bigint,
request_verb string,
request_url string,
request_proto string,
user_agent string,
ssl_cipher string,
ssl_protocol string,
target_group_arn string,
trace_id string,
domain_name string,
chosen_cert_arn string,
matched_rule_priority string,
request_creation_time string,
actions_executed string,
redirect_url string,
lambda_error_reason string,
target_port_list string,
target_status_code_list string,
classification string,
classification_reason string
)
PARTITIONED BY(year int, month int, day int)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.RegexSerDe'
WITH SERDEPROPERTIES (
'serialization.format' = '1',
'input.regex' =
'([^ ]*) ([^ ]*) ([^ ]*) ([^ ]*):([0-9]*) ([^ ]*)[:-]([0-9]*) ([-.0-9]*) ([-.0-9]*) ([-.0-9]*) (|[-0-9]*) (-|[-0-9]*) ([-0-9]*) ([-0-9]*) \"([^ ]*) ([^ ]*) (- |[^ ]*)\" \"([^\"]*)\" ([A-Z0-9-]+) ([A-Za-z0-9.-]*) ([^ ]*) \"([^\"]*)\" \"([^\"]*)\" \"([^\"]*)\" ([-.0-9]*) ([^ ]*) \"([^\"]*)\"(.*)(.*)(.*)(.*)(.*)(.*)')
LOCATION 's3://sapsailing-access-logs/AWSLogs/017363970217/elasticloadbalancing/eu-west-1'
TBLPROPERTIES (
'has_encrypted_data'='false',
'projection.day.digits'='2',
'projection.day.range'='01,31',
'projection.day.type'='integer',
'projection.enabled'='true',
'projection.month.digits'='2',
'projection.month.range'='01,12',
'projection.month.type'='integer',
'projection.year.digits'='4',
'projection.year.range'='2017,2100',
'projection.year.type'='integer',
"storage.location.template" = "s3://sapsailing-access-logs/AWSLogs/017363970217/elasticloadbalancing/eu-west-1/${year}/${month}/${day}"
)
</pre>
Replace the bucket and region name in the ``s3://`` URL as well as the table name accordingly. Note how the optional trailing six fields are "matched" by the trailing combination ``(.*)(.*)(.*)(.*)(.*)(.*)`` in the regular expression. It will inaccurately and greedily match all trailing fields into the ``redirect_url`` field, including any quotes, and the five remaining fields can be expected to remain empty. (The challenge in modeling these optional components properly in the regular expression is that the match groups need to correspond to the table fields exactly; so nesting parenthesized match groups is not an option here. Also, all tables united with the ``union all`` construct need to expose exactly the same set of fields.)

The queries then have to unite the tables to consider in the query. Example:
<pre>
with union_table AS 
    (select *
    from alb_log_partition_projection_old_and_new
    union all
    select *
    from alb_log_tokyo2020_ap_northeast_1_partition_projection
    union all
    select *
    from alb_log_tokyo2020_ap_southeast_2_partition_projection
    union all
    select *
    from alb_log_tokyo2020_eu_west_3_partition_projection
    union all
    select *
    from alb_log_tokyo2020_us_east_1_partition_projection
    union all
    select *
    from alb_log_tokyo2020_us_west_1_partition_projection),
    daycounts as (SELECT domain_name, date_trunc('day', parse_datetime(time,'yyyy-MM-dd''T''HH:mm:ss.SSSSSS''Z')) as day, count(*) as requests_per_unique_visitor_per_day
                   FROM union_table
                   GROUP by domain_name, client_ip, user_agent, 2)
select domain_name, date_trunc('year', day) as year, count(requests_per_unique_visitor_per_day) as unique_visitors_per_year, avg(requests_per_unique_visitor_per_day) as average_requests_per_unique_visitor_per_day
from daycounts
group by domain_name, date_trunc('year', day)
ORDER by year DESC, unique_visitors_per_year DESC
</pre>
which will compute the unique visitors per year across six log buckets.

The results of the queries triggered by the cron job end up in [this S3 folder](https://s3.console.aws.amazon.com/s3/buckets/sapsailing-access-logs/query-results/?region=eu-west-1&tab=overview).

### Broken or Partly Missing Logs and How We Recover

During a few events we unfortunately failed to create proper Apache log files according to the above rules for scenarios using a load-balanced setup ("ELB scenario"). In some cases, the Apache log format was missing the referrer URL. This is easy to patch into the files because we collected the logs on a per-server basis and we knew which server ran which event. For example, for Kieler Woche 2015 and Travemünder Woche 2015 we needed to insert `kielerwoche2015.sapsailing.com` and `tw2015.sapsailing.com`, respectively, to match the general log format used for analysis.

The other problem, as described already above, is that of the original client IP address. Before we encountered how to log the original client IP from the `X-Forwarded-For` header field, only the ELB's IP addresses were logged in the Apache logs. While counting the correct number of hits is possible with such logs, they don't reveal the true number of unique visitors. They do, however, contain a valid referrer URL, telling us which leaderboards and races were the most popular.

In case we had an ELB log active, storing the Amazon ELB logs to an S3 bucket, we were able to convert those with a script stored in our git at `configuration/convertELBLogToApacheFormat` into our general Apache log format. This allows us to count the unique visitors for those events, but the ELB logs don't contain the referrer URL, so we patch that to always be the plain event URL, such as `http://kielerwoche2015.sapsailing.com`. Therefore, while these converted logs tell the correct number of unique visitors and the correct number of hits, they don't allow for an analysis of the most popular leaderboards and races.

We hope that these partially-logged events remain the exception and that future events are logged homogeneously according to the rules above. The following naming conventions have been established under `/var/log/old` that shall allow the automatic log analyzers to identify the log file contents by file name patterns:

 - `access_log*`: A proper Apache log file with virtual host name as the first field, followed by the original client's IP address and a valid referrer URL as the second-to-last field
 - `elb-origin-access_log*`: An Apache log file whose origin IP addresses are usually only the ELB IP addresses, therefore not allowing for unique visitor analysis
 - `bundesliga2015_elb_access_log.gz` and `tw2015_elb_access_log.gz`: ELB log files converted to Apache format, appended in order of ascending time stamps, prefixed with the virtual host name as the first field, using the original client IP address which allows for unique visitor analysis, and the virtual host name used again as generic referrer URL
 - `original-access_log*`: Apache log file without leading virtual host name field, also usually only with ELB IP addresses instead of an original client IP; not good for any log file analysis without further conversion, but kept for reference purposes

## Automatic Log File Rotation to /var/log/old

When an EC2 server is launched from its Amazon Machine Image (AMI), the /etc/init.d/sailing script is executed with the "start" parameter. This script is really just a link to `/home/sailing/code/configuration/sailing` which comes from the git version checked out to the workspace at `/home/sailing/code`. This script patches the file `/etc/logrotate.d/httpd` such that when log rotation happens, the existence of the directory `/var/log/old/$SERVER_NAME/$SERVER_IP` is ensured and the log files are copied there.

This way, logs end up in a per server-name and per server-ip directory.

The general log rotation rules (size, time, compression, etc.) is governed by `/etc/logrotate.conf`.

## Analysis Tools

### goaccess

The file `/root/.goaccessrc` on `www.sapsailing.com` looks as follows:

```
color_scheme 1
date_format %d/%b/%Y
time_format %H:%M:%S
log_format %^ %h %^[%d:%^] "%r" %s %b "%R" "%u"
log_file STDIN
```

Still, specific parameters are required for `goaccess` in order to parse our regular Apache logs that have the virtual host name as their first field:

`goaccess --no-global-config -a -f /var/log/httpd/access_log`

The unique visitors that goaccess lists are based on the common definition that counts requests as originating from the same visitor if they happened on the same day with equal user agent and IP address.

### apachetop

The simple command `apachetop` shows the traffic on the Apache httpd server running on the host where the command is invoked, based on the access log file in the usual location `/var/log/httpd/access_log`.

### AWStats

We use AWStats to analyze our Apache access log files and produce per-month reports at [http://awstats.sapsailing.com](http://awstats.sapsailing.com/awstats/awstats.pl?output=main&config=www.sapsailing.com&framename=index). The username is `awstats`. Ask axel.uhl@sap.com or petr.janacek@sap.com for the password.

AWStats report production is controlled by three things: a cron job hooked up by `/etc/cron.weekly/awstats` which is basically a one-liner launching the `awstats` command like this:

`exec /usr/share/awstats/tools/awstats_updateall.pl now         -configdir="/etc/awstats"         -awstatsprog="/usr/share/awstats/wwwroot/cgi-bin/awstats.pl"`

and a configuration file located at `/etc/awstats/awstats.www.sapsailing.com.conf` which describes in its `LogFile` directive the filename pattern to use for collecting the log files that shall be analyzed. Currently, the file name pattern is this:

`LogFile="/usr/share/awstats/tools/logresolvemerge.pl /var/log/logrotate-target/access_log-* /var/log/httpd/access_log /var/log/old/access_log-202????? /var/log/old/access_log-202?????.gz /var/log/old/*/*/access_log-202?????.gz /var/log/old/*/*/access_log-202????? | /bin/grep --text -v HealthChecker |"`

and all log entries that contain `HealthChecker` are eliminated as they are only a "ping" request sent by the load balancer to check the instance's health status which is not to be counted as a "hit." Note that according to the above explanations of our log file formats and variants this focuses on hit analysis for those events with broken or partial / incomplete log files such as Kieler Woche 2015 and Travemünder Woche 2015. It uses the `elb-origin-*` flavors, tolerating the fact that these don't have the original client's IP address. Furthermore, the pattern assumes that all log files older than 2020-01-01 have already been scanned and archived under `/var/lib/awstats`. Should this library ever need to be rebuilt from scratch, the pattern above would need to be adjusted to include older log files as well.

Due to the way log files are rotated and moved to the NFS share `/var/log/old`, particularly by the disposable reverse proxies which move them to `/var/log/old/REVERSE_PROXIES/{IP_ADDRESS}`, log file analysis by AWStats may lag one or two weeks behind. Log files are first moved to `/var/log/logrotate-target` by the logrotate service in one round of log rotation. Only during the next round will compression and the moving to `/var/log/old/...` take place. The `LogFile` pattern will include the `logrotate-target` contents on the central reverse proxy but not those from the disposable reverse proxies because their `logrotate-target` folder is not visible to AWStats prior to compression and moving to `/var/log/old/...`.

The third element of AWStats configuration is how it is published through Apache. This is described in `/etc/httpd/conf.d/awstats.conf` and uses the password definition file `/etc/httpd/conf/passwd.awstats` which can be updated using the `htpasswd` command.

### Custom Scripts

#### `unique_ips_per_referrer`

The script is located in git at `configuration/unique_ips_per_referrer`. It has two variants next to it: `unique_ips_per_referrer_generate_results_only` and `unique_ips_per_referrer_generate_month_results_only`. The basic idea of this family of scripts is to count the unique visitors for each virtual host name (usually corresponding to an event), in total and by month. Neither the `goaccess` nor the AWStats tool provide us with these numbers. AWStats can only compute the _hits_ per virtual host name, and `goaccess` only lists unique visitors per day, not per virtual host.

The script can be fed with a list of log files in our common Apache format, with virtual host name in first, original client IP in second, time stamp in third and user agent in last column. For all new files (files already analyzed are recorded in `/var/log/old/cache/unique-ips-per-referrer/visited) the script then reduces each line to its timestamp, original client IP and user agent fields, constituting the key for identifying a unique visitor. Each log entry that has been analyzed and reduced this way is then appended to a file under `/var/log/old/cache/unique-ips-per-referrer/stats/<virtual-host-name>.ips`. When done with all inputs, the `*.ips` files produced are filtered for unique entries (using `sort -u`) and number of lines are counted, resulting in the total number of unique visitors per virtual host name since the beginning of our log file records.

Additionally, a per-month analysis is carried out by splitting the `*.ips` files by the months found in the time stamps and again applying a unique count. The resulting files can be found at `/var/log/old/cache/unique-ips-per-referrer/stats/unique-ips-days-useragents-per-event` and `/var/log/old/cache/unique-ips-per-referrer/stats/unique-ips-days-useragents-per-event-MMM-YYYY` where `MMM` represents the three-letter month name such as `Apr` and `YYYY` stands for the year.

The `unique_ips_per_referrer` script is registered as a weekly cron job in `/etc/cron.weekly` and is fed the special converted log files of Travemünder Woche 2015 and the Bundesliga logs from 2015, as well as all files matching the file name pattern `access_log-*` anywhere under `/var/log/old`. This only matches proper Apache log files with original client IP addresses, not those with ELB IP addresses only (which are named `elb-origin-access_log*` by convention, see above).

The variant `unique_ips_per_referrer_generate_results_only` assumes that the `*.ips` files have already been updated and produces the total and per-month output files.

The variant `unique_ips_per_referrer_generate_month_results_only` produces only the per-month output files, also assuming that the `*.ips` files have already been updated.

The results are published under the link [http://awstats.sapsailing.com/unique-visitors/](http://awstats.sapsailing.com/unique-visitors/) which uses the same credentials as the main AWStats publishing page.

#### `convertELBLogToApacheFormat`

This script was already briefly mentioned above. It is found at `configuration/convertELBLogToApacheFormat` and converts and Amazon Elastic Load Balancer (ELB) log file to our common Apache log file format with leading virtual host name. The virtual host name is expected as the first parameter; all subsequent parameters are treated as file names of ELB log files, either in GZIP format with `.gz` extension or uncompressed. The conversion result is streamed to the standard output and may therefore be redirected into a file.
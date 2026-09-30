# Contributing to the Sailing Analytics

Thank you for your interest in the Sailing Analytics! We welcome contributions in all forms.

Here are some of the ways you can contribute to Sailing Analytics

Note: This is a living document. Like the Sailing Analytics, this document will be improved over time with the help of our community. Feedback welcome.

----

## Reporting Issues

Bug and issue reports just like feature requests are welcome! Report them [here](https://github.com/eclipse-sailing-analytics/sailing-analytics/issues).

### Determining if an Issue Should be Created

Analyzing reports is a bit of work, so please ask yourself these questions before creating an issue:

* Does the bug really apply to Sailing Analytics 

    There may be reasons the problem is occurring that have nothing to do with the Sailing Anlaytics code. Consider the following:

  * The problem may be caused by code that is not part of Sailing Analytics.
  * The problem may be caused by data being sent to Sailing Analytics through REST API calls.
  * The behavior may be different from what you expect, but may be working as designed. Or the feature may need improvement. In this case, it would be an enhancement request. Feature requests can also be submitted to [Github Issues](https://github.com/eclipse-sailing-analytics/sailing-analytics/issues).

* Is the problem reproducible in the latest release?
  
  * If you are not using the latest libraries or source code, please try to reproduce with the latest code first.
  * Make sure the problem is consistently reproducible using repeatable steps.
* Has this bug already been reported?
  * Please search the issue tracker for a similar bug before reporting the issue as a new bug.

If the bug does indeed apply to Sailing Analytics code, please create an issue.

### Providing the Right Information for the Issue

* Summarize the issue well. The following are some guidelines:

  * Precisely state what you expected as compared to the actual behavior you're seeing.
  * Include only those details that apply to the issue.
  * Be concise, but include details important for helping us in understanding the problem and finding the root cause.
  * State the version you are testing, which browser you are using, and on which device.
  
    If it is possible to indicate what you've seen in other browser and device combinations, that is even better! For example, does the issue occur in all browsers, or just one browser on one particular operating system? (For example, "only in Firefox on Windows 10.)
  * If possible, include the last version where the bug was not present.
  * If the bug is more visual, please attach a screenshot and mark up the problem.
  * Generally, provide as much detail as necessary, but balance our need for information with how obvious the problem is.

* Provide detailed, step-by-step instructions for reproducing the issue with an example, including, if possible:

  * A URL to your example
  * Screenshots if it helps us understand better.
  * Do not include more than one bug per issue created. This helps us to analyze bugs more easily.

Please report issues in English.

### About Reporting Security Issues

If you find a security issue, we'd like to encourage you to tell us directly instead of creating a public issue. This way we can fix it before it can be exploited.

* Researchers/non-Customers: please send the relevant information to support@sapsailing.com.

### How We Process Issues

New issues are reviewed regularly for validity and prioritization. Confirmed issues are given the "Approved" label, and the rest are either sent back to the reporter with a request for more details, or closed with an explanation.

Validated issues are then moved into one of these buckets:

* Priority issues will be assigned to one of our developers
* Issues that are less urgent will be left open as "contribution welcome"
* Certain issues may be moved to our internal issue tracking system if we don't think they belong to the Sailing Analytics.

Issues are closed when the fix is merged to develop. The release that contains the fix will be noted in the comments and the release changelog.

### How We Use Bugzilla Priorities

Our Bugzilla set-up supports five priorities (1-5):

* "1" is meant for show stoppers where, e.g., the website locks up, wrong data is being reported or the application crashes. Issues with thie priority need immediate action to unblock a live event using the solution.
* "2" is used for things that definitely need fixing but that are not necessarily holding up general operations at a live event.
* "3" identifies nice to have things we would like to see fixed at some point, but there is no specific urgency. Topics with this priority may be good candidates to start working on the project.
* "4" and "5" are what you use to make sure an idea is recorded and not forgotten, but other than when searching for a neat onboarding task for someone who would like to join the project, tasks with these priorities are hardly ever looked at.

### Issue Reporting Disclaimer

Feedback, especially bug reports, are always welcome. However, our capacity as a team is limited -- we cannot answer specific project or consultation requests, nor can we invest time in fleshing out what might be a bug. Because of this, we reserve the right to close or not process issue reports that do not contain enough information. We also do not guarantee that every well-documented issue will be fixed.

That being said, we will try our very best to ensure the Sailing Analytics codebase is of high quality.

----

## Contributing Code

We welcome contributions to the Sailing Analytics codebase. Before you start your first contribution, here are some things you should know:

1. You must be aware of the Apache License (which describes contributions), and you must agree to the [Contributors License Agreement](LICENSE.md). This is common practice for most open source projects.

    * For company contributors, special rules apply. See the respective section below for details.

1. Contributions must be compliant with the project code style, quality, and standards. We also follow them :-) 

    The `Contribution Content Guidelines` section below gives more details on the coding guidelines.

1. Not all contributions will be accepted.
    * The code you are submitting must fit the overall vision and direction of the Sailing Analytics and really improve it. Bug fixes are simple cases, for example, but new features may work better as third-party extensions.
    * Major feature implementations should be discussed on the issue tracker.

### Contributor License Agreement

When you contribute anything to the Sailing Analytics (code, documentation, analysis, anything), be aware that your contribution is covered by the same [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0) that is applied to the Sailing Analytics itself.

This applies to all contributors, including those contributing on behalf of a company.

### Developer Certificate of Origin (DCO)

Due to legal reasons, contributors will be asked to accept a DCO when they create the first pull request to this project. This happens in an automated fashion during the submission process. SAP uses [the standard DCO text of the Linux Foundation](https://developercertificate.org/).

#### Company Contributors

If employees of a company contribute code, in **addition** to the individual agreement above, there needs to be one company agreement submitted. This is mainly for the protection of the contributing employees.

A company representative authorized to do so must fill out the following [Corporate Contributor License Agreement](/docs/SAP%20Corporate%20Contributor%20License%20Agreement.pdf) form. The form contains a list of employees who are authorized to contribute on behalf of your company. When this list changes, please let us know.

Submit the form to us through one of the following methods:

* Email to [opensource@sap.com](mailto:opensource@sap.com) and [support@sapsailing.com](mailto:support@sapsailing.com)
* Fax to +49 6227 78-45813
* Mail to:

  Industry Standards & Open Source Team
  Dietmar-Hopp-Allee 16
  69190 Walldorf, Germany

### Contribution Content Guidelines

  A contribution will be considered for inclusion in the Sailing Analytics if it meets the following criteria:

* The contribution fits the overall vision and direction of the Sailing Analytics
* The contribution truly improves the solution
* The contribution follows the applicable guidelines and standards.

  The "guidelines and standards" requirement could fill entire books and still lack a 100% clear definition, but rest assured that you will receive feedback if something is not right. That being said, please consult the [Onboarding Document](https://wiki.sapsailing.com/wiki/howto/onboarding).

### Contribution Process

  1. Make sure the change would be welcome, as described above.

  1. Create a fork of the Sailing Analytics sources. 

  1. Work on the change in your fork (either on the `main` branch or on a feature branch, typically named after the Github issue number, such as ``bug1234``).

  1. Commit and push your changes.

  1. Give reasonable commit messages; a short first line, and if you'd like to write additional, more verbose comments, finish the first line with a semicolon, add an empty line, then add more comments.

  1. If your change fixes an issue reported in Bugzilla, add the following line to the commit message:

      ```bug{issueNumber}: ...```

  1. Create a pull request so that we can review your change.
  1. Wait for our code review and approval, possibly enhancing your change on request.

    Note: This may take time, depending on the required effort for reviewing, testing, and clarification. Sailing Analytics developers are also working their regular duties.

1. After the change has been approved, we will inform you in a comment.

1. Due to internal SAP processes, your pull request cannot be merged directly into the [downstream branch](https://github.com/SAP/sailing-analytics). It will be merged internally, and will also immediately appear in the public repository.
1. We will close the pull request. At that point, you can delete your branch.

We look forward to hearing from you!

# Typical Data Mining Scenarios

[[_TOC_]]

## Adding a new Data Mining Bundle

The following points describe the steps to create a new data mining bundle. An example for a data mining bundle is `com.sap.sailing.datamining`.

* Add a new java project bundle according to the [Typical Development Scenarios](/wiki/typical-development-scenarios#Adding-a-Java-Project-Bundle)
* Add the necessary dependencies to the `MANIFEST.MF`
	* `com.sap.sse` (for the string messages)
    * `com.sap.sse.datamining`
    * `com.sap.sse.datamining.shared`
    * `com.sap.sse.datamining.annotations`
* Create an `Activator` for the new data mining bundle and provide a `DataMiningBundleService` to the OSGi-Context (see [Registration and Deregistration of Data Mining Bundles](data-mining-architecture#Registration-and-Deregistration-of-Data-Mining-Bundles) for detailed information)
	* This can be done by extending the `AbstractDataMiningActivator` and implementing it's abstract methods
	* Or by implementing your own `DataMiningBundleService` and register it as OSGi-Service
* Prepare the string messages
	* Create the new source folder `resources`
	* Add the package `<string messages package name>` to `resources`
	* Create empty properties files for each supported language with the name scheme `<string messages base name>_<locale acronym>`
		* Which languages are supported can be found in `ResourceBundleStringMessages.Util.initializeSupportedLocales()`
	* Create a `ResourceBundleStringMessages` in the `DataMiningBundleService` of the new data mining bundle
		* The `ResourceBundleStringMessages` needs the resource base name, which should be `<string messages package name>/<string messages base name>`
		* A reference to this `ResourceBundleStringMessages` should be returned by the method `getStringMessages()`
* **[Optional]** It may be necessary to implement GWT specific components for the new data mining bundle, like DTOs or a [Serialization Dummy](typical-data-mining-scenarios#Include-a-Type-forcefully-in-the-GWT-Serialization-Policy).
	* This should be done in a separate bundle.
	* See [Typical Development Scenarios](/wiki/typical-development-scenarios#Adding-a-Java-Project-Bundle) for instructions, how to add a shared GWT bundle.

## Adding an absolute new Data Type

A data type is an atomic unit on which the processing components operate. A data type is normally based on a domain element (like `GPSFixMoving` or `TrackedLegOfCompetitor`) and provides contextual information about it (like the name of the regatta or race) via [Functions](data-mining-architecture#Data-Mining-Functions).

### Create the necessary Data Types

The following points describe the steps to create a data type, that is based on `TrackedLegOfCompetitor`. How this has been done in the data mining bundle `com.sap.sailing.datamining` can be seen in the packages `com.sap.sailing.datamining.data` and `com.sap.sailing.datamining.impl.data`.

* Create an interface for the domain element. The naming convention for data type interfaces is `Has<domain element name>Context`.
	* If `TrackedLegOfCompetitor` would be the domain element, then the name of the data type interface should be `HasTrackedLegOfCompetitorContext`.
* Create a data type interface for the steps from the data source to the domain element
	* In the case of the `TrackedLegOfCompetitor` the steps would be
		* `LeaderboardGroup`
		* `Leaderboard`
		* `TrackedRace`
		* `TrackedLeg`
		* `TrackedLegOfCompetitor`
	* This isn't mandatory for the framework to work, but it improves its performance, because it's possible to build a processor chain, that filters after each step.
* Add the data type interfaces to the `Classes` return by  the method `getClassesWithMarkedMethods()` of the `DataMiningBundleService` of the data mining bundle.
* Add methods to get the contextual information and annotate them, so that they get registered as functions.
	* To get the contextual data of a higher level data type interface annotate the getter for this interface with `@Connector(scanForStatistics=false)`.
		* If the data type `HasTrackedLegOfCompetitorContext` should have access to the dimensions of the previous data type (in this case `HasTrackedLegContext`), a method like `HasTrackedLegContext getTrackedLegContext()` is necessary.
	* There's currently no parameter support for functions, except for signatures exactly like `(Locale, ResourceBundleStringMessages)`.
	* Cycles in the graph of annotated methods aren't supported and would result in an infinite loop during the function registration.
	* See [Data Mining Functions](data-mining-architecture#Data-Mining-Functions) for detailed information about the annotations.
* Create implementations for the data type interfaces. The naming convention for data type classes is `<domain element name>WithContext`.
	* If `TrackedLegOfCompetitor` would be the domain element, then the name of the data type class should be `TrackedLegOfCompetitorWithContext`.

### Implement the Retrieval Processors and define the Retriever Chain

The following points describe the steps to create components for the data retrieval for the previously created data type. How this has been done in the data mining bundle `com.sap.sailing.datamining` can be seen in the package `com.sap.sailing.datamining.impl.components` and the class `SailingDataRetrievalChainDefinitions`.

* Add a `DataSourceProvider` (see [Data Mining Architecture](/wiki/data-mining-architecture#Data-Source-Provider)) to the `DataMiningBundleService` of the data mining bundle, if it doesn't contain one for the data source of the data type.
* Implement a [retrieval processor](/wiki/data-mining-architecture#Retrieval-Processor) for each data type.
	* Retrieval processors should extend `AbstractRetrievalProcessor`.
	* Retrieval processors used by retriever chains need a constructor with the signature `(ExecutorService, Collection<Processor<ResultType, ?>>, int)`.
	* A retrieval processor describes how to get from the input data type (like `RacingEventService` or `TrackedRace`) to the result data type (like `TrackedLeg`) with the method `retrieveData`.
* Create a `DataRetrieverChainDefinition` and add it to the `DataMiningBundleService` of the data mining bundle (see [Data Mining Architecture](/wiki/data-mining-architecture#Defining-and-building-a-Data-Retriever-Chain)).
	* A retriever chain is created with its methods `startWith`, `addAfter` and `endWith`, that add `Classes` of your retrieval processors to the chain.
	* Each method has a parameter for the `Class` of the retrieved data type of the next step and some also have a parameter for the `Class` of the retrieval processor of the previous step to ensure type safety.
	* Each method has a parameter for a message key, that is needed for the internationalization of the chain.
	* The call order of these methods has to match `startWith addAfter* endWith` or an exception will be thrown.
	* Every retrieval processor needs a constructor with the signature `(ExecutorService, Collection<Processor<ResultType, ?>>, int)` or `(ExecutorService, Collection<Processor<ResultType, ?>>, SettingsType, int)`. Otherwise an exception will be thrown.
		* For an example of a retrieval processor with settings see `PolarGPSFixRetrievalProcessor`.

## Adding a new Data Type with a reusable Retriever Chain

* Create the necessary Data Types according to [this](#Create-the-necessary-Data-Types).
* Implement a retrieval processor for each data type according to the first step of [this](#Implement-the-Retrieval-Processors-and-define-the-Retriever-Chain).
* Split the existing retriever chain and diverge it two retriever chains.
	* The split point should be the last retrieval processor, that is shared by the retriever chains
		* If there would be these two retriever chains
			  <table>
			    <tr><th>Existing</th><th>New</th></tr>
			    <tr><td>`RacingEventService`<br>
			          `LeaderboardGroup`<br>
			          `Leaderboard`<br>
			          `TrackedRace`<br>
			          `TrackedLeg`<br>
			          `TrackedLegOfCompetitor`
	              </td>
	              <td>`RacingEventService`<br>
			          `LeaderboardGroup`<br>
			          `Leaderboard`<br>
			          `TrackedRace`<br>
			          `MarkPassing`
	              </td>
	            </tr>
			  </table>
		  then the existing retriever chain should be splitted after the retrieval processor for the `TrackedRaces`.
	* To split an existing retriever chain, create a new one after the split point with the usage of the existing one. Then move the `addAfter` and `endWith` calls to the new retriever chain. The first part of the split retriever chain can now be used to create the retriever chain for the new data type.
		* Note, that it's not allowed to call `startWith`, if a retriever chain is used to create a new one. 
	* In pseudo code would this look like this:<br>
	  Before:<br>
	    <pre>
RetrieverChain trackedLegRetrieverChain = new RetrieverChain(RacingEventService.class, TrackedLegOfCompetitor.class);
trackedLegRetrieverChain.startWith(LeaderboardGroupRetrievalProcessor.class);
trackedLegRetrieverChain.addAfter(LeaderboardRetrievalProcessor.class);
trackedLegRetrieverChain.addAfter(TrackedRaceRetrievalProcessor.class);
trackedLegRetrieverChain.addAfter(TrackedLegRetrievalProcessor.class);
trackedLegRetrieverChain.endWith(TrackedLegOfCompetitorRetrievalProcessor.class);
</pre>
	  After:
		<pre>
RetrieverChain trackedRaceRetrieverChain = new RetrieverChain(RacingEventService.class, TrackedRace.class);
trackedLegRetrieverChain.startWith(LeaderboardGroupRetrievalProcessor.class);
trackedLegRetrieverChain.addAfter(LeaderboardRetrievalProcessor.class);
trackedLegRetrieverChain.endWith(TrackedRaceRetrievalProcessor.class);
</ br>
RetrieverChain trackedLegRetrieverChain = new RetrieverChain(trackedRaceRetrieverChain, TrackedLegOfCompetitor.class);
trackedLegRetrieverChain.addAfter(TrackedLegRetrievalProcessor.class);
trackedLegRetrieverChain.endWith(TrackedLegOfCompetitorRetrievalProcessor.class);
</ br>
RetrieverChain markPassingRetrieverChain = new RetrieverChain(trackedRaceRetrieverChain, MarkPassing.class);
markPassingRetrieverChain.endWith(MarkPassingRetrievalProcessor.class);
</pre>
* Ensure, that all retriever chains are added to the `DataMiningBundleService`

## Include a Type forcefully in the GWT Serialization Policy

It can happen, that a necessary type isn't included in the GWT Serialization Policy, because of the generality of the framework. To enforce the include of a type, the GWT-Service has to use the type in one of its methods. To do this, follow these steps:

* Create a dummy class in a shared bundle, if such a dummy class doesn't exist already in the scope of the type.
	* For example like `SSEDataMiningSerializationDummy`.
	* The class has to implement the interface `SerializationDummy` and should only have a private standard constructor, to prevent an accidental instantiation.
	* Implementing this interface adds the dummy automatically to the serialization policy, without changing the `DataMiningService`.
* Add a private non-final instance variable of the type to the dummy.

The type will be included in the GWT Serialization Policy, because the `DataMiningService` has a method, that uses the interface `SerializationDummy`, that uses the type.

If you can't implement `SerializationDummy` for any reasons do the following:

* Implement the interface `Serializable` or `IsSerializable` instead.
* Add a pseudo method to the GWT-Service, that uses the dummy class.
	* For example like `SerializationDummy pseudoMethodSoThatSomeClassesAreAddedToTheGWTSerializationPolicy()`.
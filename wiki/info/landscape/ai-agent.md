# AI Agent

With the advent of powerful large language models (LLMs), we can now use this technology to produce meaningful comments based on things that happen in races and regattas. We base this feature set on [SAP AI Core](https://help.sap.com/docs/sap-ai-core/sap-ai-core-service-guide/what-is-sap-ai-core) which helps with credentials checking, session management, metering, model selection, and the credits and payment infrastructure behind it. This said, valid AI Core credentials are required for a deployment of the Sailing Analytics to enable this feature set.

## Relevant Bundles

A total of five OSGi runtime bundles (plus a few test fragments) have been added to support this feature set.

### com.sap.sse.aicore

This bundle contains a basic facade for SAP AI Core. It manages credentials and can use them to authorize HTTP requests made against SAP AI Core, can list the models deployed, make model selection, and create chat sessions that can be used with the typical ``chat-completion`` openai REST API. The chat sessions can be parameterized with the typical settings, such as ``temperature`` or ``top_p`` values. Default instances will be created with credentials provided in the ``sap.aicore.credentials`` system property, or otherwise with initially empty credentials.

### com.sap.sailing.aiagent.interfaces

An OSGi "interface" bundle that contains no implementation, only the ``AIAgent`` interface and a corresponding listener interface. This way, other bundles can discover an ``AIAgent`` implementation through the OSGi registry using a service tracker and will survive a refresh of the implementation bundle ``com.sap.sailing.aiagent`` (see below).

### com.sap.sailing.aiagent

The implementation bundle for the ``com.sap.sailing.aiagent.interfaces`` bundle. Its activator constructs a default ``AIAgent`` using a default ``AICore`` instance and registers it with the OSGi registry under the ``AIAgent`` interface.

### com.sap.sailing.aiagent.gateway

Provides a REST API for the AI Agent. This is made available under the ``/aiagent/api/v1/aiagent`` base URL and currently supports methods for starting and stopping the AI commenting for sailing events.

### com.sap.sailing.aiagent.persistence

This bundle implements MongoDB-based persistence for the AI Agent's configuration data, in particular the events currently in commenting mode, and non-standard credentials as provided through the Admin Console. The bundle is configured to start automatically, discovers the ``AIAgent`` service from the OSGi registry and, if found, configures it based on the data read from the database. Additionally, this bundle registeres as a listener on the ``AIAgent`` so that any relevant changes will lead to the corresponding database updates.

## Configuration

The AI Agent feature set is configured by a combination of system properties, through the [Admin Console](/gwt/AdminConsole.html#AIAgentConfigurationPlace:), the REST API, and upon start-up from the database. There are two essential system properties used for the configuration right now:

- ``sap.aicore.credentials``: if provided, its value is expected to be a JSON document with AI Core credentials, as you would get from viewing your credentials of you AI Core instance in SAP BTP Cockpit; if not provided, credentials may still be provided through the Admin Console later.
- ``sap.sailing.aiagent.modelname``: if provided, selected a language model name to use; a model deployment by that name must be available in the AI Core account for which the credentials are valid. If not provided, or if no model deployment by that name can be found, a default model is used (currently gpt-4o-mini). Note that the model you choose this way needs to comply with the _openai_ API for ``chat-completions`` in order to be used in this context.

Upon start-up, the persistence bundle looks for credentials stored persistently which, if found, will override any credentials that may have been provided through the ``sap.aicore.credentials`` system property; furthermore, the set of events for which AI commenting is to be enabled.

## Start-Up Sequence

In ``raceanalysis.product`` we have:
```
<plugin id="com.sap.sailing.aiagent" autoStart="true" startLevel="4" />
<plugin id="com.sap.sailing.aiagent.persistence" autoStart="true" startLevel="5" />
<plugin id="com.sap.sailing.aiagent.gateway" autoStart="true" startLevel="6" />
```

With this, ``com.sap.sailing.aiagent`` starts first, obtains a default ``AICore`` instance which will try to initialize credentials from the ``sap.aicore.credentials`` system property. With this ``AICore`` instance (regardless of whether valid credentials were found through the system property) an ``AIAgent`` implementation instance will be created, using the value of the ``sap.sailing.aiagent.modelname`` system property for the desired model name. If that property isn't set, or if the model specified by that property cannot be resolved using the default credentials from the ``sap.aicore.credentials`` property of the ``AICore`` instance, a default model will be used (currently ``gpt-4o-mini``).

This ``AIAgent`` instance will then be registered with the OSGi registry by the activator of the ``com.sap.sailing.aiagent`` bundle.

The ``com.sap.sailing.aiagent.persistence`` bundle will start up after the ``com.sap.sailing.aiagent`` bundle. It will discover the ``AIAgent`` instance in the OSGi registry where the ``com.sap.sailing.aiagent`` bundle's activator has registered it. The persistence bundle looks for configuration data in MongoDB. It will tell the ``AIAgent`` to start commenting on those (and only those) events whose IDs have been found in the database. Furthermore, if credentials are found in the database, they will be set on the ``AIAgent`` (and transitively on the ``AICore`` facade), which will lead to a new discovery of the language model by the desired or default name (see above) and subsequently the creation of a new chat session with this model deployment.

From there on, the persistence bundle will act as a listener on the ``AIAgent`` service, getting notified when the set of events to comment on changes and updating the database contents accordingly. Likewise, if the credentials are updated, e.g., by the user through the Admin Console, the new credentials are stored to the database where they will serve for ``AICore`` initialization the next time the ``com.sap.sailing.aiagent`` bundle will be started.
# Security

The Sports Sponsorships Engine (SSE) on which the SAP Sailing Analytics and the SAP Tennis Analytics are based, uses Apache Shiro to implement security. This in particular includes authentication and authorization. This document does not aim to replace the Shiro documentation which is available, e.g., at [http://shiro.apache.org/configuration.html](http://shiro.apache.org/configuration.html), [http://shiro.apache.org/permissions.html](http://shiro.apache.org/permissions.html) and [http://shiro.apache.org/web.html](http://shiro.apache.org/web.html). It relies on a sophisticated and powerful security engine that is part of SSE and whose [permission concept is described here](permission-concept).

## Users, Sessions, Roles, and Permissions

Users are identified in an authentication process. This can currently be a username/password login implemented by posting a form, through HTTP basic authentication (an "Authorization: Basic <some-base64-string>" HTTP header field) or by using an OAuth-like bearer access token that can be obtained by an authenticated user through a RESTful web service. OAuth-based authentication by external OAuth providers and SAP ID is planned (see [Bug 2482](http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=2482)).

The response for an HTTP request by an authenticated user contains the JSESSIONID cookie whose value is the session key. Our sessions have a default timeout of 30 minutes which is provided as the Shiro default (see `org.apache.shiro.session.mgt.DefaultSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT` and its use in the `org.apache.shiro.session.mgt.SimpleSession` constructor). Each request performed with this session key will renew the session timeout.

Logging out, e.g., by invoking the `/security/api/restsecurity/logout` service, invalidates the session key, and a new authentication will be required to obtain a new session.

An authenticated user has zero or more roles and zero or more immediate permissions assigned. Roles, in turn, can imply more permissions which are added to the immediate permissions to result in the complete set of permissions the user has. Roles and immediate permissions are stored persistently in an instance of `com.sap.sse.security.UserStore` and can be dynamically adjusted during application run-time.

The inference from roles to the permissions implied by those roles happens by implementations of the `com.sap.sse.security.shared.PermissionsForRoleProvider` interface. Authentication is mainly performed by so-called "Realm" implementations. SSE provides an abstract base class `com.sap.sse.security.AbstractUserStoreBasedRealm`. It supports setting a `PermissionsForRoleProvider`. Additionally, a `PermissionsForRoleProvider` can be used with `com.sap.sse.security.ui.shared.UserDTO.getAllPermissions(...)` so that UI clients can have the same set of permissions inferred for a user that the Shiro back-end will use for permission checks, so as to enable an disable UI features based on the user's permissions.

## How to Configure

Shiro security is largely configured by `shiro.ini` files in OSGi Web Bundlesand their `WEB-INF/web.xml` descriptors. Shiro web security hinges on the use of servlet filters that are configured in `web.xml`. The corresponding section to enable Shiro security for a Web Bundle looks like this:

	<context-param>
		<param-name>shiroEnvironmentClass</param-name>
		<param-value>org.apache.shiro.web.env.IniWebEnvironment</param-value>
	</context-param>
	<listener>
		<listener-class>org.apache.shiro.web.env.EnvironmentLoaderListener</listener-class>
	</listener>
	<filter>
		<filter-name>ShiroFilter</filter-name>
		<filter-class>com.sap.sse.security.shiro.ShiroFilterForAllButOptionsRequests</filter-class>
	</filter>
	<!-- Make sure any request you want accessible to Shiro is filtered. "/*" 
		catches all requests. Usually this filter mapping is defined first (before all 
		others) to ensure that Shiro works in subsequent filters in the filter chain: -->
	<filter-mapping>
		<filter-name>ShiroFilter</filter-name>
		<url-pattern>/*</url-pattern>
		<dispatcher>REQUEST</dispatcher>
		<dispatcher>FORWARD</dispatcher>
		<dispatcher>INCLUDE</dispatcher>
		<dispatcher>ERROR</dispatcher>
	</filter-mapping>

For this to work, the Web Bundle requires at least the two bundles `org.apache.shiro.core` and `org.apache.shiro.web` which are provided by the target platform. Furthermore, the bundle should require `com.sap.sse.security` so as to get support for the common user store, session replication support and the common roles and permissions management.

The Web Bundle then provides a `shiro.ini` file in its classpath root, e.g., directly within its `src` or `resources` source folder. The `shiro.ini` file contains essential configuration information about which realms, which session and which cache manager to use. It also configures URLs for login pages, default success pages and permissions required for access to URLs. The file `com.sap.sse.security/resources/shiro.ini` serves as a reasonable copy template. In the `[urls]` section the `shiro.ini` flie provides so-called filter chains for specific or pattern-based sets of URLs. In particular, the configuration can require the authenticated user to have specific roles and / or specific permissions to access the URL. Note the use of the `AnyOfRolesFilter` and how it is different from the regular `roles` filter.

All authentication filters inheriting from `com.sap.sse.security.AbstractUserStoreBasedRealm` can provide an instance for the `permissionsForRoleProvider` property in the `shiro.ini` configuration file, e.g., as follows:

    permissionsForRoleProvider = com.sap.sailing.gwt.ui.client.shared.security.SailingPermissionsForRoleProvider
    upRealm = com.sap.sse.security.UsernamePasswordRealm
    upRealm.credentialsMatcher = $credentialsMatcher
    upRealm.permissionsForRoleProvider = $permissionsForRoleProvider

## How to Implement Permission Checks

There are generally two ways in which some feature can require the user to be equipped with permissions: declaratively in the `shiro.ini` file's `[urls]` section; or programmatically by using something like ``org.apache.shiro.SecurityUtils.getSubject().checkPermission(...)`` which will throw an `AuthorizationException` in case the user lacks the necessary permissions.

Example for a declarative permission check:
    [urls]
    /api/v1/events = bearerToken, perms["EVENT:READ"]
This requires users trying to access the URL `/api/v1/events` to be authenticated using a valid `JSESSIONID` cookie or any authentication supported by the `bearerToken` filter such that the authenticated user has permissions that imply the `event:view:*` permission.

Examples for programmatic checks if the domain object, here ``event`` is at hand:

```
    SecurityUtils.getSubject().isPermitted(SecuredDomainType.EVENT.getStringPermissionForObject(DefaultActions.READ, event))
```

or

```
    SecurityUtils.getSubject().isPermitted(SecuredDomainType.EVENT.getStringPermissionForTypeRelativeIdentifier(
                DefaultActions.UPDATE, EventBaseImpl.getTypeRelativeObjectIdentifier(eventId)))
```

if only an ID such as ``eventId`` is available at the point of the permission check.

### Special Case: Permission Checks in the AdminConsole

The `AdminConsolePanel` provides some generic support for permission handling, aiming at customizing the UI to the user's actual permissions. Ideally, a user would only see UI features he/she has the permission to use. In particular, we don't want to bother the user by showing panels he/she isn't permitted to use at all.

When an administration entry point uses the `AdminConsolePanel`, each widget shown in the panel can be configured to require any of a set of permissions. For example, a panel for managing users would ask for permission `manage_users`. This is actually short for `manage_users:*:*`, a wildcard permission that implies all more specific permissions such as `manage_users:view:*` or `manage_users:edit:peter`. However, a specific permissions such as `manage_users:edit:peter` doesn't imply `manage_users:*:*` by the rules of wildcard permissions.

The widget shall be shown as soon as the user has any permission for managing users, even only a specific one. It is then up to the implementation of the widget to further constrain user interaction and information displayed, based on the user's actual permissions. To implement this, the permission check for the appearance of administration console widgets is slightly modified: a widget will appear if the permission it requires implies any of the permissions the user has, or if any of the user's permissions implies the permission required by the widget. This way, a user having a specific permission such as `manage_users:edit:peter` will see a widget that requires `manage_users`. Also, a user having the administrator permission `*` will see the same widget because `*` implies all other permissions, particularly `manage_users`.

## Standard REST Security Services

There are a number of RESTlets registered under the `/security` context root that allow RESTful clients to log in and log out a user, as well as obtain a bearer access token for a user which can then be used in conjunction with the `bearerToken` / `BearerTokenOrBasicOrFormAuthenticationFilter` authentication filter. These services are described in more detail at [/security/webservices/api/index.html](http://sapsailing.com/security/webservices/api/index.html).

## Notes on Replication

The `SecurityService` implementation is a `Replicable` that is replicated from a master to its replicas and in case of replica-initiated operations also the other way. Also, the `SecurityService` is registered with the OSGi service registry and can be discovered by other components. It has a `UserStore` and a cache manager (`com.sap.sse.security.SessionCacheManager`) that is replication aware. This cache manager has to be configured in the `shiro.ini` file.

Whenever a cache entry is updated (particularly the details of a user session such as creating a new session, expiring a session or touching a session for timeout refresh), the effect is replicated. For performance reasons, a special rule is in place for the `touch` operation (see `SecurityWebSessionManager.touch(...)`). Instead of replicating this effect immediately, a timer is launched which considers the session timeout and the assumed time it takes to replicate an operation and collects and delays such touch operations and sends them at the latest possible time point to ensure a session that got touched doesn't expire anywhere.

All operations affecting the `UserStore` are also replicated by the `SecurityService`, in particular the creation, update and removal of users, their roles and their permissions.

It is planned to enable replicating the `SecurityService` from a different master server than the one used for replicating the application domain data. See also [Bug 2465](http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=2465). This would allow an administrator to set up a separate user management server that works as a central "directory" for several other archive and event servers, sharing user management data across such a landscape.
[[_TOC_]]

## Introduction

This document describes the permission concept developed for the SAP Sailing Analytics (in the following just Sailing Analytics). The following requirements were considered during its conception:

* Be expressive enough to support the complex associations of the Sailing Analytics
* Support multiple organizations (clubs, events and individuals) working in one system
* Communicate the permissions to the frontend (so only UI elements that support permitted actions are active)
* Be not overly complex and implementation intensive

## Users and User Groups

A client can try to access the solution anonymously, without authentication, or can sign up for a user account and authenticate accordingly before making requests. A user has to provide a nickname that is unique and can optionally provide and validate an e-mail address as well as further attributes such as the full name, a company affiliation and various other settings.

User groups can be created that users can be assigned to as members. A user can be member of any number of user groups. By default, when a new user account is created, a new user group is created, too, named after the user with the suffix ``-tenant`` appended. For example, for the user with nickname ``john`` a user group named ``john-tenant`` will be created. The user is made a member of that default user group automatically.

Each server or server cluster (master and its replicas) has a unique name and a dedicated server user group. This group is named after the server: ``{servername}-server``. For example, the development server named ``DEV`` has a server group called ``DEV-server``.

A special user named ``<all>`` represents any user accessing the system, including non-authenticated, non-signed-up users.
All permissions granted to this user are implicitly also granted to all other users, and all permissions revoked for this
user are revoked for all other users.

Likewise, an unnamed "null" user group exists that all users, including those not logged in or not signed up,
are implicitly members of. This unnamed group can be used for access control list (ACL) assignments which will be explained
below, causing the permissions granted/revoked by the ACL to apply to all users.

## Ownership

Objects can have a user as an owner, and a user group as an owning group. The approach mimics that of a Unix/Linux file system where each file system object has a "group owner" and a "user owner." As will be explained later, certain permissions may then implicitly be granted if the requesting user belongs to the group owning the object or is identical to the user owning the object.

The user groups may, e.g., represent the organizations working in the system. For example, in the Sailing Analytics the organizations could be SAP in general (e.g., owning all content in the "archive server"), sailing clubs, or events like the Travemünder Woche. The group ownership can then be used to define access rules based on this group ownership, either generally for all users, or specifically for users that are members of that group. An example of the latter would be a training group that wants to track their training sessions and make the tracks accessible only to members of that group. An example of the former would be an event with a dedicated event user group, making all event data public by providing permission to all users to read all content owned by the event group.

Ownerships are usually assigned to an object upon its creation. For this, the session user will be taken as the default owning user. The owning group is determined based on the user's preferences. Logged-in users can select the user group to use as the group owner for new objects the user created. The user must be a member of that group at the time the selection is made. This assignment is stored on a per-server basis. When a dedicated server is set up for an event it is good practice to create the objects with a specific event group as group owner on that server. Still, on other servers the user may want to create objects with other groups as owners which is why this setting is remembered per server.

It is generally possible to change the group and user ownership of an object. This constitutes an action for which the user needs to have the corresponding permission.

## Access Control Concepts

The two big concepts that play together in this permission concept are access control lists (ACLs) (also used e.g. in the Linux or Windows file system) and role-based access control (RBAC).

The concept of ACLs is based on the idea of assigning each data object that is access controlled an ACL. The ACL used here is a list of entries that grant or revoke permissions to a user group. If e.g. read access is requested for a data object, its ACL is checked if any of the groups the user is a member of has an entry granting the read permission and no entry denying that read permission.

RBAC is based on the idea of having roles that imply certain permissions and assigning roles to users. When an action is to be executed, such as reading an object, the read permission is checked, and if it is not granted or revoked by an ACL entry, the requesting user's roles are checked regarding implying this read permission. If so, the permission is granted. It is important to note that always single permissions are checked and not whether or not the user has a certain role assigned.

## Permissions

A permission represents the right to perform a certain *action* on an object that has a certain *ID* and a *type*. Permissions are textually represented as such a triple: ``TYPE:ACTION:OBJECT-ID``. For example, the permission to read the data of an object of type *Event* and having ID *587e5fef-53ea-47f0-a71b-1fc29053b4f0* can be represented as ``EVENT:READ:587e5fef-53ea-47f0-a71b-1fc29053b4f0``. Default action types are:

* CREATE
* READ
* UPDATE
* DELETE
* CHANGE_OWNERSHIP
* CHANGE_ACL

The omission of a trailing part, such as the object ID, implies the permission for *all* values of that part. For example, ``LEADERBOARD:READ`` implies the right to read any leaderboard's information.

Each of the parts of a permission (type, action, ID) can optionally contain more than one element; the elements then have to be separated by commas. Example: ``EVENT,LEADERBOARD:READ`` describes the permission to read any event and any leaderboard object.

Parts can use the asterisk character ``\*`` as a wildcard, meaning all possible values for that part. Example: ``\*:READ`` describes the permission to read regardless the object type.

When *creating* objects, before checking the ``CREATE`` action for the type of object to be created, the ownership that will result from successful object creation is already assumed for the permission check. This way, users having the ``CREATE`` permission for a type of object owned by themselves or their default creation group, they may be eligible to create such objects. A second permission is always checked before granting the right for object creation: ``SERVER:CREATE_OBJECT:{servername}``. This means that the user requesting the object creation permission must not only be allowed to create the object of the requested type, but also the user must be allowed to create objects on this particular server through which the request is accepted. This way, servers and their data may be protected from unwanted object creations even if users generally have the permission to create objects they own. 

## Roles

Roles have a *definition* and *assignments*. The role's definition is an entity that has a UUID, defines a (changeable) name and specifies the (changeable) set of permissions a user will obtain by being assigned a role instantiated from this definition. A role assignment references a role definition and "inherits" the definition's name. The assignment can furthermore optionally constrain the role by a user group and/or user qualifier. In this case the permissions the role definition specifies will be granted to a user in that role if and only if requested for an object whose group/user owner matches that of the group/user qualifier provided by the role assignment, respectively.

Roles with a group ownership qualifier are displayed in the form "\<rolename\>:\<groupname\>". Roles with group and user qualifier are displayed as "\<rolename\>:\<groupname\>:\<username\>", and role assignments with only a user qualifier are shown as "\<rolename\>::\<username\>". Examples:

1. Group Owner "owner:tw2018" (Can delete the group, in addition to everything the group admin can do)
2. Group Admin "admin:kw2018" (Has (almost) every permission in his/her group)
3. Eventmanager "eventmanager:VSaW-server"
4. Racemanager "racemanager:KYC-server"
5. Editor "editor:BYC-server"
6. Resultservice "resultservice:swc2018-miami"
7. User "user::johndoe" (A role that every user should have for himself/herself; grants permissions to modify the respective user object properties such as company affiliation, password and full name)

With this it is possible, for example, to have an ``admin`` role definition with permission "\*". An instantiation of this role can then optionally restrict the user group that must be an object's group owner for the role's permissions to be applied to permission checks for that object. If a user has role ``admin:A-server`` and requests permission for an object, the "\*" permission from the ``admin`` role definition is granted if and only if the object's group owner is ``A-server``.

Similarly, if a role instantiation provides a user qualifier, the object for which a permission is checked must be owned by the user specified by the qualifier in order for the role's permissions to be granted to a user with this role.

If both, a user and group qualification are declared for a role assignment then both have to match in order for the role's permissions to be implied.

## User Groups and Roles

A user group can specify roles whose permissions to grant to clients trying to perform an action on an object whose group owner is this user group. The group can specify for each such role whether its permissions shall be granted to *all* users making a request to an object owned by this user group, or only to those users who are a member of this user group.

This concept can be used, e.g., to make the objects of an event publicly readable by everyone: the event and all its contained objects can be given to a dedicated user group, and that group can then specify the role *sailing_viewer* to be granted to *all* users trying to access objects owned by this dedicated group.

Another example: a group of sailors is sailing a training session together. They would like their tracks to be visible exactly to the members of their user group. So they create a user group for their training group, assign all their users to that group, create the event and all sub-ordinate objects with their training user group as the group owner and add the *sailing_viewer* role to that group, restricting its application to the members of their user group.

## Defaults upon First Server Startup

On fresh instances that do not use a shared UserStore, the "<all>" user as well as the user "admin" are automatically created if no users exist yet on this instance. This "admin" user has the unqualified "admin" role associated, which means this user has the permission to do everything. On event servers, this user is typically used to give permissions to specific event admin user. The "admin" user's initial password is "admin" and should be changed before making that server publicly reachable.

Upon the first start of a server or server cluster, the server-specific user group is created. The server or server cluster represents a logical object with type ``SERVER`` that has a number of actions associated with it for which permissions are checked before a client can execute them. The ``SERVER`` object is owned by the server-specific group.

Additionally, on fresh instances not sharing a UserStore, a set of default roles with default permissions are created:

### Role "admin"

This role implies the "&ast;" permission. It should ideally be used with a user group ownership qualification, such as the server-specific group, making a user with a role qualified in this way an administrator for the server object as well as for all other objects owned by that server group. The ``admin`` role could also be used together with a user qualification, as in ``admin::{username}`` to grant a user full permissions to all objects owned by that user. Note that this in the future, as we introduce payed premium content, would include such premium permissions. Likewise, the ``admin`` role qualified to the user's default group ``{username}-tenant`` could be granted to users so they obtain full rights for all objects owned by their default ``{username}-tenant`` group. 

### Role "user"

This role is intended to be used to describe the permissions that object owners shall be granted. In particular, when qualified
for objects owned by the user being assigned the role, that user obtains the permission to execute actions
``CHANGE_ACL,CHANGE_OWNERSHIP,CREATE,DELETE,READ,READ_PUBLIC,UPDATE`` for objects of any type. By default, users are also assigned
this role qualified for objects whose group owner is the user's dedicated group (``{username}-tenant``).

Note that for good reasons this role does not imply all ("&ast;") permissions to object owners. All permissions would include features that we may want to charge a price for. The ``user`` role shall include only those permissions that all users with no premium permissions have. In a sense, the user role's permission set is the difference of the admin role's permission set ("&ast;") minus all premium permissions.

### Role "sailing_viewer"

This role can be used to publish the data of a sailing event. When assigning this to a user, the user obtains all
permissions required to ``READ`` the content of the sailing event, including leaderboards, regatta details, tracking, etc.
In order to constrain access to, say, a dedicated event, that event including its object hierarchy with leaderboards,
leaderboard groups, regattas, tracked races and so on can be transferred to a dedicated group owner. The ``sailing_viewer``
role can then be assigned, qualified for that group, to one or more users, or it can be assigned to the group owning
the event hierarchy now and can then be restricted to the members of that group or can be opened up to all users.

## Granting Permissions

Generally, a user who has a permission can grant this permission to another user. This is helpful, e.g., if a user wants to establish delegate users who may take over some of his/her tasks. Establishing such permissions may require another specific permission.

There are two exceptions, however.

- Permissions obtained through an Access Control List (ACL) are not considered when computing the set of permissions a user may grant to other users. Note that permissions *denied* by an ACL from a user cannot be granted by that user to anyone else, regardless of how that user may otherwise have obtained the permission.

- Permissions acquired through a role assignment to a user with the assignment not marked as "transitive" are not considered for the set of permissions a user can grant to others. An example for such a role is the "premium" role for which users generally have to subscribe and pay; obviously we don't want those users to be able to pass on the permissions obtained this way to other users. Note how this is different from permissions obtained through roles on a group (either for the members of the group only, or for all users): such permissions *do* add to the set of permissions a user can grant to other users.

## Server Properties

In order to modify server properties through the corresponding API or UI, the user requires the ``SERVER:CONFIGURE_LOCAL_SERVER:{server-name}`` permission. Note, though, that fewer permissions may suffice if the user makes the corresponding changes at a more basic level, e.g., by explicitly adding an ACL or adding a role to a group.

### Public Server

A server that is configured to be *public* adds the ``sailing_viewer`` role to its server-specific group (``{servername}-server``) and marks it to be granted to *all* users trying to access objects owned by the server group. When server administrators who are members of the server group follow the advice to make this server group their default creation group (the group used for the group ownership of new objects created by the user) then all those objects will be publicly readable.

Note that in order to carry out this change, a user must have the ``UPDATE`` permission for the server group because a role is *added* to that group. Furthermore, the user has to have acquired the permissions implied by the ``sailing_viewer`` role for all objects owned by the server group, not through an ACL nor through a non-transitive role assignment. The ``user`` role does imply these permissions. Therefore, for example, it is sufficient for a user to have a transitive ``user`` or ``sailing_viewer`` role assignment qualified for the server group in order to be able to switch that server to *public*. Alternatively, it is sufficient to be member of the server group if the server group implies role ``user`` or role ``sailing_viewer`` for its members when accessing objects owned by the server group.

### Self-Service Server

If a server is configured as a self-service server, an ACL is created for the ``SERVER`` object that grants the ``CREATE_OBJECT`` permission to the anonymous *null* group. This way, all users, including those not logged in, are permitted to create not only a new user during a sign-up process but also objects of any other types, such as events, regattas, or leaderboards. As explained above, in order to be permitted to configure a server like this, the user must himself/herself have acquired this permission, but not through an ACL and not through a non-transitive role assignment. Furthermore, the user must have the permission ``SERVER:CHANGE_ACL:{servername}``.

## Setting up Administrative Permissions for a New Application Replica Set

When a new application replica set is created by a user, a corresponding ``{servername}-server`` user group is created, owned by the user who creates the replica set, and owned by itself for the group ownership. The user is automatically made a member of that group, and the group is assigned an ACL that grants all its members the ``READ`` permission, so the members can at least see the group and its members and roles. Furthermore, the creating user receives a transitive ``user`` role assignment qualified for the new server group.

Based on the user ownership of the server group the user who creates the replica set has a few permissions for the ``SERVER`` object as well as the server group. In particular, based on the ``USER_GROUP:UPDATE`` permission acquired through the ownership and the ``user`` role that every user has assigned, qualified for the objects owned by the user, the user who created the replica set can add further users to the server group. The user creating the replica set also has all permissions implied by the ``sailing_viewer`` role for all objects owned by the server group because the ``user`` role implies all permissions implied by the ``sailing_viewer`` role, in particular for the ``READ`` and ``READ_PUBLIC`` actions. This way, the user who creates the replica set can also toggle its *public* state.

Two more 

For toggling the "self service" property, however, the ``user`` role does not imply sufficient permissions. 

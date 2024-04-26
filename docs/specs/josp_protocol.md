# JOSP Commons - Specs: JOSP Protocol

The JOSP Protocol is the communication protocol used by the JOSP Object to
communicate with the JOSP Services, directly or via the JCP GWs.

The first goal of the JOSP Protocol is to let the JOSP Object "send states'
updates" and the connected JOSP Services "send actions' commands" to the JOSP
Object.<br/>
Then it defines also other messages types to let the JOSP Object present itself
to the JOSP Services, to let the JOSP Services manage the JOSP Object's
configuration, permissions and query cached events and state's histories.

This is a text-based protocol that uses a simple message format to exchange
information between the JOSP Object and the JOSP Services. Messages are
formatted as a set of key-value pairs, with a header that defines the message
type and the message parameters. Then, optionally, a payload can be included
to provide additional, more complex, information.

Following the generic JOSP message format:

```text
{PROTO_NAME}/{PROTO_VERSION} {MSG_TYPE} {MSG_DATE}
{MSG_PARAMS}
[{payload}]
```

And here two examples of JOSP messages, from JOD Object and from JSL Service:

```text
{PROTO_NAME}/{PROTO_VERSION} {MSG_TYPE} {MSG_DATE}
objId:00000-00000-00000
[{MSG_PARAMS}]
[{payload}]
```

```text
{PROTO_NAME}/{PROTO_VERSION} {MSG_TYPE} {MSG_DATE}
fullSrvId:aaaa/bbbb/0000
objId:00000-00000-00000
[{MSG_PARAMS}]
[{payload}]
```


## JOSP Protocol messages list

**JOSP Object 2 JOSP Service:**

| Message Type       | Message Parameters                                                                 | Procedure                                                                                                        |
|--------------------|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| UPD_MSG            | objId, compPath, cmdType, update payload                                           | [State's Update Message (O2S)](#states-update-message-o2s)                                                       |
| OBJ_DISCONNECT_MSG | objId                                                                              | N/A                                                                                                              |
| OBJ_INF_MSG        | objId, objName, jodVerson, ownerId, model, brand, longDesc, desc, isCloudConnected | [JOSP Object Presentation (O2S)](#josp-object-presentation-o2s), [JOSP Object Sync (O2S)](#josp-object-sync-o2s) |
| OBJ_STRUCT_MSG     | objId, json<Struct>                                                                | [JOSP Object Presentation (O2S)](#josp-object-presentation-o2s), [JOSP Object Sync (O2S)](#josp-object-sync-o2s) |
| OBJ_PERMS_MSG      | objId, list<JOSPPerm>                                                              | [JOSP Object Presentation (O2S)](#josp-object-presentation-o2s), [JOSP Object Sync (O2S)](#josp-object-sync-o2s) |
| SRV_PERMS_MSG      | objId, permType, connType                                                          | [JOSP Object Presentation (O2S)](#josp-object-presentation-o2s)                                                  |
| H_EVENTS_MSG       | objId, reqId, list<JOSPEvent>                                                      | <RESPONSE> [JOSP Object History (S2O)](#josp-object-history-s2o)                                                 |
| H_HISTORY_MESSAGE  | objId, compPath, reqId, list<JOSPStatusHistory>                                    | <RESPONSE> JOSP Object History (S2O)](#josp-object-history-s2o)                                                  |

**JOSP Service 2 JOSP Object:**

| Message Type       | Message Parameters                                         | Procedure                                                         |
|--------------------|------------------------------------------------------------|-------------------------------------------------------------------|
| CMD_MSG            | fullSrvId, objId, compPath, cmdType, command payload       | [Action's Command Message (S2O)](#actions-command-message-s2o)    |
| OBJ_SETNAME_MSG    | fullSrvId, objId, objName                                  | [JOSP Object Configuration (S2O)](#josp-object-configuration-s2o) |
| OBJ_SETOWNERID_MSG | fullSrvId, objId, ownerId                                  | [JOSP Object Configuration (S2O)](#josp-object-configuration-s2o) |
| OBJ_ADDPERM_MSG    | fullSrvId, objId, srvId, usrId, permType, connType         | [JOSP Object Permissions (S2O)](#josp-object-permissions-s2o)     |
| OBJ_UPDPERM_MSG    | fullSrvId, objId, permId, srvId, usrId, permType, connType | [JOSP Object Permissions (S2O)](#josp-object-permissions-s2o)     |
| OBJ_REMPERM_MSG    | fullSrvId, objId, permId                                   | [JOSP Object Permissions (S2O)](#josp-object-permissions-s2o)     |
| H_EVENTS_MSG       | fullSrvId, objId, reqId, limits, filterEventType           | [JOSP Object History (S2O)](#josp-object-history-s2o)             |
| H_HISTORY_MESSAGE  | fullSrvId, objId, compPath, reqId, limits                  | [JOSP Object History (S2O)](#josp-object-history-s2o)             |


## Protocol Procedures

### State's Update Message (O2S)

Every time a JOSP Object's component state changes, it sends an `UPD_MSG` to all
connected JOSP Services with at least the `Status` permission.

The message specify which component hans been updated, his type and the update
payload that depends on the component type.


### Action's Command Message (S2O)

When a JOSP Service wants to send a command to a JOSP Object's component, it
sends a `CMD_MSG` to the JOSP Object that contains the desired component.

Like for the `UPD_MSG`, the `CMD_MSG` contains the component's path, his type
and the action payload that depends on the component type.

Then, the JOSP Object executes the action if and only if the sender JOSP Service
has at least the `Action` permissions.


### JOSP Object Presentation (O2S)

After the JOSP Service connects to the JOSP Object, the JOSP Object sends a set
of messages to present itself to the JOSP Service.

Those messages let the JOSP Service initialize his representation of the JOSP
Object. So this set of messages includes:
- `OBJ_INF_MSG`: the JOSP Object's general information (requires `Status` permission)
- `OBJ_STRUCT_MSG`: the JOSP Object's structure (requires `Status` permission)
- `OBJ_PERMS_MSG`: the JOSP Object's permissions (requires `CoOwner` permission)
- `SRV_PERMS_MSG`: the current JOSP Service's permissions on the JOSP Object


### JOSP Object Sync (O2S)

When some configuration or permissions changes on the JOSP Object, it sends a
set of messages to let the JOSP Services keep his representation synchronized.

Depending on what has been changed, the JOSP Object sends one or more of the
following messages:
- `OBJ_INF_MSG`: the JOSP Object's general information (requires `Status` permission)
- `OBJ_STRUCT_MSG`: the JOSP Object's structure (requires `Status` permission)
- `OBJ_PERMS_MSG`: the JOSP Object's permissions (requires `CoOwner` permission)


### JOSP Object Configuration (S2O)

If the JOSP Service has the `CoOwner` permission, it can change the JOSP Object's
configuration by sending one of the following messages:

- `OBJ_SETNAME_MSG`: change the JOSP Object's name
- `OBJ_SETOWNERID_MSG`: change the JOSP Object's owner


### JOSP Object Permissions (S2O)

If the JOSP Service has the `CoOwner` permission, it can change the JOSP Object's
permissions by sending one of the following messages:

- `OBJ_ADDPERM_MSG`: add a new permission to a user or a service
- `OBJ_UPDPERM_MSG`: update an existing permission
- `OBJ_REMPERM_MSG`: remove a permission


### JOSP Object History (S2O)

The JOSP Services can query the JOSP Object's history by sending one of the
following messages:

- `H_EVENTS_MSG`: query the JOSP Object's events history (requires `CoOwner` permission)
- `H_HISTORY_MESSAGE`: query the JOSP Object's status history (requires `Status` permission)

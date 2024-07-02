# JOSP Commons - Changelog

[README](README.md) | [SPECS](docs/specs.md) | [IMPLS](docs/impls.md) | [CHANGELOG](CHANGELOG.md) | [TODOs](TODOs.md) | [LICENCE](LICENCE.md)


## Version 2.2.4

* Improved the JOSP Commons documentation
* JOSP
  * Added JOSPSecurityLevel as enum for Local Communication security levels
  * Added static methods isFullSrvId() and isFullObjId() to JOSPProtocol class and removed unnecessary fields
  * Renamed wrong STATUS names to STATE:
    * JOSPPerm.Status > JOSPPerm.State
    * HistoryStatus > HistoryMessage
    * JOSPStatusHistory > JOSPHistory
    * HISTORY_EVENTS_REQ_* > EVENTS_MSG_RES_*
    * *HistoryEventsMsg*() > *EventsResMsg*()
    * HISTORY_STATUS_REQ_* > HISTORY_MSG_RES_*
    * *HistoryCompStatusMsg*() > *HistoryResMsg*()
    * HISTORY_EVENTS_REQ_* > EVENTS_MSG_REQ_*
    * *HistoryEventsMsg*() > *EventsReqMsg*()
    * HISTORY_STATUS_REQ_* > HISTORY_MSG_REQ_*
    * *HistoryCompStatusMsg*() > *HistoryReqMsg*()
* Comm
  * Added ping() method to Client interface and his direct subclasses
  * Added Certificate Sharing support methods to ClientAbsSSL class
* Discovery
  * Updated DiscoverySystemFactory to accept custom implementations
* Java
  * Updated JavaJSONArrayToFile for better performances
  * Added methods for JKS and SSL management
  * Updated default TrustMngr and KeyMngr Algorithms for JavaSSL class
* Various fixes
* Switched from `org.apache.logging.log4j` to `org.slf4j:slf4j-api`
* Updated GradleBuildInfo to version 2


## Isolate JOSP Commons 2.2.3

* Removed all NOT JOSP Commons files
* Moved required files to jospCommons sourceSet
* Removed all NOT JOSP Commons Gradle configs
* Cleaned Gradle configs and tasks
* Moved jospCommons sourceSet to main sourceSet
* Updated all dependencies to latest versions
* Changed default jospDependenciesVersion behaviour
* Updated the Gradle Wrapper to version 8.3
* Removed buildSrc dependency
* Created TODOs.md
* Updated README.md, CHANGELOG.md and LICENCE.md to updated JOD repository

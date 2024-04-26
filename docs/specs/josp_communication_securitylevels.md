# JOSP Commons - Specs: Communication Security Levels

Given the vast variety of devices and the countless use cases for IoT solutions,
the JOSP platform meets these needs by providing five different security levels
dedicated to local JOSP communications. These five levels allow JOSP objects and
services to adapt to the security and privacy needs of each individual use
case.

- NoSSL: Unencrypted communication
- SSLShareComp: Encrypted communication using a shared certificate but without
  the full ID
- SSLComp: Encrypted communication using a non-shared certificate and without
  the full ID
- SSLShareInstance: Encrypted communication using a shared certificate and with
  the full ID
- SSLInstance: Encrypted communication using a non-shared certificate and with
  the full ID

For example, objects based on hardware with low computing power may not be able
to handle SSL encryption, while other devices may require a higher level of
security to ensure data confidentiality.<br/>
Furthermore, certificate management can be a problem. For this reason, the two
levels SSLShareComp and SSLShareInstance have been introduced, which allow
sharing the certificate among peers before the connection. On the contrary, the
SSLComp and SSLInstance levels require that certificate sharing be disabled and
therefore, that each peer knows the other's certificate before establishing the
connection.<br/>
Finally, if the certificate used contains the full ID (of the object or
service), then it is referred to as an Instance connection, otherwise it is a
Comp connection. This difference is important to ensure the identity of the
object or service with which you are communicating.

Note: If the JOSP object always and only starts a single server (SSL or NoSSL),
JOSP services on the contrary try to connect to objects using an SSL client
first and, in case of failure, retry with a NoSSL client. This makes the
services more flexible and adaptable to objects with different configurations.

By default, the JOSP object starts a no SSL server, if instead
SSL encryption is enabled, without any other option, then the JOSP object is
started with an SSL server, generating a certificate containing the entire
object ID and with the certificate sharing functionality active. Therefore, the
default security level with SSL disabled is NoSSL, while if SSL is enabled then
the default security level is SSLShareInstance.

On the JOSP service side, the default configuration is to use an SSL client
first and only subsequently retry with a NoSSL client. The SSL client has
certificate sharing enabled and the generated certificate contains the entire
service ID. Therefore, the default security level with SSL enabled is
SSLShareInstance.

## Security level definition

The security level of a local JOSP connection depends on the configuration of
both sides of the connection, i.e., the JOD object and the JOSP service. Once
the connection is established and the IDs are exchanged, the security level of
the connection is deduced for each peer.
The security level of a local JOSP communication, on the JOSP object side, is
defined by the following parameters:

- The type of communication (SSL or NoSSL): Through the jod.ssl.enabled
  property, it is possible to instruct the JODLocalServer class to start an
  encrypted server or not.
- The possibility of sharing the remote certificate (Share or NotShare): If the
  connecting client is configured to share the certificate, then the security
  level will be SSLShare*.
- The type of remote certificate (Comp or Instance): If the certificate of the
  connecting client contains the full ID of the JOSP service, then the security
  level will be *Instance, otherwise *Comp.

NB: the security levels for both sides of the same connection can be different.
For example, a JSL Service without his ID into the certificate, can connect to
a JOD Agent with his full ID into his certificate. Both peer can share their
certificate. In this case the security level of the connection will be
`SSLShareInstance` on the JSL Service side and `SSLShareComp` for the JOSP
Object
side.


## Certificates sharing

When we talk about local JOSP communication, we refer to pairs of objects and
services that communicate with each other through a local network.
Theoretically, every service connects to every object within the same network.
Therefore, the number of possible client-server (service-object) pairs is N*M.

All SSL communications require, in addition to server verification, also
authentication and validation of the client's certificate. Therefore, both
server and client must have their own certificate.

In addition to the large number of certificates to manage, their distribution
also becomes a problem. For this reason, the two levels SSLShareComp and
SSLShareInstance have been introduced, which allow the sharing of certificates
among peers before the connection. Thanks to this function, both JOSP objects
and services can auto-generate a certificate and share it with peers before
establishing the connection.

For the JOSP ecosystem, the most widely used security level is precisely
SSLShareInstance. This level, in addition to allowing the sharing of the
certificate among peers, also allows the auto-generation of the certificate
containing the full ID of the object or service.

If, on the other hand, certificate sharing is disabled on at least one of the
two peers, then the connection will be established exclusively if both peers
know each other's certificate. This means that the certificates must be manually
distributed and loaded onto the peers before establishing the connection. In
this case, we speak of SSLComp and SSLInstance level connections.

These last two security levels allow limiting the peers with which a JOSP object
or service can communicate. In fact, if an object does not know the service's
certificate (or vice versa), then the connection will be refused.

Case A: An object accepts only 1 specific service

- JOD Agent: SSL, NoShare, PreRegister Service's Certificate
- JSL Service: -

Case B: A service accepts only 1 specific object

- JOD Agent: SSL
- JSL Service: NoShare, PreRegister Object's Certificate

Every JOSP object and service is identified by a unique ID at the instance
level. This means that every object (even of the same model) will have a
different ID. The same also applies to every instance of the JOSP service. The
ID of a JOSP object corresponds to its Object ID, while the ID of a JOSP service
is composed of its Service ID, User ID, and Instance ID.

When a certificate is generated, it is possible to include the full ID of the
object or service. In this way, the identity of the peer with whom you are
communicating can be guaranteed.

In the case where the remote peer of a local JOSP connection includes the full
ID in its certificate, then the connection will be at the *Instance level.
Otherwise, the connection will be at the *Comp level.

The auto-generation of certificates, by JOSP objects and services, always
includes the full ID of the peer.

To manually generate a certificate, execute the following command and correctly
set the jod.comm.local.ks.* property of the configuration file jod.yml:

```shell
$ keytool -genkey -noprompt -keyalg RSA -keysize 2048 -validity 3650 \
  -alias {KS_ALIAS} \
  -dname '{KS_DN}' \
  -keystore {KS_PATH} \
  -deststoretype pkcs12 \
  -storepass '{KS_PASS}' -keypass '{KS_PASS}'
```

Remember to replace the values of {KS_ALIAS}, {KS_PATH} and {KS_PASS} with the
values set in the jod.yml file. While the value of {KS_DN} must be a string that
represents the Distinguished Name (DN) of the certificate. This value must
contain at least the CN={CN} pair, where {CN} corresponds to the ID shared via
certificate.
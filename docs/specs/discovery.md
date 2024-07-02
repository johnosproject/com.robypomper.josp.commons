# JOSP Commons - Specs: Discovery System

Discovery System let the JOSP Services find JOSP Objects on the same network.

Here are the available implementations:
- [Auto](../impls/discovery/auto.md): auto detect best discovery system available
- [Avahi](../impls/discovery/avahi.md): use the Avahi (Linux) cmdline tool
- [DNSSD](../impls/discovery/dnssd.md): use the DNS-SD (MacOS) cmdline tool
- [JmDNS](../impls/discovery/jmdns.md): use the JmDNS Java library
- [JmmDNS](../impls/discovery/jmmdns.md): use the JmDNS Java library

TODO: describe how the discovery system is structured, how it works and how to use it.
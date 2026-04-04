ProtocolSupportBungee
================

[![Build Status](https://build.true-games.org/buildStatus/icon?job=ProtocolSupportBungee)](https://build.true-games.org/job/ProtocolSupportBungee/)
[![Chat](https://img.shields.io/badge/chat-on%20discord-7289da.svg)](https://discord.gg/x935y8p)
<span class="badge-paypal"><a href="https://www.paypal.com/cgi-bin/webscr?return=&business=true-games.org%40yandex.ru&bn=PP-DonationsBF%3Abtn_donateCC_LG.gif%3ANonHosted&cmd=_donations&rm=1&no_shipping=1&currency_code=USD" title="Donate to this project using Paypal"><img src="https://img.shields.io/badge/paypal-donate-yellow.svg" alt="PayPal donate button" /></a></span>

Support for 1.6, 1.5, 1.4.7, pe clients on BungeeCord<br>
This plugin is under development

Important notes:
* Only latest version of this plugin is supported
* This plugin can't be reloaded or loaded not at BungeeCord startup

---

ProtocolSupportBungee is a passthrough protocol plugin, not a converter, so servers behind BungeeCord should also support those versions

Also servers behind Bungeecord should support https://github.com/ProtocolSupport/ProtocolSupport/wiki/Encapsulation-Protocol

The preferred setup is to put ProtocolSupport to all servers behind BungeeCord

---

Optional config (`plugins/ProtocolSupportBungee/config.yml`):

```yaml
encapsulation:
  enabled-by-default: true
  disable-for-targets:
    - "127.0.0.1:25566"
```

If a backend target (`host:port`) is listed in `disable-for-targets`, ProtocolSupportBungee uses the old/direct backend method (without encapsulated handshake) for that target.

---

Troubleshooting
---------------

If players are kicked with:

`Could not connect to a default or fallback server, please try again later: io.netty.channel.AbstractChannel$AnnotatedConnectException`

that message usually comes from BungeeCord failing to open a TCP connection to the backend server. In most cases this is not a ProtocolSupportBungee decoding issue.

Even if direct client -> backend join works, proxy -> backend can still fail (different network path, DNS result, bind address, firewall rules, container network namespace, or wrong backend address in Bungee config).

Check:

* backend server is online and listening on the configured `host:port`
* backend is bound to an address reachable from the proxy (e.g. not only `127.0.0.1` when proxy runs on another host/container)
* server name in `priorities` / forced-host points to an existing backend
* backend firewall / Docker network / bind address allows proxy connections
* target version setup matches passthrough requirements:
  * ProtocolSupport on Bungee
  * ProtocolSupport (or equivalent compatibility) on backend servers
  * encapsulation config (`disable-for-targets`) matches backend capabilities

Use BungeeCord logs around the failed join to confirm the exact backend address and connection refusal/timeout reason.

Isolation test for possible plugin compatibility bugs:

1. Set `encapsulation.enabled-by-default: false` and restart BungeeCord.
2. Try joining through the proxy again.
3. If join starts working only with encapsulation disabled, treat it as a ProtocolSupport/encapsulation compatibility issue (not pure network) and report it with full proxy log + backend target address.

---

Jenkins: http://build.true-games.org/job/ProtocolSupportBungee/

---

Licensed under the terms of GNU AGPLv3

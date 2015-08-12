LGControl
=========

LGControl is an Xposed module intended for use on Amazon Fire TVs.  The Fire TV remote is Bluetooth
without any IR blaster capabilities, so it can't control the actual TV, leaving you dependant on two
remotes.  Initially, I've added volume control via ethernet for certain 2011 LG TVs.  I may expand
to support more LG TVs over ethernet, though I only have my one TV to test with.

Only the TV-on command is done via CEC, as this is the only command I reverse engineered from the Fire TV source.  TV-off and Volume-up/down are done via ethernet, and thus are specific to the model of TV.  If I can compile native code using the Android NDK and the Fire TV source headers, it is theoretically possible to do all commands over CEC and work with any modern TV, but I haven't had luck configuring the toolchain yet.

TODO
----
- Automate TV IP detection - LG TVs broadcast their identity over a slight variant of UPnP.
- Internally capitalize pair code, since the code is case sensitive and Fire TV launching caps keyboard seems broken.
- Allow for custom key mapping.

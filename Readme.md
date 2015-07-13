LGControl
=========

LGControl is an Xposed module intended for use on Amazon Fire TVs.  The Fire TV remote is Bluetooth
without any IR blaster capabilities, so it can't control the actual TV, leaving you dependant on two
remotes.  Initially, I've added volume control via ethernet for certain 2011 LG TVs.  I may expand
to support more LG TVs over ethernet, though I only have my one TV to test with.

Ideally, I'd like to do this and more over CEC, but I don't know about the Fire TV's CEC interface.
In my testing, it does accept CEC commands (you can use the TV remote to control the Fire TV), but
only if the Fire TV is turned on *after* the TV.  If at any point the TV is turned off and back on
again, while the Fire TV is left on, CEC stops working.  However, my intention is the reverse - the
Fire TV controlling the TV.  If I do this, it'll be a separate project that will likely eclipse this
one.

TODO
----
- Remove the hardcoded ip address - LG TVs broadcast their identity over a slight variant of UPnP.
- Pop up a capitalized keyboard when pairing, since the code is case sensitive.
- Allow for custom key mapping.
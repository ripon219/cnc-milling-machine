# Introduction #
For a while I've been thinking that the serial interface to the router is too limiting.

  * The cable length is limited,
  * The devices that can interface must have a serial port (or an adapter)
  * The state of serial drivers for Java is kind of weak

Accordingly, I have been planning on resolving that issue.  My first choice is connectivity via WiFi, and the cheapest way I know to do that is to use one of my WRT54G routers with an embedded Linux distribution.  I've got five of them lying around the house, so it is just a matter of some messing around with hardware and software to get it to work.


# Details #

## Hardware ##

The router I'm using (WRT54G-TM) does not come with user accessible serial ports by default, so I needed to add a couple serial ports.  While I was at it, I also added a SD/MMC card slot with a custom (milled of course) PCB.  The details of the hardware mods will be posted soon on [my blog](http://vinces-electronics.blogspot.com/).  There is plenty of other information about how to do these mods available - see the links below for more detail.  The board I created connects the serial ports to front panel RJ-45 jacks wired using the Cisco standard for Console ports.


Okay - for now I will [document the adapter pinouts](CiscoConsoleAdapterPinouts.md) that I have tested with the boards.

Oh yeah - if you read this and are interested in buying a professionally made (etched, double-sided with solder mask and silk screen) PCB to add a pair of serial ports and a SD/MMC card slot to the front panel of a WRT54G router, post a comment or contact me.  I've got 5 more of these to retrofit, and my friends have a few also, so I'm thinking about getting some professionally made and selling the rest to like minded folks at cost.  looks like I can panelize aggressively (using my mill to cut them apart later) and get the boards for around $10-11 each.  I'll probably make a design revision or two before committing to creating them.  I'm seriously thinking about adding a header and mounting holes to plug in a daughter board that could add additional I/O via a PIC processor or the like.  With a connection to the serial port and some creative use of the GPIO pins used for the SD card, I could make a flash loader that could flash a new firmware image to the PIC via the Linux OS - probably with a web interface.

I would also be willing to buy the components in bulk (connectors, rs-232 transceiver, etc to make a kit, but I'd need commitments to buy them before I would do that.  Until then, I can supply a parts list.


## Connecting via IP ##

Don't expect any formatting here - this section is just a dump of what I did to make it work (including all of the failed steps that may help someone else).  Heres the dump:


The specific hardware I used for this is a WRT54GS-TM with a serial number starting with C061.  I have a few of these from when T-Mobile was liquidating them for $20 each.  It takes a sepcial procedure to flash these with an embedded linux see http://www.dd-wrt.com/wiki/index.php/WRT54G-TM for instructions.  I have only flashed these initially with DD-WRT, and have read enough reports about it, that I'm pretty sure that is the best way to go, no matter what firmware you settle on.

The hardware part was challenging when you're not sure what the software is doing, or how to get the combination of **stty** and **setserial** to get the port working.  Worse yet - it seems if you fail to disable some of the break abort type operations, the first time the router sees a BRK (e.g. constant low signal) it just stops talking.  So a baud mismatch automatically locks things up nice and tight.  That coupled with some uncertainty about which pin is sending and which is receiving, and if the baud rates match led me to connect the oscilloscope to the tx and rx lines on the serial port and watch the serial traffic.  I can see which side is rx and tx to get the pinout right, and more important, verify that the baud rate is good (e.g. the width/time of the bits in the stream).


On DD-WRT ( I got the serial port talking to the milling machine with the following magical incantations:

```
$ stty 19200 cs8 -parenb -cstopb -echo -echoe -echoctl -echoke -echok -onlcr -icrnl -opost -ignbrk -ixon -F /dev/tts/1

$ nc -l -p 23000 </dev/tts/1 >/dev/tts/1 &
```

This lets me telnet to port 23000 on the machine and control the mill with a minimum of invalid command type messages, although any movement dies with a "SERIAL ABORT" - presumably because of some echoing of the movement feedback.  Not going to worry about that - so far this is all proof of concept - time to try a permanent solution.

That is good for proof of concept, but scripting it with netcat seems like a nasty hack when better stuff is available, so off I went to research options.  The best for my purpose seems to be **ser2net**.

I found ser2net at http://ipkg.nslu2-linux.org/feeds/optware/ddwrt/cross/stable/ser2net_2.5-1_mipsel.ipk

I downloaded and installed with:

```
$ cd /jffs

$ wget http://ipkg.nslu2-linux.org/feeds/optware/ddwrt/cross/stable/ser2net_2.5-1_mipsel.ipk

$ /bin/ipkg install ser2net_2.5-1_mipsel.ipk
```

That gave me an installation under /jffs/opt/.  I adjusted by PATH and LD\_LIBRARY\_PATH to include those locations and tried to run it... and got a ":not found" message from the shell.  Hmmm.

  * Double check that the binary is where it belongs - nope not that,
  * Specify the configuration file location on the command - nope not that either
  * Double check the library path - nope - that looks good.

Still, must be a library issue.  I don't have ldd to find the library dependancies, so tried "strings ser2net | grep .so"  to get the dynamic library names - wow - that worked pretty well.  Too bad they are all there.  Sigh.  Time for some googling for answers.

I found some references to binary incompatibily between the OpenWRT and DD-WRT libraries in [this wiki entry](http://www.dd-wrt.com/wiki/index.php/Ipkg) so I proceeded to try those instructions:

```
$ cd /jffs
$ wget http://downloads.openwrt.org/whiterussian/packages/uclibc_0.9.27-9_mipsel.ipk
$ wget http://downloads.openwrt.org/whiterussian/packages/libgcc_3.4.4-9_mipsel.ipk
$ /bin/ipkg -force-depends install uclibc_0.9.27-9_mipsel.ipk libgcc_3.4.4-9_mipsel.ipk

$ wget http://ipkg.nslu2-linux.org/feeds/optware/ddwrt/cross/stable/ser2net_2.5-1_mipsel.ipk
$ ipkg install -d /jffs ser2net_2.5-1_mipsel.ipk
```

Nope - still no joy.

Looks like its time to give up on DD-WRT - the OpenWRT package system is just too much easier for this, so now I'm going to re-flash with the latest stable OpenWRT build - the Kamikaze 8.09.1 build for bcm47xx/openwrt-wrt54gs-squashfs.bin.  Flashing it is a piece of cake from the DD-WRT web console.

DD-WRT has a much better web configuration page.  It is by far the best for a device you are using as a real router, bridge or repeater.  If you are doing anything else e.g. embedded processing like creating a wireless serial port server -- OpenWRT is probably a better choice, but you better be comfortable at a command line.  OpenWRT is clearly more of a hackers build.


Back to the narrative:

Got the router flashed and started and, wait a minute, where is my wireless interface.  This is frustrating.  Off to google a bit more.  Found a few comments that seem to relate, but slightly different hardware -- rude response about the release notes and a driver that was removed.  Okay - that doesn't match my problem but I just found information about broken broadcom wireless drivers in the 2.6 kernel - yep that's it:

kamikaze 8.09.1 build has no support for the WRT54GS wireless in the 2.6 kernel builds.   That is not going to work for me ( the funny thing is I picked that one because it is what I was successfully running on the WRT54G V2.0 that I'm using as a wired switch (and a embedded playground) in my living room.  I always assumed that the wireless did not work because the friend I got it from toasted the transmitter by cranking up the transmit power too high with DD-WRT.  That might still be the case, but it makes you wonder.

Trying to re-flash with brcm-2.4 branch of 8.09.1 openwrt-wrt54gs-squashfs.bin to solve that wireless issue.  should be interesting.

That didn't work - said "The uploaded image file does not contain a supported format" -- apparently kamikaze wants to flash with a .trx file, so now I grabbed the generic version "openwrt-brcm-2.4-squashfs.trx", which it liked okay.

Proceeding to flash again...

Ah yes - that is much better- now the /etc/config/wireless file is no longer empty, so the startup found a wireless chipset that it will talk to.

Configuring for my wireless network now.  Looks like the rest of my configs survived that flash, so I am pretty happy about that.

enabling boot\_wait with:

```
$ nvram set boot_wait=on
$ nvram commit
```

Disable dnsmasq (its a client with a static address - no need to have dns or dhcp services

```
$ /etc/init.d/dnsmasq disable
```

Now install the ser2net package:

```
$ ipkg update
```


ARRRGH!  What! Not found!  But ipkg is the heart of the OpenWRT package system.  must google that to find out how such a travesty can occur...

Oh look - they renamed the ipkg to opkg - nice to document that in the install guide ( or add a symlink or shell script that says use opkg instead to save folks like me some time)  I'm not very impressed with that - of course it IS open source, so I should just shut-up and add that to the trunk (adding that to my TODO list).  The tone of some of these forum posts is disturbing too.  Lots of "OpenWRT is a hackers kernel" implying that whoever asked the question is no hacker.  Pretty sure I would not have gotten this far if that was true for me.  That is more like an excuse for not doing the very basic steps to make major changes clear, like the aforementioned script.


Enough complaining.

Now, to install the ser2net package:

```
$ opkg update
$ opkg install ser2net
```

Configure it:

```
$ vi /etc/ser2net.conf
```

Basically I end up with just one uncommented line:

```
2001:raw:0:/dev/ttyS1:19200 NONE 1STOPBIT 8DATABITS -XONXOFF LOCAL -RTSCTS
```

Test it with:

$ ser2net -p 2000 -d

This runs the configuration and opens a monitoring port on 2000 that can be used to view status - very nice.

```
$ telnet mill 2001
```

and I see the mill's output in the screen.  It eaven seems to read my commands (more or less)

To make it run all the time:

created a new script with the following

```
$ cat <<EOF >/etc/init.d/ser2net
#!/bin/sh /etc/rc.common
# Start ser2net at every boot
START=10

start() {
        echo start ser2net
        /usr/sbin/ser2net -p 2000
}

stop() {
        echo stop ser2net
        killall ser2net
}
EOF

$ chmod a+x /etc/init.d/ser2net 
$ /etc/init.d/ser2net enable
```


Reboot the router to make sure everything runs, do my telent to port 2001 again and all is well (almost)

I found that it helps to put telnet in character mode and set the crlf option.  None of that will matter when I change the software to allow connections to the mill via tcp/ip sockets.  Watch the source repository - those changes will be coming soon.  Maybe even before I use the mill to create the face plate to cover the ugly Dremel hack job I did on the front of the router.  Again - thats hardware - I'll document that soon on [my blog](http://vinces-electronics.blogspot.com/).





# Links #

  * http://www.openwrt.org
  * http://www.dd-wrt.org
  * http://vinces-electronics.blogspot.com/
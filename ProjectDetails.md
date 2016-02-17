# CNC PCB Milling Machine Project #

## Overview ##

The CNC PCB Milling Machine project is a light duty Computer and Numerical Control (CNC) milling machine designed primarily to mill isolation traces in copper clad printer circuit board material.  Additionally, it can handle milling of soft materials such as most plastics and aluminum.  Before starting this project, I would have said all plastics, then I discovered Ultra-High Molecular Weight Polyethylene (UHMW).  That stuff is **HARD**

This entire project is now open source.  This includes the design of the physical machine itself (most of which I owe to many other excellent sources), the design of the electronic motor driver circuit, the PIC assembly source code for the firmware embedded in the driver circuit, and the Java source code for the GUI based application that runs the machine.

I am very much a junk box designer.  The primary selection criteria from most of the hardware that makes up this project was what I already had on-hand or could obtain cheaply form local resources and EBay.  The design of the physical machine was limited by what I could do with a limited set of power woodworking tools (Table Saw, small Drill Press, Router and Router Table, and Power Miter saw).  There is practically nothing in the physical design of the machine itself that did not come from one of the many excellent references sources I found via a never-ending series of Google searches.

The software is my own design, influenced heavily by the ideas of many others that had done similar projects in the past.  It is not the best example of my Java development work, but I am proud of it just the same.

Currently, the software works only with my machine microcode.  It does not generate any G-Code output or convert between formats, but it certainly could go that direction.  There is a simulated machine mode that can work without a connection to the microcode.  It will do the displays and generate the isolation traces, etc.  Modifications to output G-Code (or any other protocol) would not be terribly hard to do, and could be fit into the project nicely.

## Why? ##

  * I like designing and building electronic circuits, but I rarely developed anything beyond a breadboard stage because I dislike most prototyping methods.
  * Chemical Etching sucks
  * PCB Prototyping houses just cost too much in small volumes.
  * My regular rockclimbing partner found other interests, so I found myself with some spare time on weekends.  If you happen to be in Tucson on a weekend and want to go climbing -- call me!
  * I honestly didn't think it would take me 9+ months to finish. (Ha! as if it will ever be finished)
  * I am way too cheap to buy a commercial machine.
  * I just loved the idea of building a machine that I would use to build additional parts of the machine.

## Project Components ##

### Component Overview ###

The primary components are:

  * Mechanical: The physical three-axis milling machine based on a Dremel tool, stepper motors and Acme drive screws.
  * Hardware: A Microcontroller based three axis stepper motor controller board.
  * Firmware: The microcode that is burned into the controller board's processor.
  * GUI: A GUI controller application written in Java that does all of the heavy lifting related to processing of various file formats, calculation of isolation trace patterns, and simplified manual control and monitoring of the machine.

Because this open-source project is more about software than hardware, I will list these in reverse order.

### The Java Swing Controller application ###

http://cnc-milling-machine.googlecode.com/svn/wiki/screenshot.PNG

This is a Java Swing application that was originally designed just to provide a graphical view of the milling machine so it would be easier to manually control and track the status of the machine.

It evolved to a fully featured control program that:

  * Reads multiple input file formats including:
    * Excellion NC Drill files
    * A subset of RS-274X Photo Plotter files (G-Code)
    * Roland Modela (RML) milling files (which I later found are a subset of HPGL)
    * A custom CSV file format I invented so I could show my wife what it was supposed to do.
  * Performs a number of critical PCB related processes including:
    * Conversion of photo plotter pads and segments into contiguous traces
    * Generation if Isolation tool paths around traces.
    * Generation of raster tool paths to cut trace interiors and mill large holes.
    * Allows simple optimization of milling sequence.
    * Allows mirroring across X axis for cutting isolation traces on a bottom layer.
  * Displays and allows GUI modification of all machine parameters.
  * Ten level undo operation
  * Contains a status bar showing:
    * Mouse position
    * Number of MillingActions loaded
    * Total inches of travel represented by the milling actions
    * Estimated time to process the actions, number of currently selected actions
    * A status message for current operations that shows one of:
      * x, y, and diagonal distance of the mouse selection box
      * toString results of a single selected action
      * progress of a current long running action thread

The structure of the application is very object oriented (although it can use some refactoring).

  * The physical machine is represented by a MillingMachine Javabean that has methods to control the major machine functions in a logical manner.
  * The MillingMachineFrame interacts with this bean to represent a graphical view of the machine state.
  * A class SurfacePanel handles the display of the machine coordinate space.
  * Individual machine operations such as DrilledHole and MilledLine represent MillingActions that may have both a graphical and executable component.  E.g. how should the action display on the screen, and what interactions with the MillingMachine are necessary to mill that instance on the PCB.
  * Not all MillingAction subclasses can be executed.  Some are logical GUI representations such as a TracePad, TraceSegment, or TracePolygon, which represent pads and segments loaded from a PhotoPlotter file, or calculated logical traces.
  * MillingActionList represents the set of loaded MillingActions, and is the class in which set operations such as calculating traces or isolating traces are implemented.  This could benefit from the implementation of a Command or Visitor pattern to allow the specific actions to be separated into pluggable modules.


### Firmware: The PIC 16F877 Driver Microcode ###

The microcode controls the circuit that drives the actual hardware pieces: stepper motors, limit switches, relays to control the drill (Dremel tool) and vacuum pickup.

It is written directly in the RISC assembly language for a PIC16F877 microcontroller chip.

For those that don't know, this is one of many remarkable little chips that combine an 8 bit processor, RAM, EEPROM storage, and Flash memory for program storage onto a single Integrated Circuit.  This particular IC has (ignoring many features I did not use):

  * Operating speed from DC to 20 MHz , with a minimum instruction cycle of 200 ns.
  * Up to 8K x 14 words of FLASH Program Memory
  * Up to 368 x 8 bytes of Data Memory (RAM)
  * Up to 256 x 8 bytes of EEPROM Data Memory
  * High Sink/Source Current: 25 mA
  * Universal Synchronous Asynchronous Receiver Transmitter (USART/SCI) with 9-bit address detection.
  * Up to 33 available General Purpose I/O pins


I picked this IC because I happened to have a couple in my junk box and it had all the features I needed.

I used the **free** MPLAB IDE downloaded from the [Microchip web site](http://www.microchip.com) to write and simulate/debug the program.

The microcode provides a simple command language to control the machine via an RS-232 serial connection at 19.2k baud with 8 bits, No Parity, and 1 stop bit.

The microcode implements the following commands:

```
 MN<cr> = Manual Control
 Mx+nnn[y+mmm]<cr> = Move axis x +/- nnn steps
 Cx<cr> = Calibrate axis x (reset to 0)
 SDx+nnn<cr> = Set Delay for axis x +/-/= nnn delay
 SIx=nnn<cr> = Set Inch size on axis x to nnn steps
 L<cr> = Lists current settings and location 
 Hx0 = Holding current for axis x Off (0) or On (1) 
 D0<cr> = Drill Off (0) or On (1) 
 V0<cr> = Vacuum Off (0) or On (1) 
 B<cr> = Backup startup config  
 ?<cr> = Dump this message 
```

The Java program accomplishes its magic by sending combinations of these commands.  The microcode indicates it is ready to receive a command by sending a '$' prompt.  Commands do not return the $ prompt until they are complete.

You may note that the "Mx+nnn[y+mmm]" command does ONLY relative motions.  The machine does track its position, but it is the job of the Java program to translate the current position and desired position in to relative motions to get there.  It is also limited to 254 steps per movement.  What can I say: it's an 8 bit processor and I am lazy.

The simultaneous movement of both x and y axis at a time uses a modified [http://en.wikipedia.org/wiki/Bresenham's\_line\_algorithm Bresenham line algorithm](.md) to move the cutting head in the best possible straight line between the current point and the offset point.  The z axis is moved independently. When either the x or y axis is moved with the z axis in a single command, the z axis is always moved first.  The net result is this version is good for 2D milling, but not 3D milling at this time.

The SD delay command controls the speed of stepping by setting the delay (in milliseconds) between steps.  Higher numbers mean slower movement.  I find that my stepper motor and hardware arrangement will run unloaded with a delay of 30ms without losing steps.  If there is some drag on the movement, I may have to up the delay to 60 or 100ms to avoid losing steps.

The SI (steps per inch) is really only used in the microcode by the MN (manual movement) command (see manual mode below).  When using the Java frontend, this is loaded and used for the manual movement buttons only.  Actual steps per inch will be determined by your hardware.

The CX, CY, and CZ commands let you set the zero axis for your milling operations.  This really should allow calibration to other than a zero point -- expect to see that in a future version. (This feature has added with new gui and firmware versions)

The MN command was added before I ever created the Java front-end.  Basically it lets you move the milling head around (really it moves the table around). By pressing specific keys.

The key layout is

```
Use the following motion keys:
 Q(Z+)  W(Y+)   
A(X-)  S(Stop)   D(X+)
 Z(Z-)  X(Y-)  Press <sp> to exit manual mode 
```

You may notice the layout corresponds to the positions of the keys on the left side of a QWERTY keyboard.  Pressing a directional key (like Q) will move the specified axis in the specified direction one inch (or whatever value you set the inch register to with the SI instruction), or until you press the S key.

Manual mode is handy to calibrate and test your machine -- all you need is a terminal program.

There are enough spare I/O pins on the 16F877 that I considered hooking up a keypad to control the machine manually, but the more I use it, the more I realize it is just much easier to use the Java interface.


The V,D, and H commands just control the status of the Vacuum relay, the Drill Relay, and the Holding Current.

Holding Current on for an axis means that the stepper is locked into its current position even when it is not moving.  That means it is less likely to move in that direction through drag or vibration, and that your stepper motors are going to get **hot**.  The Java front-end enables and disables holding current for drilling and milling operations in the way that I have found works best.  When controlling the microcode directly with a terminal program, it is best not to leave holding current on too long unless you build in some thermal detection or current limiting, or you are probably just going to start a fire.  You have been warned.


### Hardware: The Electronic circuit design ###

http://cnc-milling-machine.googlecode.com/svn/wiki/electronics.JPG

The electronics portion of this project consists of a unipolar stepper motor and relay driver board that was actually milled and drilled using a breadboard prototype version of the same circuit to drive the milling machine.

The project files in the SVN repository include a PADS-PCB format netlist, a board layout in FreePCB format, and the Gerber and NC drill files I used to create the board.  The version 1.0 copies of these files exclude one bypass capacitor and an additional ground connection I had to add after the initial board was milled.  See the section on PCB Engineering Changes below for details.

The central component is, of course, the PIC 16F877 microcontroller chip in a 40 pin .600 wide DIP package.  This controller chip is programmed with the firmware described above.

The GPIO pins on the PIC are used as follows:

```
; Hardware config:
; PortA = Z Motor Control & Limit Switches
;  RA5 = \Limit-Zmax
;  RA4 = \Limit-Zmin
;  RA3 = Z Coil A
;  RA2 = Z Coil \A
;  RA1 = Z Coil B
;  RA0 = Z Coil \B 


; PortB = Not used – extended to a dual row .100 header block with ground and +5v for future expansion.

; PortC = Limit Switches,  Serial
;  RC7 = Serial RX
;  RC6 = Serial TX
;  RC5 = \Limit-Ymax
;  RC4 = Not Connected
;  RC3 = Not Connected 
;  RC2 = \Limit-Ymin
;  RC1 = \Limit-Xmax
;  RC0 = \Limit-Xmin

; PortD = Motor Control
;  RD7 = X Coil A  
;  RD6 = X Coil \A 
;  RD5 = X Coil B  
;  RD4 = X Coil \B 
;  RD3 = Y Coil A  
;  RD2 = Y Coil \A 
;  RD1 = Y Coil B  
;  RD0 = Y Coil \B


; PortE = Relay control 
;   RE2 = Not Connected
;   RE1 = Vacuum Relay
;   RE0 = Drill relay
```

If you were keeping track, you may have noticed that 11 of the available GPIO pins are not currently used.  I left Port B open for possible future keypad and LCD interface, RC3/RC4 for a possible I2C EEPROM expansion or interface to a SPI device like a SD card.  The reality of this is if I were to build it again, I would probably opt for a lower pin count device and only leave the I2C pins for expansion.  Additional memory, LCD and keypad interfaces would be much easier to connect via a single I2C bus and would have cleaned up the board layout.

In the above table, the '\' is used to indicate active low signals or, in the case of the motors the **not** connections of the unipolar stepper coils.  For example,  \Limit-Zmax indicates the connection for the Z Axis Maximum limit switch, which should assert a logic low to indicate the switch is tripped.

The circuit can be split into a few simple subsystems.  Motor drivers, relay drivers, serial communications, and limit switch connections.

The three (x,y,z) motor driver circuits are all very simple.  Each motor is a unipolar stepper motor with two bipolar coils to drive it.  As used by this circuit, Coil A and Coil B are center tapped coils with each end of the coil pulled to ground by a separate signal.  The each of the coil ends (A,\A,B,\B) are connected to the Drain of four separate IRL530 MOSFETs.  The center of each coil is connected to a nice stiff positive potential (in may machine, this is +12V).  The Source of each MOSFET is tied to ground.

The gate of each MOSFET is connected via a (10k) current limiting resistor to one of the PIC pins configured as an output.  To energize the motor coil, the software drives the pin to TTL high, causing the MOSFET to conduct and pull the end of the motor coil to ground (sinking a significant amount of current in the process).

The specific MOSFETs used (IRL530) are designed to be driven by logic levels.  They also contain a reverse diode to shunt the substantial back-emf spike that occurs when the coil is switched off.  I used MOSFETs so there would be very little voltage drop to the motor coils, and the switching device itself would not have to dissipate much heat.  If you prefer, a TIP120 or equivalent bipolar junction transistor could be substituted here.  I actually used TIP120's in this circuit before my IRL530's arrived from [Jameco](http://www.jameco.com)

To provide visual feedback of the state of each motor coil, I also included a 220 ohm resistor and LED between the PIC output port and ground (in parallel to the 10 k current limiting resistor).  These could be omitted with no effect on the operation of the circuit.

The motor drive is repeated three times, one for each axis driven.

The actual motor connectors are 8 pin .100 pin headers in the following sequence:


'/A ,+V, +V, A, B, +V, +V, \B'

+V is the positive voltage potential for the center of the coil.  This provides four separate connectors for the positive supply, allowing for any combination of 5,6, or 8 wire stepper connections.  It also is symmetrical, so if you find that one of the motors goes backwards, you can just flip the connector.

Software is responsible for driving the motor coils in the proper sequence (which always has two motors energized).  Half-stepping or less power hungry

The Relay drivers are very similar to the motor circuits MOSFETs driving coils, but they also include an opto-coupler to allow both a different positive and ground potential for the coil than the logic circuit.

The relay circuit has the PIC output pin driving the LED in a packaged dual phototransistor optocoupler (ILD615-3) via a current limiting resistor (220 ohm) in parallel with a separate current limiting resistor (220 ohm) driving a (optional) relay status LED.

The phototransistor in the optocoupler switches the emitter from the +RLY power source to the Gate of a MOSFET.  The Source of the MOSFET is tied to the -RLY power source, while the Drain drives the relay coil.

This isolates the logic ground that drives the optocoupler LED from the separate +RLY power source that acts as the source potential for the relay coil.

The result is the relay coil (which is a 24v model I happened to have laying around) is driven with 24v because the +RLY is connected to a +12v source, while the MOSFET pulls the other end of the coil to a -12v potential provided on the –RLY source.

Because I drive the whole thing with a nice cheap ATX power supply I had laying around, that gets the job done.  Had I skipped the optocoupler, the only way to supply 24v to the relay would have been a 24v positive supply tied to the same ground as the +5v logic supply.  That gets expensive.

I should have done the same optocoupler thing with the motor drives, but by that time I just wanted to get the first version done.

The relay switches are connected to spade connectors on the PCB (yup -- salvaged those from another board too).  The spade connectors have a ground, AC IN (full **hot** line current), and Switched Vacuum Out and Drill Out connectors.

On my machine, I have a separate outlet box with a standard 120V AC light switch and two outlets.  The powerline from the wall connects the hot to the light switch, the light switch to the AC In on the PCB, and the switched vacuum and drill outputs to the outlets.  The ground is connected to the switch, PCB and outlets, and the neutral is connected to the outlets.

The result is when the switch is on, the outlets are controlled by the PCB, and when it is off, there is no AC power to the relays or outlets.  This gives me a safety switch to make sure the drill will not come on while I am changing bits due to a hardware or software glitch.  I like my fingers the way they are.

I should have done the same optocoupler thing with the motor drives, but by that time I just wanted to get the first version done.

The limit switch circuits are just PIC input pins run to .100 pin headers with +5V and ground connections to allow flexibility in the connections.

Truth be told, in my first board, a lost step problem caused by running the machine too fast while cutting the board meant that the milled traces and holes were positioned in such a way that the pin headers would not fit.  So, on the first board I soldered wires directly to the board and made the connections to the limit switches with solderless bullet connectors.

The board does not include any pull-up resistors for the limit switches.  In my implementation, I built these into the solderless bullet connectors I was forced to use.  I did this to save space and allow flexibility in the connection of the switches.  I now think this was a mistake.  In the next version, I will probably include weak pull-up resistors on the PCB (or use PORTB, which provides for software controlled weak pull-ups)

In fact, I would re-think much of the limit switch configuration.  Certainly, limit switches are important, but not so much as I once thought.  The reality is that limit switches should **not** be tripped in normal machine operations, and there is really no way to automatically handle the case that they are tripped.  If I was starting over, I would likely include a single limit switch input pin that goes active low when tripped.  I would wire all of the switches in series to that one pin with a single pull up resistor on the PCB.  If the pin goes low, software should stop the machine and say "fix it!" to the operator.  It really should be pretty obvious what caused a limit switch to trip, so it really is not necessary to track each switch separately in hardware and software.

The final functional piece is a MAX232 equivalent line driver to convert the TTL logic level serial TX/RX from TTL to RS-232 levels to interface with my laptop.  I ended up using a Texas Instruments equivalent because they are (and I am) cheap.   With 3+ amps of power needed to drive the motors, I didn't see any need to use a low power variant of the MAX232.  Any chip with the same pinout will work here.  I actually used 10uf caps instead of the 1uf required, because, of course, that is what I had laying around.

All of the power and ground connections route to a single power connector in a .100 pitch pin header.  There are separate +RLY, -RLY, +X, +Y, and +Z connections and traces, so it is possible to drive the X, Y, and Z motors with different positive potentials.  In my case they are all +12v, because that is what the ATX power supply gives me.

The firmware was modified to match the best board design rather than forcing the board routing to match the software.  This is most evident in the connectors for the limit switches.

I started designing the board with the free version of Eagle, but soon found that it was way too limiting for board size.  I had space for a 4" x 6" board, and clearly I was going to need it.  This sent me looking for a nice free PCB design program.

I found [FreePCB](http://www.freepcb.com/), which is wonderfully featured easy to use PCB design program.  It is also open source, with an active community supporting the software and the users.

The design was completed using FreePCB, and all of the software was tested using the CAM files produced by FreePCB.

I really like this software.  Since it is Open Source, you can probably expect to see enhancements in the future that directly interface to FreePCB file formats at least, maybe even some direct links between the FreePCB design tool and my production tools.

Prior to using FreePCB, I always found that I had to draw a schematic, and did not really get the whole netlist concept.  Partially because of the ease of use of the program and partially because my understanding of electronics increased by this project, I have now found that it is easier for me to create my original designs with a netlist and a prototype board than to draw a schematic.

#### PCB Engineering Changes ####

In the first version of the board:
  * Added an additional jumper and manually drilled several of the holes in the limit switch connector area due to a mishap with some lost steps while cutting the board.
  * Added a bypass cap (.1uf) between VCC and ground close to the PIC chip on the side with the PORTB pin header.  To do this I manually drilled holes in some of the existing traces.  This solved a problem with spurious processor resets while the motors were moving.   It was not a problem on the solderless prototype board due to the high typical capacitance of those boards.
    * Added a separate isolated wire ground connection between the z-axis motor ground and the ground on the MAX232 chip.  It took me hours to figure out that the reason I could not see any output from the serial port on my terminal while the Z holding current was on.  Turns out the ground was measuring at 2.5 v at the MAX232 because the motor coils were pulling it up.  Sometimes ground is not ground!  Next time, separate power and logic grounds would be a good idea, or a two sided board with a ground plane, or optically isolate the power and logic like I did with the relays.

### Mechanical: The Physical Machine ###

http://cnc-milling-machine.googlecode.com/svn/wiki/TopView.JPG

The actual milling machine hardware is designed around Dremel tool mounted on a Z-Axis that moves up and down at a fixed position over a table surface that moves in two dimensions (X and Y).

The best single reference I found for anyone that wants to build one of these is http://www.liutaiomottola.com/Tools/CNCRouter.htm.  This page has detailed information and a 95 page document on how to build a machine very similar to the one I ended up with.  Download it and read it.  There are some great ideas in here that I am not going to repeat.

My machine is basically the one described in the above link, except:

  * My machine has a usable milling surface of 7.5 x 10.5 inches.
  * The Z axis has a total travel of about 4", but the distance between the table and the bottom of the fixed part of the z-axis is about 2".  The practical limit on the height of the piece to be milled is 2".
  * I built my own stepper motor controller because I am way to cheap to pay $200 for something I can build from my scrap box, and it was a lot more fun that way.
  * I used stepper motors from salvaged Ink Jet printers.  I took apart quite a few, so I am not sure which one I ended up using.  At some point I may upgrade these to something a bit beefier so I can get a little more speed without losing too much torque.
  * I purchased a complete Z-Axis assembly with the drive screw and stepper motor already attached from a seller on EBay.  The one I got has an aluminum body, min and max limit switches (actually a very cool optical slotted switch with a spring assembly that triggers the same limit circuit for min and max limits.  The tool mounting is on a spring platform that allows some give -- great for drilling!  This included a 12v bipolar capable stepper motor, acme drive screw, guide rails, and limit switch circuitry.  You will probably not find one of these, so buy and improvise your own, or build the one from the webpage.  The motor and drive screw combination gives me 230 steps per inch (about a .004/inch resolution)
  * I found a lot of 10 precision Acme drive screws and matching nuts, also on Ebay, for less than I could have purchased one of the drive screws.  I used  two -- one for the x axis and one for y axis.  These were 23" long, 19" of that was threaded.  This necessitated scaling up the length of the x and y axis.
    * The ends of the drive screws had different diameter steps that I used in my design to provide thrust management.  I drilled a hole and positioned the end of the drive screw through the hole so that one of the steps in the rod end was flush with the outside edge of the UHMW end cap.  Another piece of UHMW holds a brass washer against the diameter step to prevent it from moving outward.  A second brass washer and a shaft collar on the outside of the hold down UHMW piece keeps the screw from moving in the other direction, and prevents the thrust force from being transmitted to the Stepper motor on the other end of the assembly.
    * The Ebay acme nuts were also totally different -- essentially a round nut with two symmetric half-circles on opposite sides.  I used drilled holes with pins that held this into place in the routed linear slide components.  This had the additional benefit of allowing some freedom in the orientation of the screw so it need not be perfectly straight in the linear slide to prevent binding.
    * I still have 8 of the Acme drive screws if anyone is interested.  I will eventually sell them on EBay, but I can be convinced to do it sooner if anyone is interested.
  * I omitted the lateral stiffening screws on the linear slides because of lack of clearance and the belief that they are not necessary.
  * The replacement EBay z-axis is heavier than the specified z-axis, and needed to be mounted from the sides.  Instead of the single larger diameter support in the document, I used two smaller diameter black pipes for support from both sides of the z-axis.  The net result is a very secure mounting.  I used scrap UHMW pieces to fabricate a mount from the existing holes in the side of the z assembly to the pipe flange.  Both sides are held in place with drilled tapped holes in the pipe flange and 4/40 set screws.
  * The mounting for the Dremel tool was a completely different animal.
    * The bottom plate has a hole that fits the outer diameter of a screw-on adapter from my Dremel drywall routing attachment.  I mount the bottom of the Dremel tool (and change bits) by unscrewing the attachment and removing from the bottom of the hole.  This is a very stable attachment that did not require any expensive taps or machine work to create.  I made this base plate out of UHMW.
    * The top plate is pretty much what is described in the manual, with thumbscrews to allow quick and easy tool removal.  I did not have (or want to buy) a forestner bit of a proper size to fit the outside diameter of the tool, so I cut the biggest hole I could with a hole bit, then expanded it with a half round file until it fit snuggly over the tool as one piece.  Then I drilled the holes for the supports and cut it in half lengthwise.
    * The back plate of the Dremel assembly is mounted to the drive rails on the z-axis assembly.  There are channels cut so the back of the UHMW piece fits flush with the friction plate on the z-axis that keep it from rocking.  There is fine line between too much friction and too much clearance.
  * My vacuum pickup connects directly to the end of the hose from my mini shop-vac.  I just brought the vacuum hose to my local hardware store and found parts that all fit together, the shop vac plugs into a PVC glue connector that used my handy half round file to enlarge to fit.
  * I mounted my electronics in a steel four gang utility box that I found at my local Home Depot.  The motor wires enter the box from the bottom using channels routed in the mounting board as shown in the document.  The cover for the electronics box is a piece of Plexiglas so you can see the pretty  [Blinkenlights](http://en.wikipedia.org/wiki/Blinkenlights).

The linear slides are built from UHMW and storefront extrusion channel mostly as described in the above link.


## History ##

This section needs a lot of work, but the mantra is commit early, commit often!

My original plan was to create the microcode to process G-Code like a normal CNC Mill.  The Java program was just to show the machine status.  The more I worked with the G-Code the more I thought it was a lot easier to deal with in a Java driver program.

Cam.py usage.  -- too many steps -- too many chances for error -- crappy interface.

JTS -- woo hoo!


## What would I have done differently ##

  * More solid mounting of the Dremel tool/z-axis assembly.  My slop seems to be mostly in flex from this area.
    * Optical isolation of the motor power and logic power and grounds.  This would have allowed a more reasonable choice of power supplies than is available with my common ground, and eliminated practically all of the electrical problems I had to solve.

## Ideas for later ##

  * Replace the RS-232 interface with a USB interface.  Some of the newer PIC chips support USB. (I've started working with the PIC18F2550, which is pretty easy to work with)
  * Provide a (preferably wireless) Ethernet connection and basic HTTP server to permit driving it from a PC in a fixed location.  One of the OpenWRT devices could be used to do this pretty inexpensively via the current serial interface. (I've since decided it is better to have the PC nearby for calibration)
  * Add a firmware macro function for repeated patterns.
  * Add the interpolation of circular pads
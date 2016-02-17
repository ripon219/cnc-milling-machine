# Introduction #

These are the tested RJ45 adapter pinouts for the Cisco standard console port I added to the WRT54G-TM routers.

It is interesting to note that these are based on straight through cables like standard Cat5 network cables.  If you use a rolled ore reversed cable, you basically swap DTE for DCE or vice-versa because all of the complementary pairs (e.g. DSR/DTR, CTS/RTS, RX/TX) are on opposite side of the RJ45 connector.


## RJ45 to DB9F as DCE ##

This means the router is pinned as DCE - this works for accessing the router console via the serial port on a computer.

| DB9F Pin | Color | RJ45 Pin |
|:---------|:------|:---------|
| 2        | Black | 3        |
| 3        | Yellow | 6        |
| 4        | Brown | 7        |
| 5        | Red   | 4        |
| 6        | Orange | 2        |
| 7        | White | 8        |
| 8        | Blue  | 1        |
| NC       | Green | 5        |


## RJ45 to DB9M as DTE ##

This means the router is pinned as DTE - this works for using the router to control a serial device configured as DCE (like my milling machine)

| DB9M Pin | Color | RJ45 Pin |
|:---------|:------|:---------|
| 2        | Yellow | 6        |
| 3        | Black | 3        |
| 4        | Orange | 2        |
| 5        | Red   | 4        |
| 6        | Brown | 7        |
| 7        | Blue  | 1        |
| 8        | White | 8        |
| NC       | Green | 5        |


## RJ45 to DB25M as DTE ##

This means the router is pinned as DTE - this works for using the router to control a serial device configured as DCE (like my milling machine)

| DB25M Pin | Color | RJ45 Pin |
|:----------|:------|:---------|
| 2         | Black | 3        |
| 3         | Yellow | 6        |
| 4         | White | 8        |
| 5         | Blue  | 1        |
| 6         | Orange | 2        |
| 7         | Red   | 4        |
| 20        | Brown | 7        |
| NC        | Green | 5        |
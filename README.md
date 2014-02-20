Network Discovery
=================

Machines discovery/mapping (over Wifi) and port scan (over 3G/Wifi) utility for Android devices.

Features
--------

-  Discover Machines on a LAN (connect/ping discovery, dns discovery)
-  TCP Port Scanner (connect() scan)
-  NIC vendor database
-  Export results to your sdcard in XML
-  Fast access to Wifi Settings
-  Adaptive scanning rate (slow start, then adaptive to network latency)
-  Open Source, available at http://github.com/rorist/android-network-discovery

Build
-----
    git clone https://github.com/rorist/android-network-discovery.git
    cd android-network-discovery;
    cp local.properties-example local.properties
    vim local.properties #add path to the Android SDK
    ant debug install

Todo
----

- Save all scan in DB, open previous scan, export previous scan, etc
- Settings: prevent phone from sleeping
- NMAP build script (ARM and other arch (using AOSP?))
- Add new info such as Hops (using MTR?)
- Support of other protocol: UDP, SCTP
- Send custom packets (shell codes, exploits, probes, ...)
- Nat Traversal
- Proxy (auto)support

Credits
------

- Design: oblivioncreations.se
- Icons: Crystal and Oxygen projects
- German translation, bugfixes: SubOptimal
- Spanish translation: ghiki
- Chinese translation: goapk.com

GPLv2 License
-------

    Copyright (C) 2009-2011 Aubort Jean-Baptiste (Rorist)
    
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
    
Copy of the license can be found in gpl-2.0.txt


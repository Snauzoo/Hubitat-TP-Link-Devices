# Hubitat-TP-Link-Devices New Version
TP-Link Devices and Applications for the Hubitat Environment

There is only one version supporting three integrations:

a.  Kasa Account. Integration via the user's Kasa Account through the Cloud. This uses a Smart Application for installation, device communications, and device management.

b.  Node Applet. Smart Application integration via a home wifi Node.JS bridge (pc, fire tablet, android device, Raspberry Pi). The Smart Application is used (with user entry of bridge IP) to install and manage the devices.

c.  Manual Node Installation. Traditional Hub installation. Does not use a Smart Application.

SmartApplication:

a.  Supports to Kasa Account and Node.js Applet installation and management.

b.  Kasa Account:  Uses Kasa Account with your credentials to add, remove, and manage devices.

c.  Node Applet:  Uses a node.js applet on your bridge.  Smart app adds, removes and manages devices.  Additionally, Smart app monitors and updates device IPs when they change.  For the Bridge, has function to update the IP based on user entry.

d.  Manual Installation:  Same as before except you must select prefreence "installType" = "Node Applet".

Device Handlers:

a. Single device driver per device type for any of the three installation types (Kasa Account, Node Applet, manual).

b. Eliminated support for the bulb energy monitor versions. The bulb EM functions are not of great value. It does provide the useage; however, for 8 hr/day, 30 day/month, the maximum energy for an LB-130 is < 2.6 KWHr (i.e., < 40 cent (US) per month).
Installation Prequisites:

# Installation prerequisites

a. Kasa Account. (1) Kasa Account, (2) TP-Link devices in remote mode.

b. Node Applet. (1) Node.js Bridge, (2) Static IP address for Bridge (recommended for all devices).

c. Manual Node Installation. (1) Node.js Bridge, (2) Static IP addresses for the bridge and all devices.
Installation Instructions

# Installation via Smart Application

a. Install the relevant installation file(s):

b. Installation of 'Kasa Account' or 'Node Applet' integrations.

1.  Add an application per Hubitat Environment interface.

2.  'Select Installation Type'.  Tap for the selection of 'Kasa Account' or 'Node Applet'.  Once you select, the program will land on one of two pages

3.  'Kasa Account'.  Enter your Username and Password.  Once both are entered (right or wrong), you will be directet to select in "Install a Device to Continue.  That will take you to the 'Device Installation Page.  Follow prompts to install the devices.

4.  'Node Applet'. Assure the Node.js Applet is running.  Enter the device IP (example:  192.168.1.199) for your bridge.  You will see an error until the system has time to actually detect devices.  Then follow prompts to add devices.

c. Installation of Manual Node Installation

    Start the Node.js Applet. Go to the IDE, "My Devices'. Select 'New Device'.

    Fill-out the form fields 'Name', 'Label', 'Device Network ID'.

    From the pull-down 'Type', select the appropriate device type (handler). Select 'Location' (your hub).

    In 'My devices or on the phone (Classic App) for each device, select preferences. Inter InstallType, Bridge IP, and Device IP and save.

    You can now use either phone app to control the device. Prefernces can only be updated on the IDE or the classic app.

# Update

Not necessary until you need to install a new device.

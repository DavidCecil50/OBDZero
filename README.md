# OBDZero
Assists the drivers of the iMiev, CZero, and iOn electric cars

OBDZero reads, displays and stores data from the iMiev, CZero og iOn electric cars. This data such as speed and electricity use is available on the car’s CAN computer network via a Bluetooth dongle attached to the car’s OBD port. The data is presented in 9 different screens. The first screen logs messages between the app, the OBD dongle and the car.  Two screens are intended for use while driving.  One of these, the WATTS screens, shows the car’s average watts, speed and watt-hours per km.  The other screen, DRIVE, updates the distance to the next charging station, the difference between the remaining range and the distance to the station, and suggests a speed to the station. 

OBDZero can also measure the 100% capacity of the cars battery.

The app saves data in semicolon separated text files, either in the phone’s internal RAM or on an SD Card depending on how the phone is set up.

OBDZero was developed on an older phone running Android 4.3 with an INTEY OBDII an inexpensive OBD Bluetooth dongle. 

The app does not exchange data with the Internet and it does not use GPS.

There is a user manual availiable for download at OBDzero.dk or by writing to ORPEnvironment@gmail.com

I take no responsibility for any consequences of OBDZero's use.

Acknowledgements and references:
Much of the original code for OBDZero comes from Blueterm by pymasde.es.
The commands to the Bluetooth dongle were found in ELM327DSH.pdf from www.elmelectronics.com
The interpretations of the CAN PIDs for speed, voltage and current etc. were found on http://myimiev.com/forum/ posted by jjlink, garygid, priusfan, plaes, dax, cristi, and kiev.
Special thanks to Anders Fanøe and Allan Korup for their advice on electric car and CAN technology.

# hubitat-elkm1
Hubitat and Elk M1 integration via Elk M1XEP or C1M1

**Important**: If you are upgrading from drivers created by @ekimmagrann, you will need to replace all existing 
application and device drivers with these new ones.  If you are upgrading from an Elk M1 Driver with a version less than 
Version 0.2.6, you will need to replace the Elk M1 Application driver, the Elk M1 Driver, Elk M1 Driver Keypad and
Elk M1 Driver Thermostat with the new ones if you are using them.  If you are coming from a version before 0.2.0,
you will also need to replace the Elk M1 Driver Output and Elk M1 Driver Task drivers as well.  Other drivers may 
be updated or added as desired.

In any case upon updating the main Elk M1 Driver, you will then need to open the main Elk M1 device, update the settings
as desired under 'Preferences' 
and click 'Save Preferences'.

HSM and Location Mode integration was moved to the application in Elk M1 Driver 0.2.6.  If those are desired, they will
have to be configured within the app.

If you also updated the Application driver, you will then need to open the Elk M1 Application to perform some automatic
cleanup. If you had previous integrations with physical devices, you may need to add those integrations back in.  

New Installation Process:

Copy the **Elk M1 Application** code from GitHub into a 'New App' under the 'Apps Code' menu then click 'Save'

Copy the **Elk M1 Driver** code from GitHub into a 'New Driver' under the 'Drivers Code' menu then click 'Save'

Copy the **Elk M1 Driver Output** code from GitHub into a 'New Driver' under the 'Drivers Code' menu then click 'Save'

Copy the **Elk M1 Driver Task** code from GitHub into a 'New Driver' under the 'Drivers Code' menu then click 'Save'

If you plan in using Thermostats either directly attached to the Elk M1 or attached to the Hubitat but controlled by the 
Elk M1, then you will need the following driver:

Copy the **Elk M1 Driver Thermostat** code from GitHub into a 'New Driver' under the 'Drivers Code' menu then click 'Save'

The main Elk device has the functionality of one keypad.  If you plan on importing/creating additional keypad 
devices with more functionality than just temperature, you will also need the following driver:

Copy the **Elk M1 Driver Keypad** code from GitHub into a 'New Driver' under the 'Drivers Code' menu then click 'Save'
 
If you plan on importing/creating a text to speech device using the Elk's limited vocabulary, you will also need to 
load the following driver code using the same process as above:

**Elk M1 Driver Text To Speech**

If you plan on importing/creating lighting devices, you will also need the following two drivers:

**Elk M1 Driver Lighting Dimmer**

**Elk M1 Driver Lighting Switch**

If you plan on importing/creating custom or counter value devices, you will also need one or both the following two drivers:

**Elk M1 Driver Custom**

**Elk M1 Driver Counter**

Optionally, you may add the following and change your child device driver after import to use if desired:

**Elk M1 Driver Output DoorControl**

**Virtual Tamper Alert Detector**

**Virtual Water Sensor**

**Virtual Carbon Monoxide Detector**  ** Obsolete.  Hubitat Elevation now has its own system driver for this.

**Virtual Smoke Detector**  ** Obsolete.  Hubitat Elevation now has its own system driver for this.

After the app and all desired drivers are loaded into Hubitat Elevation, Go to 'Apps'

Click 'Add User App'

Select 'Elk M1 Application'.  Search for it if necessary.

Enter your information into the settings fields

Click Next

You may begin mapping your individual devices.

Click Next

Click Done

Click 'Devices'

Click the newly created Elk M1 device

Enter any remaining settings under Preferences.

Click 'Save Preferences' 

You may now change the driver type on your child devices as desired.

Enjoy your Elk M1 Integration! 

**One final note:** I have a prototype Hubitat / Elk M1 Audio Zone driver that I started for use with Audio Systems 
set up on the Elk M1 and an M1XEP.  This is a non-functioning prototype as I do not have the necessary equipment to 
experiment with it.  If you have this set up, are technical enough to attempt to finish this driver and have the desire 
to do so, please contact me.
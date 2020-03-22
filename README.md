# hubitat-elkm1
Hubitat and Elk M1 integration via Elk M1XEP or C1M1

**Important**: If you are upgrading from drivers created by @ekimmagrann, you will need to replace all existing 
application and device drivers with these new ones.  If you are upgrading from Elk M1 Driver with a version less than 
Version 0.2.0, you will need to replace the application code, the Elk M1 Driver and the Elk M1 Driver Task with the 
new ones.  Other drivers may be updated or added as desired.

In any case upon updating the main Elk M1 Driver, you will then need to open the main Elk M1 device, update the settings
as desired under 'Preferences' 
and click 'Save Preferences'.

New Installation Process:

Copy the **Elk M1 Application** code from GitHub into a 'New App' under the 'Apps Code' menu then click 'Save'

Copy the **Elk M1 Driver** code from GitHub into a 'New Driver' under the 'Drivers Code' menu then click 'Save'

Copy the **Elk M1 Driver Output** code from GitHub into a 'New Driver' under the 'Drivers Code' menu then click 'Save'

Copy the **Elk M1 Driver Task** code from GitHub into a 'New Driver' under the 'Drivers Code' menu then click 'Save'

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

Click 'Load New Apps'

Select 'Elk M1 Application' under 'User Apps'

Enter your information into the settings fields

Click Next

You may begin mapping your individual zones

Click Next

Click Done

Click 'Devices'

Click the newly created Elk M1 device

Enter any remaining settings under Preferences.

Click 'Save Preferences' 

You may now change the driver type on your child devices as desired.

Enjoy your Elk M1 Integration! 
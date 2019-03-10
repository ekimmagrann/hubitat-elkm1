/***********************************************************************************************************************
*
*  A Hubitat Child Driver using Telnet to connect to the Elk M1 via the M1XEP.
*  
*  License:
*  This program is free software: you can redistribute it and/or modify it under the terms of the GNU
*  General Public License as published by the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
*  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
*  for more details.
*
*  Name: Elk M1 Driver
*
*  A Special Thanks to Doug Beard for the framework of this driver!
*
*  I am not a programmer so alot of this work is through trial and error. I also spent a good amount of time looking 
*  at other integrations on various platforms. I know someone else was working on an Elk driver that involved an ESP
*  setup. This is a more direct route using equipment I already owned.
*
***See Release Notes at the bottom***
***********************************************************************************************************************/

public static String version()      {  return "v0.1.4"  }
public static boolean isDebug() { return true }

    
metadata {
	definition (name: "Elk M1 Driver Thermostat", namespace: "belk", author: "Mike Magrann") {
		capability "Thermostat"
//        command "RequestThermostatData"
	}
}

def RequestTemperatureData(){
 	ifDebug("requestTemperatureData()")
    def cmd = elkCommands["RequestTemperatureData"]
	prepMsg2(cmd)
}

def RequestThermostatData(){
 	ifDebug("requestTstatData()")
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
 	ifDebug("requestTstatData()" + tstat)
	getParent().RequestThermostatData(tstat)
}
//NEW CODE
def setThermostatMode(thermostatmode){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().setThermostatMode(tstat, thermostatmode)
}
def auto(){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().auto(tstat)
}
def heat(){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().heat(tstat)
}
def cool(){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().cool(tstat)
}
def off(){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().off(tstat)
}
def setThermostatFanMode(fanmode){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().setThermostatFanMode(tstat, fanmode)
}
def fanOn(){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().fanOn(tstat)
}
def fanAuto(){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().fanAuto(tstat)
}
def setHeatingSetpoint(degrees){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().setHeatingSetpoint(tstat, degrees)
}
def setCoolingSetpoint(degrees){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().setCoolingSetpoint(tstat, degrees)
}
def SetThermostatData(){
	tstat = device.deviceNetworkId
	substringCount = tstat.length()-3
	tstat = tstat.substring(substringCount).take(3)
	tstat = tstat.substring(1);
	getParent().SetThermostatData()
}
	
/***********************************************************************************************************************
*
* Release Notes (see Known Issues Below)
*
* 0.1.4
* Rewrote code to use parent telnet
*
* 0.1.3
* No longer requires a 6 digit code - Add leading zeroes to 4 digit codes
* Code clean up
* 0.1.2
* Code clean up
* 0.1.1
* New child driver to support thermostats
*
***********************************************************************************************************************/
/***********************************************************************************************************************
*
*Feature Request & Known Issues
*
* I - System configuration needs to be set up manually on the device page
* F - Transfer System configuration from Elk M1 Application
* I - Set Schedule not currently supported
*
***********************************************************************************************************************/

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

public static String version()      {  return "v0.1.5"  }
public static boolean isDebug() { return true }

    
metadata {
	definition (name: "Elk M1 Driver Thermostat", namespace: "belk", author: "Mike Magrann") {
		capability "Thermostat"
//        command "RequestThermostatData"
	}
}

def RequestThermostatData(){
 	ifDebug("requestTstatData()")
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
 	ifDebug("requestTstatData()" + tstat)
	getParent().RequestThermostatData(tstat)
}
//NEW CODE
def setThermostatMode(String thermostatmode){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().setThermostatMode(thermostatmode, tstat)
}
def auto(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().auto(tstat)
}
def heat(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().heat(tstat)
}
def EmergencyHeat(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().EmergencyHeat(tstat)
}
def cool(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().cool(tstat)
}
def off(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().off(tstat)
}
def setThermostatFanMode(fanmode){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().setThermostatFanMode(fanmode, tstat)
}
def fanOn(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().fanOn(tstat)
}
def fanAuto(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().fanAuto(tstat)
}
def fanCirculate(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().fanCirculate(tstat)
}
def setHeatingSetpoint(BigDecimal degrees){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().setHeatingSetpoint(degrees, tstat)
}
def setCoolingSetpoint(BigDecimal degrees){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().setCoolingSetpoint(degrees, tstat)
}
def SetThermostatData(){
	String tstat = device.deviceNetworkId
	tstat = tstat.substring(tstat.length() - 2).take(2)
	getParent().SetThermostatData()
}
	
 /***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.5
 * Strongly typed variables for performance
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

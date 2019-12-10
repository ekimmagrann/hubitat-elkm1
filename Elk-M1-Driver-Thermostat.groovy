/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver using Telnet to connect to the Elk M1 via the M1XEP or C1M1.
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
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.8" }

import groovy.transform.Field

metadata {
	definition(name: "Elk M1 Driver Thermostat", namespace: "belk", author: "Mike Magrann") {
		capability "Actuator"
		capability "Thermostat"
		capability "RelativeHumidityMeasurement"
		capability "Refresh"
		attribute "hold", "Boolean"
	}
	preferences {
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

def installed() {
	"Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
	refresh()
}

def uninstalled() {
}

def parse(String description) {
	String uom = description.substring(0, 2)
	String mode = elkThermostatMode[description.substring(6, 7)]
	String hold = elkThermostatHold[description.substring(7, 8)]
	String fan = elkThermostatFan[description.substring(8, 9)]
	String cTemp = description.substring(9, 11)
	String hSet = description.substring(11, 13)
	String cSet = description.substring(13, 15)
	String cHumid = description.substring(15, 17)
	String descriptionText
	if (device.currentState("coolingSetpoint")?.value == null || device.currentState("coolingSetpoint").value != cSet) {
		descriptionText = "${device.label} coolingSetpoint is ${cSet}${uom}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "coolingSetpoint", value: cSet, unit: uom, descriptionText: descriptionText)
	}
	if (device.currentState("heatingSetpoint")?.value == null || device.currentState("heatingSetpoint").value != hSet) {
		descriptionText = "${device.label} heatingSetpoint is ${hSet}${uom}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "heatingSetpoint", value: hSet, unit: uom, descriptionText: descriptionText)
	}
	if (cHumid != "00" && (device.currentState("humidity")?.value == null || device.currentState("humidity").value != cHumid)) {
		descriptionText = "${device.label} humidity is ${cHumid}%"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "humidity", value: cHumid, unit: "%", descriptionText: descriptionText)
	}
	if (device.currentState("hold")?.value == null || device.currentState("hold").value != hold) {
		descriptionText = "${device.label} hold is ${hold}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "hold", value: hold, descriptionText: descriptionText)
	}
	if (device.currentState("temperature")?.value == null || device.currentState("temperature").value != cTemp) {
		descriptionText = "${device.label} temperature is ${cTemp}${uom}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "temperature", value: cTemp, unit: uom, descriptionText: descriptionText)
	}
	if (device.currentState("thermostatFanMode")?.value == null || device.currentState("thermostatFanMode").value != fan) {
		descriptionText = "${device.label} thermostatFanMode is ${fan}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "thermostatFanMode", value: fan, descriptionText: descriptionText)
	}
	if (device.currentState("thermostatMode")?.value == null || device.currentState("thermostatMode").value != mode) {
		descriptionText = "${device.label} thermostatMode is set to ${mode}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "thermostatMode", value: mode, descriptionText: descriptionText)
	}
	if (mode == Heat || mode == EmergencyHeat) {
		if (device.currentState("thermostatSetpoint")?.value == null || device.currentState("thermostatSetpoint").value != hSet) {
			descriptionText = "${device.label} thermostatSetpoint is ${hSet}${uom}"
			sendEvent(name: "thermostatSetpoint", value: hSet, unit: uom, descriptionText: descriptionText)
		}
	} else if (mode == Cool) {
		if (device.currentState("thermostatSetpoint")?.value == null || device.currentState("thermostatSetpoint").value != cSet) {
			descriptionText = "${device.label} thermostatSetpoint is ${cSet}${uom}"
			sendEvent(name: "thermostatSetpoint", value: cSet, unit: uom, descriptionText: descriptionText)
		}
	} else {
		if (device.currentState("thermostatSetpoint")?.value == null || device.currentState("thermostatSetpoint").value != " ") {
			sendEvent(name: "thermostatSetpoint", value: " ")
		}
	}
}

def parse(List description) {
	log.warn "parse(List description) received ${description}"
	return
}

def auto() {
	parent.sendMsg(parent.setThermostatMode(Auto, getThermID()))
}

def cool() {
	parent.sendMsg(parent.setThermostatMode(Cool, getThermID()))
}

def emergencyHeat() {
	parent.sendMsg(parent.setThermostatMode(EmergencyHeat, getThermID()))
}

def fanAuto() {
	parent.sendMsg(parent.setThermostatFanMode(Auto, getThermID()))
}

def fanCirculate() {
	parent.sendMsg(parent.setThermostatFanMode(Circulate, getThermID()))
}

def fanOn() {
	parent.sendMsg(parent.setThermostatFanMode(On, getThermID()))
}

def heat() {
	parent.sendMsg(parent.setThermostatMode(Heat, getThermID()))
}

def off() {
	parent.sendMsg(parent.setThermostatMode(Off, getThermID()))
}

def setCoolingSetpoint(BigDecimal degrees) {
	parent.sendMsg(parent.setCoolingSetpoint(degrees, getThermID()))
}

def setHeatingSetpoint(BigDecimal degrees) {
	parent.sendMsg(parent.setHeatingSetpoint(degrees, getThermID()))
}

def setSchedule(schedule) {
}

def setThermostatFanMode(String fanmode) {
	parent.sendMsg(parent.setThermostatFanMode(fanmode, getThermID()))
}

def setThermostatHoldMode(String holdmode) {
	parent.sendMsg(parent.setThermostatHoldMode(holdmode, getThermID()))
}

def setThermostatMode(String thermostatmode) {
	parent.sendMsg(parent.setThermostatMode(thermostatmode, getThermID()))
}

def refresh() {
	parent.refreshThermostatStatus(getThermID())
}

String getThermID() {
	String DNID = device.deviceNetworkId
	return DNID.substring(DNID.length() - 2).take(2)
}

@Field final Map elkThermostatMode = ['0': Off, '1': Heat, '2': Cool, '3': Auto, '4': EmergencyHeat]
@Field final Map elkThermostatHold = ['0': False, '1': True]
@Field final Map elkThermostatFan = ['0': Auto, '1': On]

@Field static final String On = "on"
@Field static final String Off = "off"
@Field static final String Heat = "heat"
@Field static final String Cool = "cool"
@Field static final String Auto = "auto"
@Field static final String Circulate = "circulate"
@Field static final String EmergencyHeat = "emergency heat"
@Field static final String False = "false"
@Field static final String True = "true"
@Field static final String FanAuto = "fan auto"
@Field static final String FanOn = "fan on"

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.8
 * Added descriptionText to thermostat events
 *
 * 0.1.7
 * Fixed issue with all commands not working
 * Added humidity and hold
 * Changed logging and events to only occur when state changes
 *
 * 0.1.6
 * Added Refresh Command
 * Added info logging
 * Simplified calls to parent driver
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
 *
 * 0.1.2
 * Code clean up
 *
 * 0.1.1
 * New child driver to support thermostats
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 * I - Fan Mode Circulate currently supported
 * I - Set Hold Mode not currently supported
 * I - Set Schedule not currently supported
 *
 ***********************************************************************************************************************/

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
 *  I am not a programmer so a lot of this work is through trial and error. I also spent a good amount of time looking
 *  at other integrations on various platforms. I know someone else was working on an Elk driver that involved an ESP
 *  setup. This is a more direct route using equipment I already owned.
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.2.1" }

import groovy.transform.Field

metadata {
	definition(name: "Elk M1 Driver Thermostat", namespace: "belk", author: "Mike Magrann") {
		capability "Actuator"
		capability "Thermostat"
		capability "RelativeHumidityMeasurement"
		capability "Refresh"
		command "setThermostatHoldMode", [[name: "hold*", type: "ENUM", constraints: elkThermostatHoldIn]]
		command "setTemperature", [[name: "temperature*", description: "1 - 99", type: "NUMBER"]]
		attribute "hold", "enum", [Off, On]
	}
	preferences {
		input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: false
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

hubitat.device.HubAction installed() {
	log.warn device.label + " Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
	updated()
	refresh()
}

void uninstalled() {
}

void updated() {
	log.warn device.label + " Updated..."
	log.warn "${device.label} description logging is ${txtEnable}"
	sendEvent(name: "supportedThermostatFanModes", value: elkThermostatFanIn.values(),
			descriptionText: "${device.label} supported Fan Modes")
	sendEvent(name: "supportedThermostatModes", value: elkThermostatModeIn.values(),
			descriptionText: "${device.label} supported Modes")
}

hubitat.device.HubAction refresh() {
	String cmd = "tr" + getThermID()
	if (dbgEnable)
		log.debug device.label + " sending refresh command: " + cmd
	parent.sendMsg(cmd)
}

void parse(String description) {
	if (dbgEnable)
		log.debug device.label + " receiving thermostat message: " + description
	String uom = description.substring(0, 2)
	String mode = elkThermostatModeIn[description.substring(6, 7)]
	String hold = elkThermostatHoldIn[description.substring(7, 8)]
	String fan = elkThermostatFanIn[description.substring(8, 9)]
	int cTemp = description.substring(9, 11).toInteger()
	int hSet = description.substring(11, 13).toInteger()
	int cSet = description.substring(13, 15).toInteger()
	int cHumid = description.substring(15, 17).toInteger()
	String descriptionText
	if (cSet > 0 && (device.currentState("coolingSetpoint")?.value == null || device.currentState("coolingSetpoint").getNumberValue() != cSet)) {
		descriptionText = "${device.label} coolingSetpoint is ${cSet}${uom}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "coolingSetpoint", value: cSet, unit: uom, descriptionText: descriptionText)
	}
	if (hSet > 0 && (device.currentState("heatingSetpoint")?.value == null || device.currentState("heatingSetpoint").getNumberValue() != hSet)) {
		descriptionText = "${device.label} heatingSetpoint is ${hSet}${uom}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "heatingSetpoint", value: hSet, unit: uom, descriptionText: descriptionText)
	}
	if (cHumid > 0 && (device.currentState("humidity")?.value == null || device.currentState("humidity").getNumberValue() != cHumid)) {
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
	if (cTemp > 0 && (device.currentState("temperature")?.value == null || device.currentState("temperature").getNumberValue() != cTemp)) {
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
		if (hSet > 0 && (device.currentState("thermostatSetpoint")?.value == null ||
				device.currentState("thermostatSetpoint").getNumberValue() != hSet)) {
			descriptionText = "${device.label} thermostatSetpoint is ${hSet}${uom}"
			sendEvent(name: "thermostatSetpoint", value: hSet, unit: uom, descriptionText: descriptionText)
		}
	} else if (mode == Cool) {
		if (cSet > 0 && (device.currentState("thermostatSetpoint")?.value == null ||
				device.currentState("thermostatSetpoint").getNumberValue() != cSet)) {
			descriptionText = "${device.label} thermostatSetpoint is ${cSet}${uom}"
			sendEvent(name: "thermostatSetpoint", value: cSet, unit: uom, descriptionText: descriptionText)
		}
	} else {
		if (device.currentState("thermostatSetpoint")?.value == null || device.currentState("thermostatSetpoint").getNumberValue() != 0) {
			sendEvent(name: "thermostatSetpoint", value: 0)
		}
	}
}

void parse(List description) {
	log.warn device.label + " parse(List description) received ${description}"
}

hubitat.device.HubAction auto() {
	setThermostatData(elkThermostatCommands["Mode"], elkThermostatModeOut[Auto])
}

hubitat.device.HubAction cool() {
	setThermostatData(elkThermostatCommands["Mode"], elkThermostatModeOut[Cool])
}

hubitat.device.HubAction emergencyHeat() {
	setThermostatData(elkThermostatCommands["Mode"], elkThermostatModeOut[EmergencyHeat])
}

hubitat.device.HubAction fanAuto() {
	setThermostatData(elkThermostatCommands["Fan"], elkThermostatFanOut[Auto])
}

hubitat.device.HubAction fanCirculate() {
	setThermostatData(elkThermostatCommands["Fan"], elkThermostatFanOut[Circulate])
}

hubitat.device.HubAction fanOn() {
	setThermostatData(elkThermostatCommands["Fan"], elkThermostatFanOut[On])
}

hubitat.device.HubAction heat() {
	setThermostatData(elkThermostatCommands["Mode"], elkThermostatModeOut[Heat])
}

hubitat.device.HubAction off() {
	setThermostatData(elkThermostatCommands["Mode"], elkThermostatModeOut[Off])
}

hubitat.device.HubAction setCoolingSetpoint(BigDecimal degrees) {
	setThermostatData(elkThermostatCommands["CoolSetPoint"], degrees)
}

hubitat.device.HubAction setHeatingSetpoint(BigDecimal degrees) {
	setThermostatData(elkThermostatCommands["HeatSetPoint"], degrees)
}

void setSchedule(schedule) {
	log.warn device.label + " setSchedule is not supported."
}

hubitat.device.HubAction setThermostatFanMode(String fanmode) {
	setThermostatData(elkThermostatCommands["Fan"], elkThermostatFanOut[fanmode])
}

hubitat.device.HubAction setThermostatHoldMode(String holdmode) {
	setThermostatData(elkThermostatCommands["Hold"], elkThermostatHoldOut[holdmode])
}

hubitat.device.HubAction setThermostatMode(String thermostatmode) {
	setThermostatData(elkThermostatCommands["Mode"], elkThermostatModeOut[thermostatmode])
}

hubitat.device.HubAction setTemperature(BigDecimal degrees) {
	setThermostatData(elkThermostatCommands["CurrentTemp"], degrees)
}

hubitat.device.HubAction setThermostatData(String command, BigDecimal value) {
	setThermostatData(command, String.format("%02d", value.intValue()))
}

hubitat.device.HubAction setThermostatData(String command, String value) {
	String cmd = "ts" + getThermID() + value + command
	if (dbgEnable)
		log.debug device.label + " sending setThermostatData command: " + cmd
	parent.sendMsg(cmd)
}

String getThermID() {
	String DNID = device.deviceNetworkId
	return DNID.substring(DNID.length() - 2).take(2)
}

@Field static final String On = "on"
@Field static final String Off = "off"
@Field static final String Heat = "heat"
@Field static final String Cool = "cool"
@Field static final String Auto = "auto"
@Field static final String Circulate = "circulate"
@Field static final String EmergencyHeat = "emergency heat"
@Field static final String FanAuto = "fan auto"
@Field static final String FanOn = "fan on"

@Field final Map elkThermostatCommands = ["Mode"        : "0", "Hold": "1", "Fan": "2", "CurrentTemp": "3", "CoolSetPoint": "4",
										  "HeatSetPoint": "5"]
@Field final Map elkThermostatModeIn = ['0': Off, '1': Heat, '2': Cool, '3': Auto, '4': EmergencyHeat]
@Field final Map elkThermostatHoldIn = ['0': Off, '1': On]
@Field final Map elkThermostatFanIn = ['0': Auto, '1': On]
@Field final Map elkThermostatModeOut = ["off": "00", "heat": "01", "cool": "02", "auto": "03", "emergency heat": "04"]
@Field final Map elkThermostatHoldOut = ["off": "00", "on": "01"]
@Field final Map elkThermostatFanOut = ["auto": "00", "on": "01", "circulate": "00"]

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.2.1
 * Renamed setThermostatTemperature to setTemperature to match other Hubitat drivers.
 *
 * 0.2.0
 * Added debug logging.
 * Relocated Elk thermostat commands from parent driver to here.
 * Strongly typed commands.
 *
 * 0.1.9
 * Added commands setThermostatHoldMode & setThermostatTemperature and attributes supportedThermostatFanModes and
 *     supportedThermostatModes.
 *
 * 0.1.8
 * Added descriptionText to thermostat events.
 *
 * 0.1.7
 * Fixed issue with all commands not working.
 * Added humidity and hold.
 * Changed logging and events to only occur when state changes.
 *
 * 0.1.6
 * Added Refresh Command.
 * Added info logging.
 * Simplified calls to parent driver.
 *
 * 0.1.5
 * Strongly typed variables for performance.
 *
 * 0.1.4
 * Rewrote code to use parent telnet.
 *
 * 0.1.3
 * No longer requires a 6 digit code - Add leading zeroes to 4 digit codes.
 * Code clean up.
 *
 * 0.1.2
 * Code clean up.
 *
 * 0.1.1
 * New child driver to support thermostats.
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

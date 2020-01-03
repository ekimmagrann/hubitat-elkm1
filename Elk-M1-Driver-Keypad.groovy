/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Elk M1 Keypad.
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
 *  Name: Elk M1 Driver Keypad
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.0" }

metadata {
	definition(name: "Elk M1 Driver Keypad", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "Switch"
		capability "ContactSensor"
		capability "Initialize"
		capability "PushableButton"
		capability "TemperatureMeasurement"
		command "armAway"
		command "armStay"
		command "armStayInstant"
		command "armNight"
		command "armNightInstant"
		command "armVacation"
		command "chime"
		command "disarm"
		command "push", [[name: "button*", description: "1 - 6", type: "NUMBER"]]
		command "refresh"
		attribute "alarm", "string"
		attribute "alarmState", "string"
		attribute "armMode", "string"
		attribute "armState", "string"
		attribute "armStatus", "string"
		attribute "beep", "string"
		attribute "button1", "string"
		attribute "button2", "string"
		attribute "button3", "string"
		attribute "button4", "string"
		attribute "button5", "string"
		attribute "button6", "string"
		attribute "chime", "string"
		attribute "chimeMode", "string"
		attribute "f1LED", "string"
		attribute "f2LED", "string"
		attribute "f3LED", "string"
		attribute "f4LED", "string"
		attribute "f5LED", "string"
		attribute "f6LED", "string"
		attribute "lastUser", "string"
	}
	preferences {
		input name: "code", type: "text", title: "User code"
		input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
		input name: "txtEnable", type: "enum", title: "Enable descriptionText logging",
				options: ["none", "all", "keypad", "area"], defaultValue: "all", required: true
	}
}

def updated() {
	log.info "Updated..."
	log.warn "${device.label} description logging is: ${txtEnable == true}"
	initialize()
}

def installed() {
	"Installed..."
	device.updateSetting("dbgEnable", [type: "bool", value: false])
	device.updateSetting("txtEnable", [type: "enum", value: "keypad"])
	initialize()
	refresh()
}

def uninstalled() {
	parent.sendMsg(parent.refreshKeypadArea())
}

def initialize() {
	sendEvent(name: "numberOfButtons", value: 6)
	sendEvent(name: "button1", value: "F1")
	sendEvent(name: "button2", value: "F2")
	sendEvent(name: "button3", value: "F3")
	sendEvent(name: "button4", value: "F4")
	sendEvent(name: "button5", value: "F5")
	sendEvent(name: "button6", value: "F6")
	parent.sendMsg(parent.refreshKeypadArea())
}

def parse(String description) {
	if (txtEnable != "none" && txtEnable != "areaEvents")
		log.info "${device.label} is ${description}"
}

def parse(List statuses) {
	statuses.each {
		if (dbgEnable)
			log.debug "${device.label} parsing ${it}"
		if (it.name == "area") {
			state.area = it.value
		} else {
			if (device.currentState(it.name)?.value == null || device.currentState(it.name).value != it.value || it.name == "pushed") {
				// Creating a copy so the change below doesn't carry over to other devices.
				Map eventMap = [:]
				it.each { entry ->
					eventMap[entry.key] = entry.value
				}
				if (eventMap.descriptionText == null)
					eventMap.descriptionText = device.label + " " + eventMap.name + " was " + eventMap.value
				else
					eventMap.descriptionText = device.label + " " + eventMap.descriptionText
				if ((txtEnable == "all" || txtEnable == eventMap.type) && eventMap.name != "armState" && eventMap.name != "temperature" &&
						(eventMap.isStateChange == null || eventMap.isStateChange))
					log.info eventMap.descriptionText
				sendEvent(eventMap)
			}
		}
	}
}

int getUnitCode() {
	String DNID = device.deviceNetworkId
	return DNID.substring(DNID.length() - 2).take(2).toInteger()
}

def armAway() {
	parent.armAway(state.area ?: "1", code)
}

def armStay() {
	parent.armStay(state.area ?: "1", code)
}

def armStayInstant() {
	parent.armStayInstant(state.area ?: "1", code)
}

def armNight() {
	parent.armNight(state.area ?: "1", code)
}

def armNightInstant() {
	parent.armNightInstant(state.area ?: "1", code)
}

def armVacation() {
	parent.armVacation(state.area ?: "1", code)
}

def chime() {
	parent.chime(getUnitCode())
}

def disarm() {
	parent.disarm(state.area ?: "1", code)
}

def off() {
	parent.off(state.area ?: "1", code)
}

def on() {
	parent.on(state.area ?: "1", code)
}

def push(BigDecimal button) {
	parent.push(button, getUnitCode())
}

def refresh() {
	int keypadNo = getUnitCode()
	List cmds = []
	cmds.add(parent.sendMsg(parent.requestKeypadPress("0", keypadNo)))
	cmds.add(parent.RequestTextDescriptions("12", keypadNo))
	cmds.add(parent.RequestTextDescriptions("13", keypadNo))
	cmds.add(parent.RequestTextDescriptions("14", keypadNo))
	cmds.add(parent.RequestTextDescriptions("15", keypadNo))
	cmds.add(parent.RequestTextDescriptions("16", keypadNo))
	cmds.add(parent.RequestTextDescriptions("17", keypadNo))
	return delayBetween(cmds, 500)
}
/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.0
 * New child driver for Elk M1 Keypads
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Elk M1 Lighting Dimmers
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
 *  Name: Elk M1 Driver Lighing Dimmer
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.0" }

metadata {
	definition(name: "Elk M1 Driver Lighting Dimmer", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "Switch"
		capability "SwitchLevel"
		capability "Momentary"
		command "refresh"
	}
	preferences {
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

def updated() {
	log.info "Updated..."
	log.warn "${device.label} description logging is: ${txtEnable == true}"
}

def installed() {
	"Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
	refresh()
}

def uninstalled() {
}

String getUnitCode() {
	String DNID = device.deviceNetworkId
	return DNID.substring(DNID.length() - 3).take(3)
}

def parse(String description) {
	int level = description.toInteger()
	String switchState = level == 0 ? "off" : "on"
	if (device.currentState("switch")?.value == null || device.currentState("switch").value != switchState) {
		sendEvent(name: "switch", value: switchState)
		if (txtEnable)
			log.info "${device.label} is ${switchState}"
	}
	if (level > 1 && (device.currentState("level")?.value == null || device.currentState("level").value.toInteger() != level)) {
		sendEvent(name: "level", value: level)
		if (txtEnable)
			log.info "${device.label} was set to ${level}%"
	}
}

def parse(List description) {
	log.warn "parse(List description) received ${description}"
	return
}

def off() {
	parent.sendMsg(parent.controlLightingOff(getUnitCode()))
}

def on() {
	parent.sendMsg(parent.controlLightingOn(getUnitCode()))
}

def push() {
	parent.sendMsg(parent.controlLightingToggle(getUnitCode()))
}

def setLevel(BigDecimal level, BigDecimal duration = 0) {
	int setting = level.intValue()
	int time = duration.intValue()
	if (setting == 0)
		off()
	else {
		if (time > 9999)
			time = 9999
		if (setting == 1)
			setting = 2
		else if (setting >= 100)
			setting = 99
		parent.sendMsg(parent.controlLightingMode(getUnitCode(), "09", String.format("%02d", setting), String.format("%04d", time)))
	}
}

def refresh() {
	parent.refreshLightingStatus(getUnitCode())
}

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.0
 * New child driver to Elk M1 Lighting Dimmer
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 *
 ***********************************************************************************************************************/

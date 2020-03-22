/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Elk M1 Lighting/Appliance Switch
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
 *  Name: Elk M1 Driver Lighting Switch
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.1" }

metadata {
	definition(name: "Elk M1 Driver Lighting Switch", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "Switch"
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
	String switchState = description.toInteger() == 0 ? "off" : "on"
	if (device.currentState("switch")?.value == null || device.currentState("switch").value != switchState) {
		String descriptionText = "${device.label} is ${switchState}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "switch", value: switchState, descriptionText: descriptionText)
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

def refresh() {
	parent.refreshLightingStatus(getUnitCode())
}

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.1
 * Added descriptionText to lighting events
 *
 * 0.1.0
 * New child driver to Elk M1 Lighting Switch
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 *
 ***********************************************************************************************************************/

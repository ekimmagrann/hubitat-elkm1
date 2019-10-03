/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Elk M1 Output as a Door Control.
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
 *  Name: Elk M1 Driver Output Door Control
 *  Description: This is a driver for an output connected to a door opener that has a contact assigned to a zone
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.0" }

metadata {
	definition(name: "Elk M1 Driver Output-DoorControl", namespace: "captncode", author: "captncode") {
		capability "DoorControl"
		capability "Momentary"
		command "close"
		command "open"
		command "push"
		command "writeLog", ["string"]
		command "report", ["bool", "string"]
		attribute "door", "string"
	}
	preferences {
		input name: "transitionTime", type: "enum", title: "Transition time", required: true,
				options: [[5: "5 seconds"], [15: "15 seconds"], [30: "30 seconds"], [90: "90 seconds"]]
		input "zoneNumber", "number", title: "Door contact zone 1-208", required: true, range: "1..208"
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

def writeLog(String cmd) {
	if (txtEnable && cmd == "on") {
		log.info "${device.label} is pushed"
	}
}

def updated() {
	log.info "Updated..."
	log.warn "${device.label} description logging is: ${txtEnable == true}"
	log.warn "${device.label} door contact zone is: ${zoneNumber}"
	refresh()
}

def installed() {
	"Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
	refresh()
}

def refresh() {
	parent.unRegisterZoneReport(device.deviceNetworkId)
	parent.registerZoneReport(device.deviceNetworkId, String.format("%03d", zoneNumber.intValue()))
}

def uninstalled() {
	parent.unRegisterZoneReport(device.deviceNetworkId)
}

def report(String deviceDNID, boolean violated) {
	if (violated) {
		runIn(transitionTime.toInteger(), "setOpen")
	} else {
		state.open = false;
		String descriptionText = "door is closed"
		sendEvent(name: "door", value: "closed", descriptionText: descriptionText, isStateChange: true)
		if (txtEnable) log.info "${device.label} ${descriptionText}"
	}
}

def setOpen() {
	state.open = true;
	String descriptionText = "door is open"
	sendEvent(name: "door", value: "open", descriptionText: descriptionText, isStateChange: true)
	if (txtEnable) log.info "${device.label} ${descriptionText}"
}

def push() {
	String output = device.deviceNetworkId
	output = output.substring(output.length() - 3).take(3)
	parent.ControlOutputOn(output.toInteger(), '00001')
	if (state.open != null && state.open) {
		String descriptionText = "door is closing"
		sendEvent(name: "door", value: "closing", descriptionText: descriptionText)
		if (txtEnable) log.info "${device.label} ${descriptionText}"
	} else if (state.open == null || !state.open) {
		String descriptionText = "door is opening"
		sendEvent(name: "door", value: "opening", descriptionText: descriptionText)
		if (txtEnable) log.info "${device.label} ${descriptionText}"
	}
}

def close() {
	if (state.open == null || state.open) {
		push()
	}
}

def open() {
	if (state.open == null || !state.open) {
		push()
	}
}

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.0
 * New child driver to Elk M1 Output-DoorControl
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 *
 ***********************************************************************************************************************/

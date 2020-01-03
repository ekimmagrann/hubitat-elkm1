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

public static String version() { return "v0.1.4" }

metadata {
	definition(name: "Elk M1 Driver Output-DoorControl", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "DoorControl"
		capability "Momentary"
		command "report", ["bool", "string"]
		command "refresh"
	}
	preferences {
		input name: "transitionTime", type: "enum", title: "Transition time", required: true,
				options: [[5: "5 seconds"], [15: "15 seconds"], [30: "30 seconds"], [90: "90 seconds"]]
		input "zoneNumber", "number", title: "Door contact zone 1-208", required: true, range: "1..208"
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

def updated() {
	log.info "Updated..."
	log.warn "${device.label} description logging is: ${txtEnable == true}"
	log.warn "${device.label} door contact zone is: ${zoneNumber}"
	parent.unRegisterZoneReport(device.deviceNetworkId)
	parent.registerZoneReport(device.deviceNetworkId, String.format("%03d", zoneNumber.intValue()))
}

def installed() {
	"Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
	refresh()
}

def uninstalled() {
	parent.unRegisterZoneReport(device.deviceNetworkId)
}

def parse(String description) {
	if (txtEnable && description == "on")
		log.info "${device.label} was pushed"
}

def parse(List description) {
	log.warn "parse(List description) received ${description}"
	return
}

def report(String deviceDNID, boolean violated) {
	if (violated) {
		runIn(transitionTime.toInteger(), "setOpen")
	} else {
		state.open = false;
		if (device.currentState("door")?.value == null || device.currentState("door").value != "closed") {
			String descriptionText = "${device.label} was closed"
			if (txtEnable)
				log.info descriptionText
			sendEvent(name: "door", value: "closed", descriptionText: descriptionText)
		}
	}
}

def setOpen() {
	state.open = true;
	if (device.currentState("door")?.value == null || device.currentState("door").value != "open") {
		String descriptionText = "${device.label} was open"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "door", value: "open", descriptionText: descriptionText)
	}
}

def push() {
	String output = device.deviceNetworkId
	output = output.substring(output.length() - 3).take(3)
	parent.sendMsg(parent.ControlOutputOn(output.toInteger(), '00001'))
	if (state.open != null && state.open) {
		if (device.currentState("door")?.value == null || device.currentState("door").value != "closing") {
			String descriptionText = "${device.label} is closing"
			if (txtEnable)
				log.info descriptionText
			sendEvent(name: "door", value: "closing", descriptionText: descriptionText)
		}
	} else if (state.open == null || !state.open) {
		if (device.currentState("door")?.value == null || device.currentState("door").value != "opening") {
			String descriptionText = "${device.label} is opening"
			if (txtEnable)
				log.info descriptionText
			sendEvent(name: "door", value: "opening", descriptionText: descriptionText)
		}
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

def refresh() {
	parent.refreshZoneStatus()
}

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.4
 * Updated push to work with parent driver change
 *
 * 0.1.3
 * Minor cleanup of descriptionText on events
 *
 * 0.1.2
 * Changed logging and events to only occur when state changes
 *
 * 0.1.1
 * Added Refresh Command
 * Simplified logging and event code
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

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

public static String version() { return "v0.1.6" }

metadata {
	definition(name: "Elk M1 Driver Output-DoorControl", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "GarageDoorControl"
		capability "Momentary"
		command "report", ["bool", "string"]
		command "refresh"
	}
	preferences {
		input name: "transitionTime", type: "enum", title: "Transition time", required: true,
				options: [[5: "5 seconds"], [15: "15 seconds"], [30: "30 seconds"], [90: "90 seconds"]]
		input name: "zoneNumber", type: "number", title: "Door contact zone 1-208", required: true, range: "1..208"
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

void updated() {
	log.warn "${device.label} Updated..."
	log.warn "${device.label} description logging is: ${txtEnable}"
	log.warn "${device.label} door contact zone is: ${zoneNumber}"
	parent.unRegisterZoneReport(device.deviceNetworkId)
	parent.registerZoneReport(device.deviceNetworkId, String.format("%03d", zoneNumber.intValue()))
}

hubitat.device.HubAction installed() {
	log.warn "${device.label} Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
	refresh()
}

void uninstalled() {
	parent.unRegisterZoneReport(device.deviceNetworkId)
}

void parse(String description) {
	if (txtEnable && description == "on")
		log.info "${device.label} was pushed"
}

void parse(List description) {
	log.warn "${device.label} parse(List description) received ${description}"
}

void report(String deviceDNID, boolean violated) {
	state.contactOpen = violated
	if (violated) {
		setStatus("opening")
		runIn(transitionTime.toInteger(), "setStatus", [data: "open"])
	} else {
		setStatus("closed")
	}
}

void setStatus(String status) {
	if (status == "open" && state.contactOpen != null && !state.contactOpen) {
		status = "closed"
	} else if (status == "open") {
		state.open = true
	}
	if (status == "closed") {
		state.open = false
	}
	if (device.currentState("door")?.value == null || device.currentState("door").value != status) {
		String descriptionText = "${device.label} was ${status}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "door", value: status, descriptionText: descriptionText)
	}
}

hubitat.device.HubAction push() {
	String output = device.deviceNetworkId
	output = output.substring(output.length() - 3).take(3)
	parent.sendMsg(parent.ControlOutputOn(output.toInteger(), '00001'))
	if (state.open != null && state.open)
		setStatus("closing")
	else if (state.open == null || !state.open)
		setStatus("opening")
}

hubitat.device.HubAction close() {
	if (state.open == null || state.open) {
		push()
	}
}

hubitat.device.HubAction open() {
	if (state.open == null || !state.open) {
		push()
	}
}

hubitat.device.HubAction refresh() {
	parent.refreshZoneStatus()
}

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.6
 * Changed DoorControl capability to GarageDoorControl for compatibility with the Amazon Echo skill
 * Strongly typed commands
 *
 * 0.1.5
 * Enhanced open and closed events to for reliability
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

/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Elk M1 Tasks.
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
 *  Name: Elk M1 Driver Tasks
 *
 *  I am not a programmer so alot of this work is through trial and error. I also spent a good amount of time looking
 *  at other integrations on various platforms.
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.6" }

metadata {
	definition(name: "Elk M1 Driver Tasks", namespace: "belk", author: "Mike Magrann") {
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
	refresh()
}

def uninstalled() {
}

def parse(String description) {
	if (description == "on") {
		if (txtEnable)
			log.info "${device.label} is activated"
	}
	sendEvent(name: "switch", value: description, isStateChange: true)
}

def parse(List description) {
	log.warn "parse(List description) received ${description}"
	return
}

def push() {
	String task = device.deviceNetworkId
	task = task.substring(task.length() - 3).take(3)
	getParent().TaskActivation(task.toInteger())
}

def on() {
	push()
}

def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true)
}

def refresh() {
	off()
}

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.6
 * Added Refresh Command
 * Simplified logging and event code
 *
 * 0.1.5
 * Strongly typed variables for performance
 *
 * 0.1.4
 * Added info logging
 *
 * 0.1.3
 * Added Momentary capability
 *
 * 0.1.2
 * Changed TaskActivations to TaskActivation
 * Removed code for 'off' since this is not relevant
 *
 * 0.1.1
 * New child driver to support tasks
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 *
 ***********************************************************************************************************************/
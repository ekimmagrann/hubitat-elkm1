/***********************************************************************************************************************
 *
 *  A Virtual Carbon Monoxide Detector.
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
 *  Name: Virtual Carbon Monoxide Detector
 *
 ***********************************************************************************************************************/

public static String version() { return "v0.1.1" }

metadata {
	definition(name: "Virtual Carbon Monoxide Detector", namespace: "captncode", author: "captncode", component: true) {
		capability "Actuator"
		capability "CarbonMonoxideDetector"
		command "clear"
		command "detected"
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

def parse(String description) { log.warn "parse(String description) not implemented" }

def parse(List description) {
	description.each {
		if (it.name in ["carbonMonoxide"]) {
			if (txtEnable) log.info it.descriptionText
			sendEvent(it)
		}
	}
	return
}

def clear() {
	if (device.currentState("carbonMonoxide")?.value == null || device.currentState("carbonMonoxide").value != "clear") {
		String descriptionText = "carbonMonoxide is clear"
		if (txtEnable)
			log.info "${device.label} ${descriptionText}"
		sendEvent(name: "carbonMonoxide", value: "clear", descriptionText: descriptionText)
	}
}

def tested() {
}

def detected() {
	if (device.currentState("carbonMonoxide")?.value == null || device.currentState("carbonMonoxide").value != "detected") {
		String descriptionText = "carbonMonoxide is detected"
		if (txtEnable)
			log.info "${device.label} ${descriptionText}"
		sendEvent(name: "carbonMonoxide", value: "detected", descriptionText: descriptionText)
	}
}
/***********************************************************************************************************************
 *
 * Release Notes
 *
 * 0.1.1
 * Changed logging and events to only occur when state changes
 *
 * 0.1.0
 * New Virtual Carbon Monoxide Detector Driver
 *
 ***********************************************************************************************************************/

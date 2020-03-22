/***********************************************************************************************************************
 *
 *  A Virtual Water Sensor.
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
 *  Name: Virtual Water Sensor
 *
 ***********************************************************************************************************************/

public static String version() { return "v0.1.1" }

metadata {
	definition(name: "Virtual Water Sensor", namespace: "captncode", author: "captncode", component: true) {
		capability "Actuator"
		capability "WaterSensor"
		command "dry"
		command "wet"
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
		if (it.name in ["water"]) {
			if (txtEnable) log.info it.descriptionText
			sendEvent(it)
		}
	}
	return
}

def dry() {
	if (device.currentState("water")?.value == null || device.currentState("water").value != "dry") {
		String descriptionText = "sensor is dry"
		if (txtEnable)
			log.info "${device.label} ${descriptionText}"
		sendEvent(name: "water", value: "dry", descriptionText: descriptionText)
	}
}

def wet() {
	if (device.currentState("water")?.value == null || device.currentState("water").value != "wet") {
		String descriptionText = "sensor is wet"
		if (txtEnable)
			log.info "${device.label} ${descriptionText}"
		sendEvent(name: "water", value: "wet", descriptionText: descriptionText)
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
 * New Virtual Water Sensor Driver
 *
 ***********************************************************************************************************************/

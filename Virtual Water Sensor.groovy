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

public static String version() { return "v0.1.2" }

metadata {
	definition(name: "Virtual Water Sensor", namespace: "captncode", author: "captncode", component: false) {
		capability "WaterSensor"
		command "dry"
		command "wet"
	}
	preferences {
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

void updated() {
	log.warn device.label + " Updated..."
	log.warn "${device.label} description logging is ${txtEnable}"
}

void installed() {
	log.warn "${device.label} Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
}

void parse(String description) {
	log.warn device.label + " parse(String description) not implemented"
}

void parse(List<Map> description) {
	description.each {
		if (device.currentState(it.name)?.value == null || device.currentState(it.name).value != it.value || it.isStateChange) {
			Map eventMap = [:]
			it.each { entry ->
				eventMap[entry.key] = entry.value
			}
			if (eventMap.descriptionText == null)
				eventMap.descriptionText = device.label + " " + eventMap.name + " is " + eventMap.value
			else
				eventMap.descriptionText = device.label + " " + eventMap.descriptionText
			if (txtEnable)
				log.info eventMap.descriptionText
			sendEvent(eventMap)
		}
	}
}

void dry() {
	if (device.currentState("water")?.value == null || device.currentState("water").value != "dry") {
		String descriptionText = device.label + " sensor is dry"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "water", value: "dry", descriptionText: descriptionText)
	}
}

void wet() {
	if (device.currentState("water")?.value == null || device.currentState("water").value != "wet") {
		String descriptionText = device.label + " sensor is wet"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "water", value: "wet", descriptionText: descriptionText)
	}
}
/***********************************************************************************************************************
 *
 * Release Notes
 *
 * 0.1.2
 * Fixed error when installed.
 * Strongly typed commands.
 * Improved descriptionText in event.
 *
 * 0.1.1
 * Changed logging and events to only occur when state changes.
 *
 * 0.1.0
 * New Virtual Water Sensor Driver.
 *
 ***********************************************************************************************************************/

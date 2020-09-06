/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Elk M1 Custom
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
 *  Name: Elk M1 Driver Custom
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.1" }

metadata {
	definition(name: "Elk M1 Driver Custom", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "Refresh"
		command "setCustomTime", [[name: "hours*", description: "0 - 23", type: "NUMBER"],
								  [name: "minutes*", description: "0 - 59", type: "NUMBER"]]
		command "setCustomValue", [[name: "value*", description: "0 - 65535", type: "NUMBER"]]
		attribute "custom", "string"
		attribute "format", "enum", ["number", "time of day", "timer"]
	}
	preferences {
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

void updated() {
	log.warn device.label + " Updated..."
	log.warn "${device.label} description logging is ${txtEnable}"
}

hubitat.device.HubAction installed() {
	log.warn device.label + " Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
	refresh()
}

void uninstalled() {
}

hubitat.device.HubAction refresh() {
	parent.sendMsg(parent.refreshCustomValue(getUnitCode()))
}

int getUnitCode() {
	String DNID = device.deviceNetworkId
	return DNID.substring(DNID.length() - 2).take(2).toInteger()
}

void parse(Map description) {
	int value = description.value
	String format = description.format
	String valueStr = value.toString()
	String formatStr
	if (format == "1") {
		format = "timer"
	} else if (format == "2") {
		int hours = value / 256
		int minutes = value % 256
		valueStr = String.format("%02d", hours) + ":" + String.format("%02d", minutes)
		format = "time of day"
	} else {
		format = "number"
	}
	if (device.currentState("custom")?.value == null || device.currentState("custom").value != valueStr) {
		String descriptionText = "${device.label} is ${valueStr}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "custom", value: valueStr, descriptionText: descriptionText)
	}
	if (device.currentState("format")?.value == null || device.currentState("format").value != format) {
		String descriptionText = "${device.label} format is ${format}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "format", value: format, descriptionText: descriptionText)
	}
}

void parse(List description) {
	log.warn device.label + " parse(List description) received ${description}"
}

hubitat.device.HubAction setCustomTime(BigDecimal hours, BigDecimal minutes) {
	if (device.currentState("format")?.value == null || device.currentState("format").value == "time of day") {
		parent.sendMsg(parent.setCustomTime(getUnitCode(), hours, minutes))
	} else {
		log.warn device.label + " value must be in number format"
	}
}

hubitat.device.HubAction setCustomValue(BigDecimal value) {
	if (device.currentState("format")?.value == null || device.currentState("format").value != "time of day") {
		parent.sendMsg(parent.setCustomValue(getUnitCode(), value))
	} else {
		log.warn device.label + " value must be in time of day format"
	}
}

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.1
 * Strongly typed commands
 * Changed format to ENUM so the values can be selected in the rules engine.
 *
 * 0.1.0
 * New child driver to Elk M1 Custom
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 * I - The Elk M1 panel does not automatically report when a custom value changes.  This could cause the custom device
 *     within Hubitat to go out of sync.  The Refresh command can be used to update the value in this case.
 *
 ***********************************************************************************************************************/

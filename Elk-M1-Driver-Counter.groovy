/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Elk M1 Counter
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
 *  Name: Elk M1 Driver Counter
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.1" }

metadata {
	definition(name: "Elk M1 Driver Counter", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "Refresh"
		command "setCounterValue", [[name: "value*", description: "0 - 65535", type: "NUMBER"]]
		attribute "counter", "number"
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
	parent.sendMsg(parent.refreshCounterValue(getUnitCode()))
}

int getUnitCode() {
	String DNID = device.deviceNetworkId
	return DNID.substring(DNID.length() - 2).take(2).toInteger()
}

void parse(String description) {
	int value = description.toInteger()
	if (device.currentState("counter")?.value == null || device.currentState("counter").getNumberValue() != value) {
		String descriptionText = "${device.label} is ${value}"
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "counter", value: value, descriptionText: descriptionText)
	}
}

void parse(List description) {
	log.warn device.label + " parse(List description) received ${description}"
}

hubitat.device.HubAction setCounterValue(BigDecimal value) {
	parent.sendMsg(parent.setCounterValue(getUnitCode(), value))
}

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.1
 * Strongly typed commands
 *
 * 0.1.0
 * New child driver to Elk M1 Counter
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 * I - The Elk M1 panel does not automatically report when a counter value changes.  This could cause the counter device
 *     within Hubitat to go out of sync.  The Refresh command can be used to update the value in this case.
 *
 ***********************************************************************************************************************/

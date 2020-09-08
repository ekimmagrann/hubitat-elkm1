/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Elk M1 Keypad.
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
 *  Name: Elk M1 Driver Keypad
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.1" }

import groovy.transform.Field

metadata {
	definition(name: "Elk M1 Driver Keypad", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "ContactSensor"
		capability "Initialize"
		capability "Lock"
		capability "PushableButton"
		capability "TemperatureMeasurement"
		command "armAway"
		command "armHome"
		command "armHomeInstant"
		command "armNight"
		command "armNightInstant"
		command "armVacation"
		command "chime"
		command "disarm"
		command "push", [[name: "button*", description: "1 - 6", type: "NUMBER"]]
		command "refresh"
		command "showTextOnKeypads", [[name: "line1", description: "Max 16 characters", type: "STRING"],
									  [name: "line2", description: "Max 16 characters", type: "STRING"],
									  [name: "timeout*", description: "0 - 65535", type: "NUMBER"],
									  [name: "beep", type: "ENUM", constraints: ["no", "yes"]]]
		command "zoneBypass", [[name: "zone*", description: "1 - 208, 0 = Unbypass all", type: "NUMBER"]]
		attribute "alarm", "enum", [Clear, Detected]
		attribute "alarmState", "string"
		attribute "armingIn", "number"
		attribute "armState", "enum", [NotReadytoArm, ReadytoArm, ReadytoArmBut, ArmedwithExit, ArmedFully, ForceArmed, ArmedwithaBypass]
		attribute "armStatus", "enum", [Disarmed, ArmedAway, ArmedHome, ArmedHomeInstant, ArmedNight, ArmedNightInstant, ArmedVacation,
										ArmingAway, ArmingHome, ArmingHomeInstant, ArmingNight, ArmingNightInstant, ArmingVacation]
		attribute "beep", "enum", [Off, Beeped, Beeping]
		attribute "button1", "string"
		attribute "button2", "string"
		attribute "button3", "string"
		attribute "button4", "string"
		attribute "button5", "string"
		attribute "button6", "string"
		attribute "chime", "enum", [Off, Chimed]
		attribute "chimeMode", "enum", [Off, Tone, Voice, ToneVoice]
		attribute "f1LED", "enum", [Off, On, Blinking]
		attribute "f2LED", "enum", [Off, On, Blinking]
		attribute "f3LED", "enum", [Off, On, Blinking]
		attribute "f4LED", "enum", [Off, On, Blinking]
		attribute "f5LED", "enum", [Off, On, Blinking]
		attribute "f6LED", "enum", [Off, On, Blinking]
		attribute "invalidUser", "string"
		attribute "lastUser", "string"
		attribute "securityKeypad", "string"
	}
	preferences {
		input name: "code", type: "text", title: "User code"
		input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
		input name: "txtEnable", type: "enum", title: "Enable descriptionText logging",
				options: ["none", "all", "keypad", "area"], defaultValue: "all", required: true
	}
}

hubitat.device.HubAction updated() {
	log.warn "Updated..."
	log.warn "${device.label} description logging is ${txtEnable}"
	initialize()
}

List<hubitat.device.HubAction> installed() {
	log.warn "Installed..."
	device.updateSetting("dbgEnable", [type: "bool", value: false])
	device.updateSetting("txtEnable", [type: "enum", value: "keypad"])
	List<hubitat.device.HubAction> cmds = [initialize()]
	cmds.addAll(refresh())
}

hubitat.device.HubAction uninstalled() {
	parent.sendMsg(parent.refreshKeypadArea())
}

hubitat.device.HubAction initialize() {
	sendEvent(name: "numberOfButtons", value: 6, type: "keypad", descriptionText: "${device.label} numberOfButtons default value set")
	sendEvent(name: "button1", value: "F1", type: "keypad", descriptionText: "${device.label} button1 default value set")
	sendEvent(name: "button2", value: "F2", type: "keypad", descriptionText: "${device.label} button2 default value set")
	sendEvent(name: "button3", value: "F3", type: "keypad", descriptionText: "${device.label} button3 default value set")
	sendEvent(name: "button4", value: "F4", type: "keypad", descriptionText: "${device.label} button4 default value set")
	sendEvent(name: "button5", value: "F5", type: "keypad", descriptionText: "${device.label} button5 default value set")
	sendEvent(name: "button6", value: "F6", type: "keypad", descriptionText: "${device.label} button6 default value set")
	parent.sendMsg(parent.refreshKeypadArea())
}

List<hubitat.device.HubAction> refresh() {
	int keypadNo = getUnitCode()
	List<hubitat.device.HubAction> cmds = []
	cmds.add(parent.sendMsg(parent.requestKeypadPress("0", keypadNo)))
	cmds.add(parent.sendMsg(parent.requestKeypadStatus(keypadNo)))
	cmds.add(parent.sendMsg(parent.RequestTextDescriptions("12", keypadNo)))
	cmds.add(parent.sendMsg(parent.RequestTextDescriptions("13", keypadNo)))
	cmds.add(parent.sendMsg(parent.RequestTextDescriptions("14", keypadNo)))
	cmds.add(parent.sendMsg(parent.RequestTextDescriptions("15", keypadNo)))
	cmds.add(parent.sendMsg(parent.RequestTextDescriptions("16", keypadNo)))
	cmds.add(parent.sendMsg(parent.RequestTextDescriptions("17", keypadNo)))
	return delayBetween(cmds, 500)
}

void parse(String description) {
	if (txtEnable != "none" && txtEnable != "areaEvents")
		log.info "${device.label} is ${description}"
}

void parse(List<Map> statuses) {
	statuses.each {
		if (dbgEnable)
			log.debug "${device.label} parsing ${it}"
		if (it.name == "area") {
			state.area = it.value
		} else {
			if (it.name == "pushed" || it.isStateChange ||
					device.currentState(it.name)?.value == null || device.currentState(it.name).value != it.value.toString()) {
				// Creating a copy so the change below doesn't carry over to other devices.
				Map eventMap = [:]
				it.each { entry ->
					eventMap[entry.key] = entry.value
				}
				if (eventMap.descriptionText == null)
					eventMap.descriptionText = device.label + " " + eventMap.name + " was " + eventMap.value
				else
					eventMap.descriptionText = device.label + " " + eventMap.descriptionText
				if ((txtEnable == "all" || txtEnable == eventMap.type) && eventMap.name != "armState" && eventMap.name != "contact" &&
						eventMap.name != "lock" && eventMap.name != "securityKeypad" && eventMap.name != "temperature")
					log.info eventMap.descriptionText
				sendEvent(eventMap)
			}
		}
	}
}

int getUnitCode() {
	String DNID = device.deviceNetworkId
	return DNID.substring(DNID.length() - 2).take(2).toInteger()
}

hubitat.device.HubAction armAway() {
	parent.armAway(state.area ?: 1, code)
}

hubitat.device.HubAction armHome() {
	parent.armHome(state.area ?: 1, code)
}

hubitat.device.HubAction armHomeInstant() {
	parent.armHomeInstant(state.area ?: 1, code)
}

hubitat.device.HubAction armNight() {
	parent.armNight(state.area ?: 1, code)
}

hubitat.device.HubAction armNightInstant() {
	parent.armNightInstant(state.area ?: 1, code)
}

hubitat.device.HubAction armVacation() {
	parent.armVacation(state.area ?: 1, code)
}

hubitat.device.HubAction chime() {
	parent.chime(getUnitCode())
}

hubitat.device.HubAction disarm() {
	parent.disarm(state.area ?: 1, code)
}

hubitat.device.HubAction showTextOnKeypads(BigDecimal time, String beep) {
	showTextOnKeypads("", "", time, beep, sysArea)
}

hubitat.device.HubAction showTextOnKeypads(String line1, BigDecimal time, String beep) {
	showTextOnKeypads(line1, "", time, beep, sysArea)
}

hubitat.device.HubAction showTextOnKeypads(String line1 = "", String line2 = "", BigDecimal time = 0, String beep = "no") {
	parent.showTextOnKeypads(line1, line2, time, beep, state.area ?: 1)
}

hubitat.device.HubAction lock() {
	parent.lock(state.area ?: 1, code)
}

hubitat.device.HubAction push(BigDecimal button) {
	parent.push(button, getUnitCode())
}

hubitat.device.HubAction unlock() {
	parent.unlock(state.area ?: 1, code)
}

hubitat.device.HubAction zoneBypass(BigDecimal zoneNumber = 0) {
	parent.zoneBypass(zoneNumber, state.area ?: 1, code)
}

// Key Mapping Readable Text
@Field static final String Off = "off"
@Field static final String On = "on"
@Field static final String Clear = "clear"
@Field static final String Detected = "detected"
@Field static final String Blinking = "blinking"
@Field static final String Chimed = "chimed"
@Field static final String Beeped = "beeped"
@Field static final String Beeping = "beeping"
@Field static final String Tone = "tone"
@Field static final String Voice = "voice"
@Field static final String ToneVoice = "tone/voice"
@Field static final String NotReadytoArm = "Not Ready to Arm"
@Field static final String ReadytoArm = "Ready to Arm"
@Field static final String ReadytoArmBut = "Ready to Arm, but a zone is violated and can be force armed"
@Field static final String ArmedwithExit = "Armed with Exit Timer working"
@Field static final String ArmedFully = "Armed Fully"
@Field static final String ForceArmed = "Force Armed with a force arm zone violated"
@Field static final String ArmedwithaBypass = "Armed with a Bypass"
@Field static final String Disarmed = "disarmed"
@Field static final String ArmedAway = "armed away"
@Field static final String ArmedHome = "armed home"
@Field static final String ArmedHomeInstant = "armed home instant"
@Field static final String ArmedNight = "armed night"
@Field static final String ArmedNightInstant = "armed night instant"
@Field static final String ArmedVacation = "armed vacation"
@Field static final String ArmingAway = "arming away"
@Field static final String ArmingHome = "arming home"
@Field static final String ArmingHomeInstant = "arming home instant"
@Field static final String ArmingNight = "arming night"
@Field static final String ArmingNightInstant = "arming night instant"
@Field static final String ArmingVacation = "arming vacation"

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.1
 * Changed alarm attribute from on/off to detected/clear.
 * Changed alarm, armState, armStatus, beep, chime, chimeMode and the Function key LED attributes to ENUM so
 *       the values can be selected in the rules engine.
 * Changed Switch Capability to Lock Capability for Dashboard arming/disarming and for the Amazon Alexa skill.
 * Renamed Stay mode to Home mode to align with Hubitat terminology.
 * Added securityKeypad attribute for SecurityKeypad Capability of parent driver.
 * Added armingIn attribute to show exit delay.
 * Added missing invalidUser attribute.
 * Added showTextOnKeypads command.
 * Added zoneBypass command.
 * Removed armMode attribute.  Between securityKeypad and armStatus, it should be unnecessary.
 *
 * 0.1.0
 * New child driver for Elk M1 Keypads.
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 *
 ***********************************************************************************************************************/
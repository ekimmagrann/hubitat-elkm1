/***********************************************************************************************************************
 *
 *  A Hubitat Driver using Telnet on the local network to connect to the Elk M1 via the M1XEP or C1M1
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
 *  Name: Elk M1 Driver

 *  A Special Thanks to Doug Beard for the framework of this driver!
 *
 *  I am not a programmer so a lot of this work is through trial and error. I also spent a good amount of time looking
 *  at other integrations on various platforms. I know someone else was working on an Elk driver that involved an ESP
 *  setup. This is a more direct route using equipment I already owned.
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.2.6" }

import groovy.transform.Field

metadata {
	definition(name: "Elk M1 Driver", namespace: "belk", author: "Mike Magrann") {
		capability "Actuator"
		capability "ContactSensor"
		capability "Initialize"
		capability "PushableButton"
		capability "SecurityKeypad"
		capability "Telnet"
		//capability "TemperatureMeasurement"
		capability "Lock"
		command "armHomeInstant"
		command "armNight"
		command "armNightInstant"
		command "armVacation"
		command "chime"
		command "push", [[name: "button*", description: "1 - 6", type: "NUMBER"]]
		command "refreshArmStatus"
		command "refreshCounterValues"
		command "refreshCustomValues"
//		command "refreshKeypadArea"
		command "refreshLightingStatus"
		command "refreshOutputStatus"
		command "refreshTemperatureStatus"
		command "refreshTroubleStatus"
		command "refreshZoneStatus"
		command "requestZoneDefinitions"
		command "requestZoneVoltage", [[name: "zone*", description: "1 - 208", type: "NUMBER"]]
		command "sendMsg"
		command "showTextOnKeypads", [[name: "line1", description: "Max 16 characters", type: "STRING"],
									  [name: "line2", description: "Max 16 characters", type: "STRING"],
									  [name: "timeout*", description: "0 - 65535", type: "NUMBER"],
									  [name: "beep", type: "ENUM", constraints: ["no", "yes"]]]
		command "speakPhrase", [[name: "phraseNumber*", description: "1 - 319", type: "NUMBER"]]
		command "speakWord", [[name: "wordNumber*", description: "1 - 473", type: "NUMBER"]]
		command "zoneBypass", [[name: "zone*", description: "1 - 208, 0 = Unbypass all", type: "NUMBER"]]
		command "zoneTrigger", [[name: "zone*", description: "1 - 208", type: "NUMBER"]]
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
		attribute "trouble", "enum", [Clear, Detected]
	}
	preferences {
		input name: "ip", type: "text", title: "IP Address", required: true
		input name: "port", type: "number", title: "Port", range: 1..65535, required: true, defaultValue: 2101
		input name: "keypad", type: "number", title: "Keypad", description: "0 = Do not connect", range: 0..16, required: true, defaultValue: 1
		input name: "code", type: "text", title: "User code"
		input name: "timeout", type: "number", title: "Timeout in minutes", range: 0..1999, defaultValue: 0
		input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: false
		input name: "txtEnable", type: "enum", title: "Enable event logging for system +",
				options: ["none", "all", "keypad", "area"], defaultValue: "all", required: true
		input name: "lockArm", type: "enum", title: "Lock to arm mode",
				options: ["none", "away", "home", "homeInstant", "night", "nightInstant", "vacation",
						  "nextAway", "nextHome", "forceAway", "forceHome"], defaultValue: "away", required: true
		input name: "lockDisarm", type: "bool", title: "Allow lock to disarm", defaultValue: true
	}
}

//general handlers
List<hubitat.device.HubAction> installed() {
	log.warn "${device.label} installed..."
	initialize()
}

List<hubitat.device.HubAction> updated() {
	log.info "${device.label} Updated..."
	if (dbgEnable)
		log.debug "${device.label}: Configuring IP: ${ip}, Port ${port}, Keypad ${keypad}, Code: ${code != ""}, Timeout: ${timeout}"
	sendEvent(name: "numberOfButtons", value: 6, type: "keypad", descriptionText: "${device.label} numberOfButtons default value set")
	sendEvent(name: "button1", value: "F1", type: "keypad", descriptionText: "${device.label} button1 default value set")
	sendEvent(name: "button2", value: "F2", type: "keypad", descriptionText: "${device.label} button2 default value set")
	sendEvent(name: "button3", value: "F3", type: "keypad", descriptionText: "${device.label} button3 default value set")
	sendEvent(name: "button4", value: "F4", type: "keypad", descriptionText: "${device.label} button4 default value set")
	sendEvent(name: "button5", value: "F5", type: "keypad", descriptionText: "${device.label} button5 default value set")
	sendEvent(name: "button6", value: "F6", type: "keypad", descriptionText: "${device.label} button6 default value set")
	sendEvent(name: "maxCodes", value: 199, type: "system", descriptionText: "${device.label} maxCodes default value set")
	initialize()
}

List<hubitat.device.HubAction> initialize() {
	List<hubitat.device.HubAction> commandList = null
	if (state.alarmState != null) state.remove("alarmState")
	if (state.armState != null) state.remove("armState")
	if (state.armStatus != null) state.remove("armStatus")
	state.remove("creatingZone")
	device.removeSetting("locationSet")
	device.removeSetting("switchFully")
	device.removeSetting("tempCelsius")
	if (state.entryExit == null)
		state.entryExit = new long[8]
	if (port == null)
		device.updateSetting("port", [type: "number", value: 2101])
	if (keypad == null)
		device.updateSetting("keypad", [type: "number", value: 1])
	if (timeout == null)
		device.updateSetting("timeout", [type: "number", value: 0])
	if (dbgEnable == null)
		device.updateSetting("dbgEnable", [type: "bool", value: "false"])
	if (txtEnable == null)
		device.updateSetting("txtEnable", [type: "text", value: "all"])
	if (lockArm == null)
		device.updateSetting("lockArm", [type: "text", value: "away"])
	if (lockDisarm == null)
		device.updateSetting("lockDisarm", [type: "bool", value: "true"])
	telnetClose()
	if (keypad == 0) {
		log.warn "${device.label} is disconnected.  Change your keypad in Preferences to reconnect."
	} else {
		boolean success = true
		try {
			//open telnet connection
			telnetConnect([termChars: [13, 10]], ip, port.toInteger(), null, null)
			//give it a chance to start
			pauseExecution(1000)
			if (dbgEnable)
				log.debug "${device.label}: Telnet connection to Elk M1 established"
		} catch (e) {
			log.warn "${device.label}: initialize error: ${e.message}"
			success = false
		}
		if (success)
			commandList = refresh()
		heartbeat() // Start checking for telnet timeout
		return commandList
	}
}

List<hubitat.device.HubAction> refresh() {
	List<hubitat.device.HubAction> cmds = []
	cmds.add(refreshKeypadArea())
	cmds.add(refreshUserArea())
	cmds.add(refreshVersionNumber())
	runIn(2, refreshSmart)
	runIn(12, refreshElk)
	return delayBetween(cmds, 300)
}

void refreshSmart() {
	parent.smartRefresh()
}

void refreshElk() {
	List<String> cmds = []
	cmds.add((String) refreshTemperatureStatus())
	cmds.add((String) refreshArmStatus())
	cmds.add((String) refreshOutputStatus())
	refreshLightingStatus(false).each { cmds.add((String) it) }
	cmds.add((String) refreshTroubleStatus())
	cmds.add((String) refreshAlarmStatus())
	cmds.add((String) requestKeypadStatus())
	cmds.add((String) requestKeypadPress())
	refreshCounterValues(false).each { cmds.add((String) it) }
	sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, 1000), hubitat.device.Protocol.TELNET))
	pauseExecution(1000)
	cmds = []
	cmds.add((String) refreshCustomValues())
	cmds.add((String) RequestTextDescriptions("12", keypad.toInteger()))
	cmds.add((String) RequestTextDescriptions("13", keypad.toInteger()))
	cmds.add((String) RequestTextDescriptions("14", keypad.toInteger()))
	cmds.add((String) RequestTextDescriptions("15", keypad.toInteger()))
	cmds.add((String) RequestTextDescriptions("16", keypad.toInteger()))
	cmds.add((String) RequestTextDescriptions("17", keypad.toInteger()))
	cmds.add((String) refreshZoneStatus())
	sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, 1000), hubitat.device.Protocol.TELNET))
}

void uninstalled() {
	telnetClose()
	getChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

//Elk M1 Command Line Request - Start of
hubitat.device.HubAction unlock() {
	if (lockDisarm)
		disarm()
}

hubitat.device.HubAction lock(int sysArea = state.area, String sysCode = code) {
	switch (lockArm) {
		case "away":
			armAway(sysArea, sysCode)
			break
		case "home":
			armHome(sysArea, sysCode)
			break
		case "homeInstant":
			armHomeInstant(sysArea, sysCode)
			break
		case "night":
			armNight(sysArea, sysCode)
			break
		case "nightInstant":
			armNightInstant(sysArea, sysCode)
			break
		case "vacation":
			armVacation(sysArea, sysCode)
			break
		case "nextAway":
			ArmNextAway(sysArea, sysCode)
			break
		case "nextHome":
			ArmNextHome(sysArea, sysCode)
			break
		case "forceAway":
			ArmForceAway(sysArea, sysCode)
			break
		case "forceHome":
			ArmForceHome(sysArea, sysCode)
			break
	}
}

hubitat.device.HubAction disarm(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} disarm"
	String cmd = elkCommands["Disarm"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armAway(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armAway"
	String cmd = elkCommands["ArmAway"]
	if (!isArmed(sysArea))
		sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armHome(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armHome"
	String cmd = elkCommands["ArmHome"]
	if (!isArmed(sysArea))
		sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armHomeInstant(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armHomeInstant"
	String cmd = elkCommands["ArmHomeInstant"]
	if (!isArmed(sysArea))
		sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armNight(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armNight"
	String cmd = elkCommands["ArmNight"]
	if (!isArmed(sysArea))
		sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armNightInstant(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armNightInstant"
	String cmd = elkCommands["ArmNightInstant"]
	if (!isArmed(sysArea))
		sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armVacation(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armVacation"
	String cmd = elkCommands["ArmVacation"]
	if (!isArmed(sysArea))
		sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction ArmNextAway(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} ArmNextAway"
	String cmd = elkCommands["ArmNextAway"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction ArmNextHome(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} ArmNextHome"
	String cmd = elkCommands["ArmNextHome"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction ArmForceAway(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} ArmForceAway"
	String cmd = elkCommands["ArmForceAway"]
	if (!isArmed(sysArea))
		sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction ArmForceHome(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} ArmForceHome"
	String cmd = elkCommands["ArmForceHome"]
	if (!isArmed(sysArea))
		sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction getCodes() {
	if (dbgEnable)
		log.debug "${device.label} getCodes started"
	state.remove("userList")
	state.requestedChange = "{}"
	state.creatingDevice = "02"
	RequestTextDescriptions(state.creatingDevice, 1)
}

hubitat.device.HubAction deleteCode(BigDecimal codeposition, String sysCode = code) {
	setCode(codeposition, null, null, sysCode)
}

hubitat.device.HubAction setCode(BigDecimal codeposition, String pincode, String name = null, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} code ${codeposition} RequestUserChange"
	String reason = null
	if (state.userType == "User")
		reason = "User not allowed to set user codes."
	else if (device.currentState("codeLength")?.value == null)
		reason = "codeLength is not known.  Please save preferences, wait 30 seconds and try again."
	else if (pincode != null && (pincode.length() != device.currentState("codeLength").value.toInteger() || !pincode.isInteger() ||
			pincode.toInteger() < 0 || pincode != pincode.trim()))
		reason = "PIN code must be numeric and " + device.currentState("codeLength").value + " digits."
	else if (pincode != null && pincode.padLeft(6, '0').take(6) == "000000")
		reason = "PIN code must not be zero."
	if (reason != null) {
		log.warn device.label + " " + reason
		sendEvent(name: "codeChanged", value: "failed", type: "system", descriptionText: device.label + " codeChanged for " + codeposition +
				" (" + name + ") was failed - " + reason, isStateChange: true)
		return null
	}

	String oldCode = sysCode.padLeft(6, '0').take(6)
	String newCode = (pincode ?: "0").padLeft(6, '0').take(6)
	if (state.requestedChange == null)
		state.requestedChange = "{}"
	String extra
	Map<Map> pendingList = new groovy.json.JsonSlurper().parseText(state.requestedChange)
	if (newCode == "000000") {
		pendingList[codeposition.toString()] = [code: pincode, name: name, status: "D"]
		extra = "10"
	} else {
		pendingList[codeposition.toString()] = [code: pincode, name: name, status: "A"]
		extra = "00"
		if (name != null && name.length() > 0)
			log.warn device.label + " does not support user name updates.  The name will not be updated on the panel."
	}
	state.requestedChange = new groovy.json.JsonBuilder(pendingList).toString()
	oldCode = "0" + oldCode.substring(0, 1) + "0" + oldCode.substring(1, 2) + "0" + oldCode.substring(2, 3) + "0" +
			oldCode.substring(3, 4) + "0" + oldCode.substring(4, 5) + "0" + oldCode.substring(5, 6)
	newCode = "0" + newCode.substring(0, 1) + "0" + newCode.substring(1, 2) + "0" + newCode.substring(2, 3) + "0" +
			newCode.substring(3, 4) + "0" + newCode.substring(4, 5) + "0" + newCode.substring(5, 6)
	String cmd = elkCommands["RequestUserChange"] + String.format("%03d", codeposition.toInteger()) + oldCode + newCode + state.userAreas
	sendMsg(cmd, extra)
}

hubitat.device.HubAction setCodeLength(BigDecimal pincodelength) {
	log.warn "${device.label} does not support setCodeLength."
}

hubitat.device.HubAction setEntryDelay(BigDecimal entrancedelay) {
	log.warn "${device.label} does not support setEntryDelay."
}

hubitat.device.HubAction setExitDelay(BigDecimal exitdelay) {
	log.warn "${device.label} does not support setExitDelay."
}

hubitat.device.HubAction refreshUserArea(String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} refreshUserArea"
	String cmd = elkCommands["RequestUserArea"] + sysCode.padLeft(6, '0').take(6)
	sendMsg(cmd)
}

hubitat.device.HubAction refreshVersionNumber() {
	if (dbgEnable)
		log.debug "${device.label} refreshVersionNumber"
	String cmd = elkCommands["RequestVersionNumber"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshArmStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshArmStatus"
	String cmd = elkCommands["RequestArmStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshTroubleStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshTroubleStatus"
	String cmd = elkCommands["RequestTroubleStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshAlarmStatus() {
	if (dbgEnable)
		log.debug "${device.label} requestAlarmStatus"
	String cmd = elkCommands["RequestAlarmStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshTemperatureStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshTemperatureStatus"
	String cmd = elkCommands["RequestTemperatureData"]
	sendMsg(cmd)
}

hubitat.device.HubAction RequestTextDescriptions(String deviceType, int deviceNumber) {
	if (dbgEnable)
		log.debug "${device.label} RequestTextDescriptions Type: ${deviceType} Device: ${deviceNumber}"
	String cmd = elkCommands["RequestTextDescriptions"] + deviceType + String.format("%03d", deviceNumber)
	sendMsg(cmd)
}

void startCreatingDevice(String deviceType) {
	state.creatingDevice = deviceType
	sendHubCommand(RequestTextDescriptions(deviceType, 1))
}

void stopCreatingDevice() {
	if (state.creatingDevice == "02" && state.userList != null) {
		TreeMap userList = state.userList
		TreeMap<Map> lockCodes = [:]
		TreeMap<Map> oldCodes = [:]
		Map oldUser
		String key
		String blankCode = "0000"
		if (device.currentState("codeLength")?.value != null)
			blankCode = blankCode.padRight(device.currentState("codeLength").value.toInteger(), '0')
		if (device.currentState("lockCodes")?.value != null)
			oldCodes = new groovy.json.JsonSlurper().parseText(decrypt(device.currentState("lockCodes").value))
		userList.each { userNumber, userText ->
			key = userNumber.toInteger().toString()
			oldUser = oldCodes[key]
			if ((userText != "" && userText != "USER " + userNumber) || (oldUser != null && !(oldUser.name in [null, ""])))
				lockCodes[key] = oldUser == null ? [name: userText, code: blankCode] : oldUser
		}
		String value = encrypt(new groovy.json.JsonBuilder(lockCodes).toString())
		log.info device.label + " getCodes complete. " + lockCodes.size().toString() + " codes found."
		sendEvent(name: "lockCodes", value: value, type: "system", descriptionText: device.label + " lockCodes updated. " +
				lockCodes.size().toString() + " codes found by getCodes.")
		if (device.currentState("lockCodes")?.value != null && device.currentState("lockCodes").value != value)
			log.info device.label + " lockCodes changed to " + value
		state.remove("userList")
	}
	state.remove("creatingDevice")
}

hubitat.device.HubAction ControlOutputOn(BigDecimal output = 0, String time = "0") {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputOn output ${output}"
	String cmd = elkCommands["ControlOutputOn"]
	cmd = cmd + String.format("%03d", output.intValue()) + time.padLeft(5, '0')
	sendMsg(cmd)
}

hubitat.device.HubAction ControlOutputOff(BigDecimal output = 0) {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputOff output ${output}"
	String cmd = elkCommands["ControlOutputOff"]
	cmd = cmd + String.format("%03d", output.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction ControlOutputToggle(BigDecimal output = 0) {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputToggle output ${output}"
	String cmd = elkCommands["ControlOutputToggle"]
	cmd = cmd + String.format("%03d", output.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction TaskActivation(BigDecimal task = 0) {
	if (dbgEnable)
		log.debug "${device.label} TaskActivation task: ${task}"
	String cmd = elkCommands["TaskActivation"]
	cmd = cmd + String.format("%03d", task.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction speakPhrase(BigDecimal phraseNumber = 0) {
	if (phraseNumber > 0 && phraseNumber < 320) {
		if (dbgEnable)
			log.debug "${device.label} speakPhrase ${phraseNumber}"
		String cmd = elkCommands["SpeakPhrase"] + String.format("%03d", phraseNumber.toInteger())
		sendMsg(cmd)
	}
}

hubitat.device.HubAction speakWord(BigDecimal wordNumber = 0) {
	if (wordNumber > 0 && wordNumber < 474) {
		if (dbgEnable)
			log.debug "${device.label} speakWord ${wordNumber}"
		String cmd = elkCommands["SpeakWord"] + String.format("%03d", wordNumber.toInteger())
		sendMsg(cmd)
	}
}

hubitat.device.HubAction setCounterValue(BigDecimal counter, BigDecimal value) {
	if (counter >= 1 && counter <= 64 && value >= 0 && value <= 65535) {
		if (dbgEnable)
			log.debug "${device.label} setCounterValue counter: ${counter} value ${value}"
		String cmd = elkCommands["SetCounterValue"] + String.format("%02d", counter.intValue()) + String.format("%05d", value.intValue())
		sendMsg(cmd)
	}
}

hubitat.device.HubAction setCustomTime(BigDecimal custom, BigDecimal hours, BigDecimal minutes) {
	BigDecimal value = hours * 256 + minutes
	setCustomValue(custom, value)
}

hubitat.device.HubAction setCustomValue(BigDecimal custom, BigDecimal value) {
	if (custom >= 1 && custom <= 20 && value >= 0 && value <= 65535) {
		if (dbgEnable)
			log.debug "${device.label} setCustomValue custom: ${custom} value ${value}"
		String cmd = elkCommands["SetCustomValue"] + String.format("%02d", custom.intValue()) + String.format("%05d", value.intValue())
		sendMsg(cmd)
	}
}

hubitat.device.HubAction requestZoneDefinitions() {
	if (dbgEnable)
		log.debug "${device.label} requestZoneDefinitions"
	String cmd = elkCommands["RequestZoneDefinitions"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshZoneStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshZoneStatus"
	String cmd = elkCommands["RequestZoneStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshOutputStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshOutputStatus"
	String cmd = elkCommands["RequestOutputStatus"]
	sendMsg(cmd)
}

List<hubitat.device.HubAction> refreshCounterValues(boolean delay = true) {
	List<hubitat.device.HubAction> cmds = []
	int i
	for (i = 1; i <= 64; i += 1) {
		if (getChildDevice(device.deviceNetworkId + "_X_" + String.format("%02d", i)) != null)
			cmds.add(refreshCounterValue(i))
	}
	if (delay)
		return delayBetween(cmds, 500)
	else
		return cmds
}

hubitat.device.HubAction refreshCounterValue(BigDecimal counter) {
	if (dbgEnable)
		log.debug "${device.label} refreshCounterValue(${counter})"
	String cmd = elkCommands["RequestCounterValue"] + String.format("%02d", counter.toInteger())
	sendMsg(cmd)
}

hubitat.device.HubAction refreshCustomValues() {
	if (dbgEnable)
		log.debug "${device.label} refreshCustomValues"
	String cmd = elkCommands["RequestAllCustomValues"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshCustomValue(BigDecimal custom) {
	if (dbgEnable)
		log.debug "${device.label} refreshCustomValue(${custom})"
	String cmd = elkCommands["RequestCustomValue"] + String.format("%02d", custom.toInteger())
	sendMsg(cmd)
}

hubitat.device.HubAction zoneBypass(BigDecimal zoneNumber = 0, int sysArea = state.area, String sysCode = code) {
	if ((zoneNumber >= 0 && zoneNumber < 209) || zoneNumber == 999) {
		String zoneNbr = String.format("%03d", zoneNumber.toInteger())
		if (dbgEnable)
			log.debug "${device.label} zone ${zoneNbr} zoneBypass"
		String cmd = elkCommands["ZoneBypass"] + zoneNbr
		sendMsg(cmd, sysArea, sysCode)
	}
}

hubitat.device.HubAction zoneTrigger(BigDecimal zoneNumber = 0) {
	if (zoneNumber > 0 && zoneNumber < 209) {
		String zoneNbr = String.format("%03d", zoneNumber.toInteger())
		if (dbgEnable)
			log.debug "${device.label} zone ${zoneNbr} zoneTrigger"
		String cmd = elkCommands["ZoneTrigger"] + zoneNbr
		sendMsg(cmd)
	}
}

hubitat.device.HubAction requestZoneVoltage(BigDecimal zoneNumber = 0) {
	if (zoneNumber > 0 && zoneNumber < 209) {
		String zoneNbr = String.format("%03d", zoneNumber.toInteger())
		if (dbgEnable)
			log.debug "${device.label} zone ${zoneNbr} zoneVoltage"
		String cmd = elkCommands["ZoneVoltage"] + zoneNbr
		sendMsg(cmd)
	}
}

hubitat.device.HubAction controlLightingMode(String unitCode, String mode, String setting, String time) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingMode ${unitCode} mode: ${mode} setting: ${setting} time: ${time}"
	String cmd = elkCommands["ControlLightingMode"] +
			unitCode + mode.padLeft(2, '0').take(2) + setting.padLeft(2, '0').take(2) + time.padLeft(4, '0').take(4)
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingOn(String unitCode = "A03") {
	if (dbgEnable)
		log.debug "${device.label} controlLightingOn: ${unitCode}"
	String cmd = elkCommands["ControlLightingOn"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingOff(String unitCode) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingOff: ${unitCode}"
	String cmd = elkCommands["ControlLightingOff"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingToggle(String unitCode) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingToggle: ${unitCode}"
	String cmd = elkCommands["ControlLightingToggle"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction chime(int keypadNumber = keypad) {
	push(7, keypadNumber)
}

hubitat.device.HubAction push(BigDecimal button, int keypadNumber = keypad) {
	String buttonCode = button.toString()
	if (button == 7)
		buttonCode = "C"
	if (button == 8)
		buttonCode = "*"
	if (button >= 1 && button <= 8) {
		requestKeypadPress(buttonCode, keypadNumber)
	}
}

List<hubitat.device.HubAction> refreshLightingStatus(boolean delay = true) {
	if (dbgEnable)
		log.debug "${device.label} refreshLightingStatus"
	List<hubitat.device.HubAction> cmds = [refreshLightingStatus("0"), refreshLightingStatus("1"), refreshLightingStatus("2"),
										   refreshLightingStatus("3")]
	if (delay)
		return delayBetween(cmds, 500)
	else
		return cmds
}

hubitat.device.HubAction refreshLightingStatus(String unitCode) {
	String bank = unitCode.take(1)
	if (bank >= "A" && bank <= "D")
		bank = "0"
	else if (bank >= "E" && bank <= "H")
		bank = "1"
	else if (bank >= "I" && bank <= "L")
		bank = "2"
	else if (bank >= "M" && bank <= "P")
		bank = "3"

	if (dbgEnable)
		log.debug "${device.label} refreshLightingStatus: ${bank}"
	String cmd = elkCommands["RequestLightingStatus"] + bank
	sendMsg(cmd)
}

hubitat.device.HubAction refreshKeypadArea() {
	if (dbgEnable)
		log.debug "${device.label} refreshKeypadArea"
	String cmd = elkCommands["RequestKeypadArea"]
	sendMsg(cmd)
}

hubitat.device.HubAction requestKeypadPress(String key = "0", int keypadNumber = keypad) {
	if (dbgEnable)
		log.debug "${device.label} requestKeypadPress"
	String cmd = elkCommands["RequestKeypadPress"] + String.format("%02d", keypadNumber) + key
	sendMsg(cmd)
}

hubitat.device.HubAction requestKeypadStatus(int keypadNumber = keypad) {
	if (dbgEnable)
		log.debug "${device.label} requestKeypadStatus"
	String cmd = elkCommands["RequestKeypadStatus"] + String.format("%02d", keypadNumber)
	sendMsg(cmd)
}

hubitat.device.HubAction showTextOnKeypads(BigDecimal time, String beep, int sysArea = state.area) {
	return showTextOnKeypads("", "", time, beep, sysArea)
}

hubitat.device.HubAction showTextOnKeypads(String line1, BigDecimal time, String beep, int sysArea = state.area) {
	return showTextOnKeypads(line1, "", time, beep, sysArea)
}

hubitat.device.HubAction showTextOnKeypads(String line1 = "", String line2 = "", BigDecimal time = 0, String beep = "no", int sysArea = state.area) {
	if (dbgEnable)
		log.debug "${device.label} showTextOnKeypads: line1 ${line1}, line2 ${line2}, time ${time}, beep ${beep}, area ${sysArea}"
	if (line1.length() == 0) {
		line1 = line2
		line2 = ""
	}
	String clear = (time == 0 ? "1" : "2")
	String hasBeep = (beep == "yes" ? "1" : "0")
	int duration = (time < 1 ? 0 : time > 65535 ? 65535 : time.intValue())
	if (line1.length() > 16)
		line1 = line1.substring(0, 16)
	else if (line1.length() == 0)
		clear = "0"
	else if (line1.length() < 16)
		line1 = line1 + "^"
	if (line2.length() > 16)
		line2 = line2.substring(0, 16)
	else if (line2.length() > 0 && line2.length() < 16)
		line2 = line2 + "^"
	String cmd = elkCommands["ShowTextOnKeypads"] + sysArea.toString().padLeft(1, '0') + clear + hasBeep + String.format("%05d", duration) +
			line1.padRight(16, ' ') + line2.padRight(16, ' ')
	sendMsg(cmd)
}
//Elk M1 Command Line Request - End of


//Elk M1 Message Send Lines - Start of
hubitat.device.HubAction sendMsg(String cmd, int sysArea = 0, String sysCode = null) {
	return sendMsg(cmd, "00", sysArea, sysCode)
}

hubitat.device.HubAction sendMsg(String cmd, String extra, int sysArea = 0, String sysCode = null) {
	String msg
	if (sysArea < 0 || sysArea > 8 || sysCode == null)
		msg = cmd + extra
	else
		msg = cmd + sysArea.toString() + sysCode.padLeft(6, '0').take(6) + extra
	String msgStr = addChksum(Integer.toHexString(msg.length() + 2).toUpperCase().padLeft(2, '0') + msg)
	if (dbgEnable)
		log.debug "${device.label} sendMsg: $msgStr"
	return new hubitat.device.HubAction(msgStr, hubitat.device.Protocol.TELNET)
}

hubitat.device.HubAction sendMsg(hubitat.device.HubAction action = null) {
	return action
}

String addChksum(String msg) {
	char[] msgArray = msg.toCharArray()
	int msgSum = 0
	msgArray.each { (msgSum += (int) it) }
	String chkSumStr = Integer.toHexString(256 - (msgSum % 256)).toUpperCase().padLeft(2, '0')
	if (chkSumStr.length() == 2)
		return msg + chkSumStr
	else
		return msg + chkSumStr.substring(1)
}

//Elk M1 Message Send Lines - End of


//Elk M1 Event Receipt Lines
private List parse(String message) {
	List statusList = null
	if (dbgEnable)
		log.debug "${device.label} Parsing Incoming message: " + message

	switch (message.substring(2, 4)) {
		case "ZC":
			zoneChange(message)
			break
		case "XK":
			heartbeat()
			break
		case "CC":
			outputChange(message)
			break
		case "TC":
			taskChange(message)
			break
		case "EE":
			statusList = entryExitChange(message)
			break
		case "AS":
			statusList = armStatusReport(message)
			break
		case "ZS":
			zoneStatusReport(message)
			break
		case "CS":
			outputStatus(message)
			break
		case "DS":
			lightingDeviceStatus(message)
			break
		case "PC":
			lightingDeviceChange(message)
			break
		case "LW":
			statusList = temperatureData(message)
			break
		case "ST":
			statusList = statusTemperature(message)
			break
		case "TR":
			thermostatData(message)
			break
		case "IC":
			statusList = userCodeEntered(message)
			break
		case "EM":
			statusList = sendEmail(message)
			break
		case "AR":
			alarmReporting(message)
			break
		case "PS":
			lightingBankStatus(message)
			break
		case "LD":
			logData(message)
			break
		case "SD":
			statusList = stringDescription(message)
			break
		case "AM":
			statusList = updateAlarmAreas(message)
			break
		case "AZ":
			updateAlarmZones(message)
			break
		case "KA":
			keypadAreaAssignments(message)
			break
		case "KC":
			statusList = keypadKeyChangeUpdate(message)
			break
		case "KF":
			statusList = keypadFunctionKeyUpdate(message)
			break
		case "VN":
			versionNumberReport(message)
			break
		case "ZD":
			zoneDefinitionReport(message)
			break
		case "IE":
			statusList = refresh()
			break
		case "RP":
			statusList = connectionStatus(message)
			break
		case "SS":
			statusList = updateSystemTrouble(message)
			break
		case "AP":
			receiveTextString(message)
			break
		case "CU":
			statusList = responseUserChange(message)
			break
		case "CR":
			updateCustom(message)
			break
		case "CV":
			updateCounter(message)
			break
		case "CA":
			audioData(message, message.substring(4, 6)) // Audio Zone number added
			break;
		case "CD":
			audioData(message, message.substring(8, 10)) // Audio Zone number added
			break
		case "ZV":
			zoneVoltage(message)
			break
		case "UA":
			statusList = userAreaReport(message)
			break
		case "RR":
		case "ZB":
			break
		default:
			if (txtEnable) log.info "${device.label}: The ${message.substring(2, 4)} command is unknown"
			break
	}
	if (statusList != null && statusList.size() > 0) {
		//log.debug "Trying: ${statusList}"
		List rtn = []
		statusList.each {
			if (it instanceof Map) {
				if (device.currentState(it.name)?.value == null || device.currentState(it.name).value != it.value.toString() || it.isStateChange) {
					if (it.descriptionText == null)
						it.descriptionText = device.label + " " + it.name + " was " + it.value
					else
						it.descriptionText = device.label + " " + it.descriptionText
					rtn << createEvent(it)
					if ((txtEnable == "all" || it?.type == "system" || txtEnable == it?.type) && it.name != "armState" && it.name != "contact" &&
							it.name != "lock" && it.name != "securityKeypad" && it.name != "trouble" && it.name != "temperature")
						log.info it.descriptionText
				}
			} else {
				rtn << it
			}
		}
		statusList = rtn
	}
	return statusList
}

void zoneChange(String message) {
	String zoneNumber = message.substring(4, 7)
	String zoneStatusCode = message.substring(7, 8)
	String zoneStatus = elkZoneStatuses[zoneStatusCode]
	switch (zoneStatusCode) {
		case "1":
		case "2":
		case "3":
			zoneNormal(zoneNumber, zoneStatus)
			break;
		case "9":
		case "A":
		case "B":
			zoneViolated(zoneNumber, zoneStatus)
			break;
		case "5":
		case "6":
		case "7":
		case "C":
		case "D":
		case "E":
		case "F":
			zoneTrouble(zoneNumber, zoneStatus)
			break
		default:
			if (dbgEnable) log.debug "${device.label} Unknown zone status: zone ${zoneStatus} - ${zoneStatus}"
			break
	}
}

void zoneVoltage(String message) {
	String zoneNumber = message.substring(4, 7)
	BigDecimal zoneVoltageNumber = new BigDecimal(message.substring(7, 10)) / 10
	com.hubitat.app.DeviceWrapper zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice != null) {
		String description = "${zoneDevice.label} voltage was ${zoneVoltageNumber}"
		zoneDevice.sendEvent(name: "voltage", value: zoneVoltageNumber, descriptionText: description, unit: "V")
		if (txtEnable != "none")
			log.info description
	} else if (dbgEnable) {
		log.debug "${device.label} zone ${zoneNumber} voltage was ${zoneVoltageNumber}";
	}
}

List<Map> userAreaReport(String message) {
	List<Map> statusList = null
	int codeLength = message.substring(20, 21).toInteger()
	String userCode = message.substring(10 - codeLength, 10)
	String userAreas = message.substring(10, 12)
	String userType = message.substring(21, 22)
	String tempUnit = message.substring(22, 23)
	switch (userType) {
		case "1":
			userType = "User"
			break
		case "2":
			userType = "Master"
			break
		case "3":
			userType = "Installer"
			break
		case "4":
			userType = "ELKRP"
			break
	}
	if (dbgEnable)
		log.debug "${device.label} userCode ${userCode}, userAreas ${userAreas}, codeLength ${codeLength}, userType ${userType}, tempUnit ${tempUnit}";
	if (userCode == code) {
		state.userAreas = userAreas
		state.userType = userType
	}
	state.tempUnit = "˚" + tempUnit
	statusList = [[name: "codeLength", value: codeLength, type: "system"]]
	return statusList
}

List<Map> entryExitChange(String message) {
	List<Map> statusList = null
	int sysArea = message.substring(4, 5).toInteger()
	boolean isEntry = (message.substring(5, 6) != "0")
	int delay = Math.max(message.substring(6, 9).toInteger(), message.substring(9, 12).toInteger())
	String armStatus
	state.entryExit[sysArea - 1] = now()
	if (delay == 0 || isEntry) {
		armStatus = elkArmStatuses[message.substring(12, 13)]
	} else {
		armStatus = elkArmingStatuses[message.substring(12, 13)]
	}
	if (dbgEnable)
		log.debug "${device.label} Area: $sysArea, Entry: $isEntry, Delay: $delay, armStatus: $armStatus"
	if (state.keypadAreas != null && state.keypadAreas[sysArea.toString()] != null) {
		if (!isEntry)
			statusList = updateAreaStatus(sysArea, [[name: "armingIn", value: delay, type: "area"]])
		if (delay != 0 && isEntry && armStatus != Disarmed && sysArea == state.area) {
			parent.speakEntryDelay()
		}
	}
	return statusList
}

List<Map> armStatusReport(String message) {
	List<Map> statusList = []
	Integer i
	String armStatus
	String armUpState
	String alarmState
	String armedAreas = ""
	for (i = 1; i <= 8; i += 1) {
		armUpState = message.substring(11 + i, 12 + i)
		armedAreas += (armUpState >= "3" && armUpState <= "6") ? "1" : "0"
		if (state.keypadAreas != null && state.keypadAreas[i.toString()] != null) {
			armStatus = elkArmStatuses[message.substring(3 + i, 4 + i)]
			armUpState = elkArmUpStates[armUpState]
			alarmState = elkAlarmStates[message.substring(19 + i, 20 + i)]
			if (dbgEnable && i == state.area) {
				log.debug "${device.label} Area ${i} armStatus: ${armStatus} armState: ${armUpState} alarmState: ${alarmState}"
			}

			List<Map> statuses = [[name: "alarmState", value: alarmState, type: "area"]]
			// Ignore Armed Fully unless it was preceeded by an Entry/Exit delay message.
			if (armUpState != ArmedFully || now() - (long) (state.entryExit[i - 1]) < 4000) {
				if (armUpState == ArmedwithExit)
					armStatus = elkArmingStatuses[message.substring(3 + i, 4 + i)]
				statuses << [name: "armState", value: armUpState, type: "area"]
				statuses += setStatus(i, armStatus)
			}
			if (alarmState == PoliceAlarm || alarmState == BurgularAlarm) {
				statuses << [name: "contact", value: "open", type: "area"]
				//if (sysArea == state.area)
				parent.speakAlarm()
			} else {
				statuses << [name: "contact", value: "closed", type: "area"]
			}

			statusList += updateAreaStatus(i, statuses)
		}
	}
	if (armedAreas != state.armedAreas)
		state.armedAreas = armedAreas
	return statusList
}

boolean isArmed(int sysArea) {
	if (state.armedAreas == null || state.armedAreas.length() != 8) {
		return false
	} else {
		return (state.armedAreas.substring(sysArea - 1, sysArea) == "1")
	}
}

List<Map> setStatus(int sysArea, String armStatus) {
	List<Map> statusList = []
	String armMode = "Disarmed"
	String lock
	String securityKeypad
	boolean armChange = false
	String lastUserName = state.lastUserName == null ? "" : " by " + state.lastUserName
	if (sysArea == state.area && (device.currentState("armStatus")?.value == null ||
			device.currentState("armStatus").value != armStatus)) {
		armChange = true
	}

	switch (armStatus) {
		case Disarmed:
			securityKeypad = armStatus
			lock = "unlocked"
			break
		case ArmedAway:
			securityKeypad = armStatus
			armMode = "Away"
			lock = "locked"
			break
		case ArmedHome:
		case ArmedHomeInstant:
			securityKeypad = ArmedHome
			armMode = "Home"
			lock = "locked"
			break
		case ArmedNight:
		case ArmedNightInstant:
			securityKeypad = ArmedNight
			armMode = "Night"
			lock = "locked"
			break
		case ArmedVacation:
			securityKeypad = ArmedAway
			armMode = "Vacation"
			lock = "locked"
			break
		case ArmingAway:
			if (armChange)
				parent.speakArmingAway()
			securityKeypad = ArmedAway
			lock = "unlocked with timeout"
			break
		case ArmingHome:
		case ArmingHomeInstant:
			if (armChange)
				parent.speakArmingHome()
			securityKeypad = ArmedHome
			lock = "unlocked with timeout"
			break
		case ArmingNight:
		case ArmingNightInstant:
			if (armChange)
				parent.speakArmingNight()
			securityKeypad = ArmedNight
			lock = "unlocked with timeout"
			break
		case ArmingVacation:
			if (armChange)
				parent.speakArmingVacation()
			securityKeypad = ArmedAway
			lock = "unlocked with timeout"
			break
	}
	parent.setArmMode(armMode)
	statusList << [name: "armStatus", value: armStatus, type: "area", descriptionText: "armStatus was " + armStatus + lastUserName]
	if (securityKeypad)
		statusList << [name: "securityKeypad", value: securityKeypad, type: "area", descriptionText: "securityKeypad was " +
				securityKeypad + lastUserName]
	if (lock)
		statusList << [name: "lock", value: lock, type: "area", descriptionText: "lock was " + lock + lastUserName]
	return statusList
}

List<Map> updateAreaStatus(int sysArea, List<Map> areaStatus) {
	List<Map> statusList = []
	if (state.keypadAreas != null) {
		List<String> keypads = state.keypadAreas[sysArea.toString()]
		if (keypads != null) {
			keypads.each {
				if (it == "00") {
					statusList = areaStatus
				} else {
					com.hubitat.app.DeviceWrapper keypadDevice = getChildDevice(device.deviceNetworkId + "_P_0" + it)
					if (keypadDevice != null && keypadDevice.hasCapability("Actuator"))
						keypadDevice.parse(areaStatus)
				}
			}
		}
	}
	return statusList
}

List<Map> updateKeypadStatus(String keypadNumber, List<Map> keypadStatus) {
	List<Map> statusList = []
	if (keypadNumber.toInteger() == keypad && keypadStatus.first().name != "temperature") {
		statusList = keypadStatus
	}
	com.hubitat.app.DeviceWrapper keypadDevice = getChildDevice("${device.deviceNetworkId}_P_0${keypadNumber}")
	if (keypadDevice != null) {
		if (keypadDevice.hasCapability("Actuator")) {
			keypadDevice.parse(keypadStatus)
		} else if (keypadStatus.first().name == "temperature") {
			Map eventMap = keypadStatus.first()
			if (eventMap.descriptionText == null)
				eventMap.descriptionText = keypadDevice.label + " " + eventMap.name + " was " + eventMap.value
			else
				eventMap.descriptionText == keypadDevice.label + " " + eventMap.descriptionText
			keypadDevice.sendEvent(eventMap)
		}
	}
	return statusList
}

List<Map> userCodeEntered(String message) {
	String userCode = message.substring(4, 16)
	String userNumber = message.substring(16, 19).toInteger().toString()
	String keypadNumber = message.substring(19, 21)
	Map userEvent
	if (dbgEnable)
		log.debug "${device.label} userCodeEntered was: ${userNumber}, code ${userCode} on keypad ${keypadNumber}"
	if (userCode == "000000000000") {
		String lastUserName
		if (device.currentState("lockCodes")?.value != null) {
			TreeMap<Map> oldCodes = new groovy.json.JsonSlurper().parseText(decrypt(device.currentState("lockCodes").value))
			lastUserName = oldCodes[userNumber]?.name
		}
		if (lastUserName == null) {
			switch (userNumber) {
				case "201":
					lastUserName = "Installer"
					break
				case "202":
					lastUserName = "ElkRP"
					break
				case "203":
					lastUserName = "No Code"
					break
				default:
					lastUserName = "Unknown user"
					break
			}
		}
		state.lastUserName = userNumber + " - " + lastUserName
		userEvent = [name: "lastUser", value: userNumber, type: "keypad", descriptionText: "lastUser was " + userNumber + " - " + lastUserName]
	} else {
		if (userCode.substring(0, 1) == "0" && userCode.substring(2, 3) == "0" && userCode.substring(4, 5) == "0" &&
				userCode.substring(6, 7) == "0" && userCode.substring(8, 9) == "0" && userCode.substring(10, 11) == "0")
			userCode = userCode.substring(1, 2) + userCode.substring(3, 4) + userCode.substring(5, 6) +
					userCode.substring(7, 8) + userCode.substring(9, 10) + userCode.substring(11, 12)
		userEvent = [name: "invalidUser", value: userCode, type: "keypad", descriptionText: "invalidUser was " + userCode]
	}
	com.hubitat.app.DeviceWrapper keypadDevice = getChildDevice(device.deviceNetworkId + "_P_0" + keypadNumber)
	if (keypadDevice != null && keypadDevice.hasCapability("Actuator"))
		keypadDevice.parse([userEvent])
	if (keypadNumber != "00" && keypadNumber.toInteger() != keypad)
		userEvent.descriptionText += " on keypad " + keypadNumber
	return [userEvent]
}

List<Map> sendEmail(String message) {
	int emailNumber = message.substring(4, 7).toInteger()
	return [[name: "sendEmail", value: emailNumber, type: "system", descriptionText: "${device.label} sent email # ${emailNumber}"]]
}

void alarmReporting(String message) {
	String accountNumber = message.substring(4, 10)
	String alarmCode = message.substring(10, 14)
	int alarmArea = message.substring(14, 16).toInteger()
	String zone = message.substring(16, 19)
	String telIp = message.substring(19, 20)
	log.warn "${device.label} AlarmReporting account: ${accountNumber}, code: ${alarmCode}, area ${alarmArea}, zone ${zone}, device ${telIp}"
}

void outputChange(String message) {
	String outputNumber = message.substring(4, 7)
	String outputState = elkStates[message.substring(7, 8)]
	com.hubitat.app.DeviceWrapper outputDevice = getChildDevice("${device.deviceNetworkId}_O_${outputNumber}")
	if (outputDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} outputChange: ${outputNumber} - ${outputState}"
		outputDevice.parse(outputState)
		if (state.outputReport != null) {
			state.outputReport = sendReport(state.outputReport, outputDevice, outputNumber, outputState == On)
		}
	}
}

void outputStatus(String message) {
	String outputString = message.substring(4, 212)
	String outputState
	int i
	for (i = 1; i <= 208; i++) {
		outputState = outputString.substring(i - 1, i)
		//if (dbgEnable)
		//	log.debug "${device.label}: OutputStatus: Output " + i + " - " + elkStates[outputState]
		outputChange("    " + String.format("%03d", i) + outputState)
	}
}

void lightingDeviceStatus(String message) {
	int deviceNumber = message.substring(4, 7).toInteger()
	String level = message.substring(7, 9)
	int ndx = (deviceNumber - 1) / 16
	int unitNumber = deviceNumber - ndx * 16
	lightingDeviceChange("    " + "ABCDEFGHIJKLMNOP".substring(ndx, ndx + 1) + String.format("%02d", unitNumber) + level)
}

void lightingBankStatus(String message) {
	int bank = message.substring(4, 5).toInteger()
	char[] statusString = message.substring(5, 69).toCharArray()
	String groups = "ABCDEFGHIJKLMNOP".substring(bank * 4, bank * 4 + 4)
	String search = device.deviceNetworkId + "_L_"
	int len = search.length()
	String childDeviceDNID
	int ndx
	int level
	getChildDevices().each {
		childDeviceDNID = it.deviceNetworkId
		if (childDeviceDNID.substring(0, len) == search) {
			ndx = groups.indexOf(childDeviceDNID.substring(len, len + 1))
			if (ndx != -1) {
				level = ((int) statusString[childDeviceDNID.substring(len + 1, len + 3).toInteger() + ndx * 16 - 1]) - 48
				lightingDeviceChange("    " + childDeviceDNID.substring(len, len + 3) + String.format("%02d", level))
			}
		}
	}
}

void lightingDeviceChange(String message) {
	String unitCode = message.substring(4, 7)
	String level = message.substring(7, 9)
	if (dbgEnable)
		log.debug "${device.label} Light: ${unitCode} Level: ${level}"
	getChildDevice("${device.deviceNetworkId}_L_${unitCode}")?.parse(level)
}

void logData(String message) {
	if (message.substring(20, 23) == "000") {
		String eventDesc = elkLogData[message.substring(4, 8).toInteger()]
		if (eventDesc != null) {
			String sysArea = message.substring(11, 12)
			if (sysArea == "0")
				sysArea = ""
			else
				sysArea = " Area ${sysArea}"
			String sysNumber = message.substring(8, 11)
			if (sysNumber == "000")
				sysNumber = ""
			else if (sysNumber.substring(0, 2) == "00")
				sysNumber = sysNumber.substring(2)
			else if (sysNumber.substring(0, 1) == "0")
				sysNumber = sysNumber.substring(1)
			log.warn "${device.label}${sysArea}: ${eventDesc} ${sysNumber}"
		}
	}
}

List stringDescription(String message) {
	List<Map> statusList = []
	String deviceNumber = message.substring(6, 9)
	if (deviceNumber != "000") {
		String deviceName
		String deviceType = message.substring(4, 6)
		byte firstText = message.substring(9, 10)
		String deviceText = (String) ((char) (firstText & 0b01111111)) + message.substring(10, 25).trim()
		// Mask high order "Show On Keypad" bit in first letter
		if (deviceText != "") {
			if (deviceType == "02")
				updateUserName(deviceNumber, deviceText)
			if (deviceType >= "12" && deviceType <= "17")
				statusList = updateFKeyName(deviceNumber.substring(1), deviceType, deviceText.trim())
			else if (deviceType == "19")
				audioData(message)
			else if (state.creatingDevice == deviceType)
				createDevice([deviceNumber: deviceNumber, deviceName: deviceName, deviceType: deviceType, deviceText: deviceText])
		}
		if (state.creatingDevice == deviceType) {
			runIn(10, "stopCreatingDevice")
			int i = deviceNumber.toInteger() // Request next device description
			int max = elkTextDescriptionsMax[deviceType].toInteger()
			if (i < max) {
				statusList << RequestTextDescriptions(deviceType, i + 1)
			}
		}
	}
	return statusList
}

List<Map> temperatureData(String message) {
	List<Map> statusList = null
	String temp
	int i
	int deviceNumber
	for (i = 4; i <= 50; i += 3) {
		temp = message.substring(i, i + 3)
		if (temp != "000") {
			deviceNumber = (i - 1) / 3
			statusList = statusTemperature("    1" + String.format("%02d", deviceNumber) + temp + "    ")
		}
	}

	for (i = 52; i <= 98; i += 3) {
		temp = message.substring(i, i + 3)
		if (temp != "000") {
			deviceNumber = (i - 49) / 3
			statusTemperature("    0" + String.format("%02d", deviceNumber) + temp + "    ")
		}
	}
	return statusList
}

List<Map> statusTemperature(String message) {
	List<Map> statusList = null
	String group = elkTempTypes[message.substring(4, 5).toInteger()]
	String deviceNumber = message.substring(5, 7)
	int temp = message.substring(7, 10).toInteger()
	String uom = state.tempUnit
	if (group == TemperatureProbe) {
		temp = temp - 60
		if (dbgEnable)
			log.debug "${device.label} Zone ${deviceNumber} temperature was ${temp} ${uom}"
		com.hubitat.app.DeviceWrapper zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_0${deviceNumber}")
		if (zoneDevice?.hasCapability("TemperatureMeasurement")) {
			zoneDevice.sendEvent(name: "temperature", value: temp, unit: uom, descriptionText: "${device.label} temperature was ${temp} ${uom}")
		}
	} else if (group == Keypads) {
		temp = temp - 40
		if (dbgEnable)
			log.debug "${device.label} Keypad ${deviceNumber} temperature is ${temp} ${uom}"
		statusList = updateKeypadStatus(deviceNumber, [[name: "temperature", value: temp, unit: uom, type: "keypad"]])
	} else if (group == Thermostats) {
		if (dbgEnable)
			log.debug "${device.label} Thermostat ${deviceNumber} temperature is ${temp} ${uom}"
		com.hubitat.app.DeviceWrapper thermDevice = getChildDevice("${device.deviceNetworkId}_T_0${deviceNumber}")
		if (thermDevice?.hasCapability("Thermostat")) {
			thermDevice.sendEvent(name: "temperature", value: temp, unit: uom, descriptionText: "${device.label} temperature was ${temp} ${uom}")
		}
	}
	return statusList
}

void taskChange(String message) {
	String taskNumber = message.substring(4, 7)
	com.hubitat.app.DeviceWrapper taskDevice = getChildDevice(device.deviceNetworkId + "_K_" + taskNumber)
	if (taskDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} Task Change Update: ${taskNumber}"
		taskDevice.parse()
		if (state.taskReport != null) {
			state.taskReport = sendReport(state.taskReport, taskDevice, taskNumber, true)
		}
	}
}

void thermostatData(String message) {
	String thermNumber = message.substring(4, 6).padLeft(3, '0')
	com.hubitat.app.DeviceWrapper thermDevice = getChildDevice(device.deviceNetworkId + "_T_" + thermNumber)
	if (thermDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} thermostatData: ${thermNumber} - ${message.substring(6, 17)}"
		thermDevice.parse(state.tempUnit + message.substring(2))
	}
}

void heartbeat() {
	if (timeout != null && timeout.toString().isInteger() && timeout >= 1)
		runIn(timeout * 60, "telnetTimeout")
}

void keypadAreaAssignments(String message) {
	int myArea = 0
	int sysArea
	int i
	Map<String> areas = [:]
	for (i = 1; i <= 16; i += 1) {
		sysArea = message.substring(i + 3, i + 4).toInteger()
		if (sysArea != 0) {
			if (i == keypad)
				myArea = sysArea
			String keypadNumber = String.format("%02d", i)
			com.hubitat.app.DeviceWrapper keypadDevice = getChildDevice("${device.deviceNetworkId}_P_0${keypadNumber}")
			if (keypadDevice != null && keypadDevice.hasCapability("Actuator")) {
				if (areas[sysArea] == null)
					areas[sysArea] = []
				areas[sysArea] << keypadNumber
				keypadDevice.parse([[name: "area", value: sysArea, type: "keypad"]])
			}
		}
	}
	if (myArea == 0) {
		log.warn "${device.label} warning: keypad not found"
	} else {
		if (areas[myArea] == null)
			areas[myArea] = ["00"]
		else
			areas[myArea] = ["00"] + areas[myArea]
		state.area = myArea
		state.keypadAreas = areas
	}
	if (dbgEnable)
		log.debug "Keypad Area Assignments ${areas}"
}

List<Map> keypadKeyChangeUpdate(String message) {
	String keypadNumber = message.substring(4, 6)
	String key = elkKeys[message.substring(6, 8)]
	List<Map> statusList = []
	List<Map> statuses = []
	int i
	if (key != NOTHING) {
		statuses << [name: "pushed", value: key, type: "keypad", descriptionText: "pushed ${key}"]
	}
	for (i = 1; i <= 6; i++) {
		statuses << [name: "f${i.toString()}LED", value: elkStates[message.substring(i + 7, i + 8)], type: "keypad"]
	}
	if (statuses.size() > 0)
		statusList += updateKeypadStatus(keypadNumber, statuses)

	byte[] chimes = message.substring(14, 23).getBytes()
	for (i = 1; i <= 8; i++) {
		if (state.keypadAreas != null && state.keypadAreas[i.toString()] != null) {
			statuses = []
			if (chimes[i] == 0x30) {
				statuses << [name: "beep", value: Off, type: "area"]
				statuses << [name: "chime", value: Off, type: "area"]
			} else {
				if (chimes[i] & 2)
					statuses << [name: "beep", value: Beeping, type: "area"]
				else if (chimes[i] & 1)
					statuses << [name: "beep", value: Beeped, type: "area"]
				if (chimes[i] & 4)
					statuses << [name: "chime", value: Chimed, type: "area"]
			}
			if (statuses.size() > 0)
				statusList += updateAreaStatus(i, statuses)
		}
	}
	return statusList
}

List<Map> keypadFunctionKeyUpdate(String message) {
	String keypadNumber = message.substring(4, 6)
	String key = elkFKeys[message.substring(6, 7)]
	List<Map> statusList = []
	if (key != NOTHING) {
		statusList += updateKeypadStatus(keypadNumber, [[name: "pushed", value: key, type: "keypad", descriptionText: "pushed ${key}"]])
	}
	int i
	for (i = 1; i <= 8; i++) {
		if (state.keypadAreas != null && state.keypadAreas[i.toString()] != null) {
			statusList += updateAreaStatus(i, [[name: "chimeMode", value: elkChimeStates[message.substring(i + 6, i + 7)], type: "area"]])
		}
	}
	return statusList
}

void versionNumberReport(String message) {
	if (dbgEnable)
		log.debug "versionNumberReport"
	BigInteger m1Version = new BigInteger(message.substring(4, 10), 16)
	BigInteger xepVersion = new BigInteger(message.substring(10, 16), 16)
	int m1U = m1Version / 65536
	int m1M = (m1Version - m1U * 65536) / 256
	int m1L = m1Version - m1U * 65536 - m1M * 256
	int xepU = xepVersion / 65536
	int xepM = (xepVersion - xepU * 65536) / 256
	int xepL = xepVersion - xepU * 65536 - xepM * 256
	state.m1Version = String.format("%3d.%3d.%3d", m1U, m1M, m1L).replace(" ", "")
	state.xepVersion = String.format("%3d.%3d.%3d", xepU, xepM, xepL).replace(" ", "")
	if (txtEnable != "none")
		log.info "${device.label} panel version ${state.m1Version}, XEP Version ${state.xepVersion} found"
}

List<hubitat.device.HubAction> connectionStatus(String message) {
	List<hubitat.device.HubAction> commandList = null
	switch (message.substring(4, 6)) {
		case "00":
			log.warn "${device.label} ELKRP disconnected"
			commandList = refresh()
			break
		case "01":
			log.warn "${device.label} ELKRP is connected"
			break
		case "02":
			log.warn "${device.label} M1XEP is initializing"
			break
	}
	return commandList
}

List<Map> updateSystemTrouble(String message) {
	List<Map> statusList = []
	byte[] statuses = message.substring(4, 38).getBytes()
	if (dbgEnable)
		log.debug "${device.label} updateSystemTrouble"
	int troubleCode
	String troubleMessage
	List<Integer> activeTrouble = []
	int i
	for (i = 0; i <= 33; i++) {
		troubleCode = statuses[i] - 48
		if (troubleCode || (state.trouble != null && state.trouble.findIndexOf { it == i } != -1)) {
			troubleMessage = elkTroubleCodes[i]
			if (troubleMessage != null) {
				if (troubleMessage == BoxTamperTrouble || troubleMessage == TransmitterLowBatteryTrouble || troubleMessage == SecurityAlert ||
						troubleMessage == LostTransmitterTrouble || troubleMessage == FireTrouble)
					troubleMessage += " zone " + troubleCode.toString()
				if (troubleCode) {
					activeTrouble << i
				} else {
					troubleMessage += " restored"
				}
				log.warn "${device.label} ${troubleMessage}"
			}
		}
	}
	if (activeTrouble.size() > 0) {
		state.trouble = activeTrouble
		if (device.currentState("trouble")?.value == null || device.currentState("trouble").value != Detected)
			statusList << [name: "trouble", value: Detected, type: "system"]
	} else {
		state.remove("trouble")
		if (device.currentState("trouble")?.value == null || device.currentState("trouble").value != Clear)
			statusList << [name: "trouble", value: Clear, type: "system"]
	}
	return statusList
}

List<Map> updateAlarmAreas(String message) {
	List<Map> statusList = []
	if (dbgEnable)
		log.debug "${device.label} updateAlarmAreas"
	String isAlarm
	for (i = 1; i <= 8; i++) {
		isAlarm = (message.substring(3 + i, 4 + i) == "1" ? Detected : Clear)
		statusList += updateAreaStatus(i, [[name: "alarm", value: isAlarm, type: "area"]])
	}
	return statusList
}

void updateAlarmZones(String message) {
	if (dbgEnable)
		log.debug "${device.label} updateAlarmZones"
	String zoneType
	int i
	for (i = 1; i <= 208; i++) {
		zoneType = elkZoneDefinitions[message.substring(i + 3, i + 4)]
		if (zoneType != Disabled) {
			log.warn "${device.label} zone ${i}, type ${zoneType} is in alarm"
		}
	}
}

void receiveTextString(String message) {
	String textString = message.substring(4)
	if (txtEnable != "none")
		log.info "${device.label} receiveTextString: ${textString}"
}

List<Map> responseUserChange(String message) {
	List<Map> statusList = []
	String userCode = message.substring(4, 7)
	String userStatus = message.substring(7, 8)
	if (dbgEnable)
		log.debug "${device.label} responseUserChange user ${userCode} status ${userStatus}"
	String key = userCode.toInteger().toString()
	TreeMap<Map> newCodes
	if (state.requestedChange == null)
		newCodes = [:]
	else
		newCodes = new groovy.json.JsonSlurper().parseText(state.requestedChange)
	if (key == "0" || key == "255") // If the user change failed...
		key = newCodes.iterator().next().key // Assume this change is the first in the pending list
	TreeMap pendingChange = newCodes[key]
	if (pendingChange != null) {
		newCodes.remove(key)
		state.requestedChange = new groovy.json.JsonBuilder(newCodes).toString()
		TreeMap<Map> oldCodes
		if (device.currentState("lockCodes")?.value == null)
			oldCodes = [:]
		else
			oldCodes = new groovy.json.JsonSlurper().parseText(decrypt(device.currentState("lockCodes").value))
		if (userCode == "255" && oldCodes[key]?.code != null && pendingChange.code == oldCodes[key].code)
			userCode = key.padLeft(3, '0')
		String userName = key.toString() + (pendingChange.name in [null, ""] ? "" : " - " + pendingChange.name)
		String reason = ""
		String value
		if (userCode == "000") {
			value = "failed"
			reason = " - invalid authorization code"
		} else if (userCode == "255") {
			value = "failed"
			reason = " - duplicate code"
		} else {
			if (userStatus == "1") {
				oldCodes.remove(key)
				value = "deleted"
			} else {
				pendingChange.remove("status")
				if (pendingChange.code in [null, ""] && !(oldCodes[key]?.code in [null, ""]))
					pendingChange.code = oldCodes[key].code
				if (pendingChange.name in [null, ""] && !(oldCodes[key]?.name in [null, ""]))
					pendingChange.name = oldCodes[key].name
				oldCodes[key] = pendingChange
				value = oldCodes[key] == null ? "added" : "changed"
			}
			statusList << [name           : "lockCodes", value: encrypt(new groovy.json.JsonBuilder(oldCodes).toString()), type: "system",
						   descriptionText: "Lock code " + userName + " was " + value]
		}
		String descriptionText = "codeChanged for ${userName} was ${value}${reason}"
		if (reason != "")
			log.warn descriptionText
		statusList << [name: "codeChanged", value: value, type: "system", descriptionText: descriptionText, isStateChange: true]
	}
	return statusList
}

void updateCustom(String message) {
	String custom = message.substring(4, 6)
	if (custom == "00") {
		int offset
		int i
		for (i = 1; i <= 20; i++) {
			offset = i * 6
			updateCustom(String.format("%02d", i), message.substring(offset, offset + 5).toInteger(), message.substring(offset + 5, offset + 6))
		}
	} else {
		updateCustom(custom, message.substring(6, 11).toInteger(), message.substring(11, 12))
	}
}

void updateCustom(String custom, int value, String format) {
	if (dbgEnable)
		log.debug "${device.label} updateCustom ${custom} = ${value}, format ${format}"
	com.hubitat.app.DeviceWrapper customDevice = getChildDevice(device.deviceNetworkId + "_Y_" + custom)
	if (customDevice != null) {
		customDevice.parse([value: value, format: format])
	}
}

void updateCounter(String message) {
	String counter = message.substring(4, 6)
	int value = message.substring(6, 11).toInteger()
	if (dbgEnable)
		log.debug "${device.label} updateCounter ${counter} = ${value}"
	com.hubitat.app.DeviceWrapper counterDevice = getChildDevice(device.deviceNetworkId + "_X_" + counter)
	if (counterDevice != null) {
		counterDevice.parse(value.toString())
	}
}

void audioData(String message, String zoneNumber = "00") {
	if (zoneNumber == "00") { // If no zone number, send to all audio zones.
		if (dbgEnable)
			log.debug "${device.label} audioData All Zones: ${message}"
		String search = device.deviceNetworkId + "_A_"
		int len = search.length()
		getChildDevices().each {
			if (it.deviceNetworkId.substring(0, len) == search)
				it.parse(message)
		}
	} else { // Send to the provided audio zone.
		if (dbgEnable)
			log.debug "${device.label} audioData Zone: ${zoneNumber} - ${message}"
		com.hubitat.app.DeviceWrapper audioDevice = getChildDevice(device.deviceNetworkId + "_A_" + zoneNumber)
		if (audioDevice != null) {
			audioDevice.parse(message)
		}
	}
}

void zoneDefinitionReport(String message) {
	String zoneString
	String zoneDefinitions = message.substring(4, 212)
	int i
	for (i = 1; i <= 208; i++) {
		zoneString = elkZoneDefinitions[zoneDefinitions.substring(i - 1, i)]
		if (zoneString != null && zoneString != Disabled && txtEnable != "none") {
			log.info "${device.label} zoneDefinitionReport: Zone " + i + " - " + zoneString
		}
	}
}

void zoneStatusReport(String message) {
	String zoneString = message.substring(4, 212)
	String zoneStatus
	int i
	for (i = 1; i <= 208; i++) {
		zoneStatus = zoneString.substring(i - 1, i)
		if (zoneStatus != null && zoneStatus != "0") {
			//if (dbgEnable)
			//	log.debug "${device.label} ZoneStatus: Zone " + i + " - " + elkZoneStatuses[zoneStatus]
			zoneChange("    " + String.format("%03d", i) + zoneStatus + "    ")
		}
	}
}

// Zone Status
void zoneViolated(String zoneNumber, String zoneStatus) {
	if (dbgEnable)
		log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus}";
	com.hubitat.app.DeviceWrapper zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${zoneNumber}")
	}
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${zoneNumber}")
	}

	if (zoneDevice != null) {
		if (state.zoneReport != null) {
			state.zoneReport = sendReport(state.zoneReport, zoneDevice, zoneNumber, true)
		}
		List<String> cmdList = zoneDevice.supportedCommands
		if (cmdList.find { it.name == "open" } != null) {
			if (zoneDevice.currentState("contact")?.value == null || zoneDevice.currentState("contact").value != "open") {
				zoneDevice.open()
			}
		} else if (cmdList.find { it.name == "active" } != null) {
			if (zoneDevice.currentState("motion")?.value == null || zoneDevice.currentState("motion").value != "active") {
				zoneDevice.active()
			}
		} else if (cmdList.find { it.name == "on" } != null) {
			if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "on") {
				zoneDevice.on()
			}
		} else if (cmdList.find { it.name == "detected" } != null) {
			if ((zoneDevice.currentState("smoke")?.value == null || zoneDevice.currentState("smoke").value != "detected") &&
					(zoneDevice.currentState("carbonMonoxide")?.value == null || zoneDevice.currentState("carbonMonoxide").value != "detected")) {
				zoneDevice.detected()
			}
		} else if (cmdList.find { it.name == "wet" } != null) {
			if (zoneDevice.currentState("water")?.value == null || zoneDevice.currentState("water").value != "wet") {
				zoneDevice.wet()
			}
		} else if (cmdList.find { it.name == "arrived" } != null) {
			zoneDevice.arrived()
		} else {
			String capFound = zoneDevice.capabilities.find { capabilitiesViolated[it.name] != null }?.name
			if (capFound != null) {
				Map zoneEvent = capabilitiesViolated[capFound]
				if (zoneEvent.isStateChange ||
						zoneDevice.currentState(zoneEvent.name)?.value == null || zoneDevice.currentState(zoneEvent.name).value != zoneEvent.value) {
					if (zoneEvent.value == "open")
						zoneEvent.descriptionText = zoneDevice.label + " was opened"
					else
						zoneEvent.descriptionText = zoneDevice.label + " " + zoneEvent.name + " was " + zoneEvent.value
					zoneDevice.sendEvent(zoneEvent)
					if (txtEnable != "none")
						log.info zoneEvent.descriptionText
				}
			}
		}
	}
}

void zoneNormal(String zoneNumber, String zoneStatus) {
	if (dbgEnable)
		log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus}";
	com.hubitat.app.DeviceWrapper zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${zoneNumber}")
	}
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${zoneNumber}")
	}
	if (zoneDevice != null) {
		if (state.zoneReport != null) {
			state.zoneReport = sendReport(state.zoneReport, zoneDevice, zoneNumber, false)
		}
		List<String> cmdList = zoneDevice.supportedCommands
		if (cmdList.find { it.name == "close" } != null) {
			if (zoneDevice.currentState("contact")?.value == null || zoneDevice.currentState("contact").value != "closed") {
				zoneDevice.close()
			}
		} else if (cmdList.find { it.name == "inactive" } != null) {
			if (zoneDevice.currentState("motion")?.value == null || zoneDevice.currentState("motion").value != "inactive") {
				zoneDevice.inactive()
			}
		} else if (cmdList.find { it.name == "off" } != null) {
			if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "off") {
				zoneDevice.off()
			}
		} else if (cmdList.find { it.name == "clear" } != null) {
			if ((zoneDevice.currentState("smoke")?.value == null || zoneDevice.currentState("smoke").value != "clear") &&
					(zoneDevice.currentState("carbonMonoxide")?.value == null || zoneDevice.currentState("carbonMonoxide").value != "clear")) {
				zoneDevice.clear()
			}
		} else if (cmdList.find { it.name == "dry" } != null) {
			if (zoneDevice.currentState("water")?.value == null || zoneDevice.currentState("water").value != "dry") {
				zoneDevice.dry()
			}
		} else if (cmdList.find { it.name == "departed" } != null) {
			zoneDevice.departed()
		} else {
			String capFound = zoneDevice.capabilities.find { capabilitiesNormal[it.name] != null }?.name
			if (capFound != null) {
				Map zoneEvent = capabilitiesNormal[capFound]
				if (zoneEvent.isStateChange ||
						zoneDevice.currentState(zoneEvent.name)?.value == null || zoneDevice.currentState(zoneEvent.name).value != zoneEvent.value) {
					if (zoneEvent.value == "closed")
						zoneEvent.descriptionText = zoneDevice.label + " was closed"
					else
						zoneEvent.descriptionText = zoneDevice.label + " " + zoneEvent.name + " was " + zoneEvent.value
					zoneDevice.sendEvent(zoneEvent)
					if (txtEnable != "none")
						log.info zoneEvent.descriptionText
				}
			}
		}
	}
}

void zoneTrouble(String zoneNumber, String zoneStatus) {
	com.hubitat.app.DeviceWrapper zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice != null) {
		String capFound = zoneDevice.capabilities.find { capabilitiesNormal[it.name] != null }?.name
		if (capFound != null) {
			Map zoneEvent = capabilitiesNormal[capFound]
			zoneEvent.value = zoneStatus
			if (zoneEvent.isStateChange ||
					zoneDevice.currentState(zoneEvent.name)?.value == null || zoneDevice.currentState(zoneEvent.name).value != zoneEvent.value) {
				zoneEvent.descriptionText = zoneDevice.label + " was " + zoneEvent.value
				zoneDevice.sendEvent(zoneEvent)
				if (txtEnable != "none")
					log.info zoneEvent.descriptionText
			}
		}
	} else if (dbgEnable) {
		log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus}";
	}
}

void updateUserName(String userNumber, String userText) {
	if (state.creatingDevice == "02" && !(userText ==~ /(?i).*Not Defined.*/)) {
		TreeMap<String> userList = state.userList == null ? [:] : state.userList
		userList[userNumber] = userText
		state.userList = userList
		if (dbgEnable)
			log.debug "${device.label}: user: ${userNumber}, name: ${userText}"
	}
}

List<Map> updateFKeyName(String keypadNumber, String keyType, String keyText) {
	List<Map> statusList = null
	if (!(keyText ==~ /(?i).*Not Defined.*/)) {
		String keyName = elkTextDescriptionsTypes[keyType]
		if (dbgEnable)
			log.debug "${device.label}: keypad: ${keypadNumber}, key: ${keyName}, name: ${keyText}"
		statusList = updateKeypadStatus(keypadNumber, [[name: keyName, value: keyText, type: "keypad"]])
	}
	return statusList
}

void createDevice(deviceInfo) {
	String deviceNumber = deviceInfo.deviceNumber
	String deviceName = deviceInfo.deviceName
	String deviceType = deviceInfo.deviceType
	String deviceText = deviceInfo.deviceText
	String deviceNetworkId
	//if (dbgEnable)
	//	log.debug "${device.label}: deviceNumber: ${deviceNumber}, deviceName: ${deviceName}, deviceType: ${deviceType}, deviceText: ${deviceText}"
	if (deviceType == "00") {
		if (deviceName == null) {
			deviceName = "Zone " + deviceNumber + " - " + deviceText
		}
		if (getChildDevice("${device.deviceNetworkId}_C_${deviceNumber}") == null &&
				getChildDevice("{$device.deviceNetworkId}_M_${deviceNumber}") == null &&
				getChildDevice("${device.deviceNetworkId}_Z_${deviceNumber}") == null) {
			deviceNetworkId = "${device.deviceNetworkId}_Z_${deviceNumber}"
			if (deviceName ==~ /(?i).*motion.*/) {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Motion"
				addChildDevice("hubitat", "Virtual Motion Sensor", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
				com.hubitat.app.DeviceWrapper newDevice = getChildDevice(deviceNetworkId)
				newDevice.updateSetting("autoInactive", [type: "enum", value: 0])
			} else if (deviceName ==~ /(?i).*temperature.*/) {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Temperature"
				addChildDevice("hubitat", "Virtual Temperature Sensor", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
				com.hubitat.app.DeviceWrapper newDevice = getChildDevice(deviceNetworkId)
			} else {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Contact"
				addChildDevice("hubitat", "Virtual Contact Sensor", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
				com.hubitat.app.DeviceWrapper newDevice = getChildDevice(deviceNetworkId)
			}
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${device.deviceNetworkId}_Z_${deviceNumber} already exists"
		}
	} else if (deviceType == "03") {
		if (deviceName == null) {
			deviceName = "Keypad ${deviceNumber.substring(1, 3)} - ${deviceText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_P_${deviceNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Keypad"
			try {
				addChildDevice("captncode", "Elk M1 Driver Keypad", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
			} catch (e) {
				addChildDevice("hubitat", "Virtual Temperature Sensor", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
				com.hubitat.app.DeviceWrapper newDevice = getChildDevice(deviceNetworkId)
				newDevice.updateSetting("txtEnable", [value: "false", type: "bool"])
			}
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (deviceType == "04") {
		if (deviceName == null) {
			deviceName = "Output ${deviceNumber} - ${deviceText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_O_${deviceNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Output"
			addChildDevice("belk", "Elk M1 Driver Outputs", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (deviceType == "05") {
		if (deviceName == null) {
			deviceName = "Task ${deviceNumber.substring(1, 3)} - ${deviceText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_K_${deviceNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Task"
			addChildDevice("belk", "Elk M1 Driver Tasks", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (deviceType == "07") {
		int lightNumber = deviceNumber.toInteger()
		int ndx = (lightNumber - 1) / 16
		int unitNumber = lightNumber - ndx * 16
		deviceNumber = "ABCDEFGHIJKLMNOP".substring(ndx, ndx + 1) + String.format("%02d", unitNumber)
		if (deviceName == null) {
			deviceName = "Lighting ${deviceNumber} - ${deviceText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_L_${deviceNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (deviceName ==~ /(?i).*dim.*/) {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Lighting Dimmer"
				addChildDevice("captncode", "Elk M1 Driver Lighting Dimmer", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
			} else {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Lighting Switch"
				addChildDevice("captncode", "Elk M1 Driver Lighting Switch", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
			}
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (deviceType == "09") {
		deviceNumber = deviceNumber.substring(1, 3)
		if (deviceName == null) {
			deviceName = "Custom ${deviceNumber} - ${deviceText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_Y_${deviceNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Custom"
			addChildDevice("captncode", "Elk M1 Driver Custom", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (deviceType == "10") {
		deviceNumber = deviceNumber.substring(1, 3)
		if (deviceName == null) {
			deviceName = "Counter ${deviceNumber} - ${deviceText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_X_${deviceNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Counter"
			addChildDevice("captncode", "Elk M1 Driver Counter", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (deviceType == "11") {
		if (deviceName == null) {
			deviceName = "Thermostat ${deviceNumber} - ${deviceText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_T_${deviceNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Thermostat"
			addChildDevice("belk", "Elk M1 Driver Thermostat", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (deviceType == "18") {
		deviceNumber = deviceNumber.substring(1, 3)
		if (deviceName == null) {
			deviceName = "Audio Zone ${deviceNumber} - ${deviceText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_A_${deviceNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: Audio"
			addChildDevice("captncode", "Elk M1 Driver Audio", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (deviceType == "SP") {
		if (deviceName == null) {
			deviceName = deviceText
		}
		deviceNetworkId = "${device.deviceNetworkId}_S_0"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${deviceName} with deviceNetworkId = ${deviceNetworkId} of type: SpeechSynthesis"
			addChildDevice("captncode", "Elk M1 Driver Text To Speech", deviceNetworkId, [name: deviceName, isComponent: false, label: deviceName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	}
}

void registerZoneReport(String deviceNetworkId, String zoneNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering zone ${zoneNumber} reporting for ${deviceNetworkId}"
	state.zoneReport = registerReport(state.zoneReport, deviceNetworkId, zoneNumber)
}

void unRegisterZoneReport(String deviceNetworkId, String zoneNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering zone ${zoneNumber} reporting for ${deviceNetworkId}"
	state.zoneReport = unRegisterReport(state.zoneReport, deviceNetworkId, zoneNumber)
}

void registerOutputReport(String deviceNetworkId, String outputNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering output ${outputNumber} reporting for ${deviceNetworkId}"
	state.outputReport = registerReport(state.outputReport, deviceNetworkId, outputNumber)
}

void unRegisterOutputReport(String deviceNetworkId, String outputNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering output ${outputNumber} reporting for ${deviceNetworkId}"
	state.outputReport = unRegisterReport(state.outputReport, deviceNetworkId, outputNumber)
}

void registerTaskReport(String deviceNetworkId, String taskNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering task ${taskNumber} reporting for ${deviceNetworkId}"
	state.taskReport = registerReport(state.taskReport, deviceNetworkId, taskNumber)
}

void unRegisterTaskReport(String deviceNetworkId, String taskNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering task ${taskNumber} reporting for ${deviceNetworkId}"
	state.taskReport = unRegisterReport(state.taskReport, deviceNetworkId, taskNumber)
}

HashMap registerReport(HashMap reportList, String deviceNetworkId, String deviceNumber) {
	if (reportList == null) {
		reportList = [:]
	}
	if (reportList[deviceNumber] == null)
		reportList[deviceNumber] = [deviceNetworkId]
	else
		reportList[deviceNumber] << deviceNetworkId
	return reportList
}

HashMap unRegisterReport(HashMap reportList, String deviceNetworkId, String deviceNumber) {
	if (reportList == null) {
		reportList = [:]
	} else if (deviceNumber == null) {
		HashMap<List<String>> newreport = [:]
		reportList.each { fromDevice ->
			fromDevice.value.each {
				if (it != deviceNetworkId)
					newreport = registerReport(newreport, it, fromDevice.key)
			}
		}
		reportList = newreport
	} else if (reportList[deviceNumber] != null) {
		List<String> toList = reportList[deviceNumber]
		reportList.remove(deviceNumber)
		if (deviceNetworkId != null) {
			toList.each {
				if (it != deviceNetworkId)
					reportList = registerReport(reportList, it, deviceNumber)
			}
		}
	}
	if (reportList.size() == 0)
		reportList = null
	return reportList
}

HashMap sendReport(HashMap reportList, reportDevice, String deviceNumber, boolean violated) {
	List<String> toList = reportList[deviceNumber]
	if (toList != null) {
		toList.each {
			com.hubitat.app.DeviceWrapper otherChild = getChildDevice(it)
			if (otherChild != null && otherChild.hasCommand("report")) {
				otherChild.report(reportDevice.deviceNetworkId, violated)
			} else {
				reportList = unRegisterReport(reportList, it, deviceNumber)
			}
		}
	}
	return reportList
}

//Telnet
List<hubitat.device.HubAction> telnetTimeout() {
	telnetStatus("timeout")
}

int getReTry(boolean inc) {
	int reTry = (state.reTryCount ?: 0).toInteger()
	if (inc) reTry++
	state.reTryCount = reTry
	return reTry
}

List<hubitat.device.HubAction> telnetStatus(String status) {
	log.warn "${device.label} telnetStatus error: ${status}"
	if (status == "receive error: Stream is closed" || status == "send error: Broken pipe (Write failed)" || status == "timeout") {
		getReTry(true)
		log.error "${device.label} Telnet connection dropped..."
		log.warn "${device.label} Telnet is restarting..."
		initialize()
	}
}

////REFERENCES AND MAPPINGS////
// Key Mapping Readable Text
@Field static final String Off = "off"
@Field static final String On = "on"
@Field static final String Clear = "clear"
@Field static final String Detected = "detected"
@Field static final String Blinking = "blinking"
@Field static final String Chimed = "chimed"
@Field static final String Beeped = "beeped"
@Field static final String Beeping = "beeping"

@Field final Map elkStates = [
		"0": Off,
		"1": On,
		"2": Blinking
]

@Field static final String Tone = "tone"
@Field static final String Voice = "voice"
@Field static final String ToneVoice = "tone/voice"
@Field final Map elkChimeStates = [
		"0": Off,
		"1": Tone,
		"2": Voice,
		"3": ToneVoice
]

@Field static final String NOTHING = ""
@Field static final String STARKEY = "*"
@Field static final String POUNDKEY = "#"
@Field static final String F1KEY = "1"
@Field static final String F2KEY = "2"
@Field static final String F3KEY = "3"
@Field static final String F4KEY = "4"
@Field static final String STAYKEY = "Stay"
@Field static final String EXITKEY = "Exit"
@Field static final String CHIMEKEY = "Chime"
@Field static final String BYPASSKEY = "Bypass"
@Field static final String MENUKEY = "Menu"
@Field static final String DOWNKEY = "Down"
@Field static final String UPKEY = "Up"
@Field static final String RIGHTKEY = "Right"
@Field static final String LEFTKEY = "Left"
@Field static final String F6KEY = "6"
@Field static final String F5KEY = "5"
@Field static final String DATAKEYMODE = "Data"

// Event Mapping
@Field final Map elkKeys = [
		'00': NOTHING,
		'11': STARKEY,
		'12': POUNDKEY,
		'13': F1KEY,
		'14': F2KEY,
		'15': F3KEY,
		'16': F4KEY,
		'17': STAYKEY,
		'18': EXITKEY,
		'19': CHIMEKEY,
		'20': BYPASSKEY,
		'21': MENUKEY,
		'22': DOWNKEY,
		'23': UPKEY,
		'24': RIGHTKEY,
		'25': LEFTKEY,
		'26': F6KEY,
		'27': F5KEY,
		'28': DATAKEYMODE
]

@Field final Map elkFKeys = [
		'*': STARKEY,
		'0': NOTHING,
		'1': F1KEY,
		'2': F2KEY,
		'3': F3KEY,
		'4': F4KEY,
		'5': F5KEY,
		'6': F6KEY,
		'C': CHIMEKEY
]
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

@Field final Map elkArmStatuses = [
		'0': Disarmed,
		'1': ArmedAway,
		'2': ArmedHome,
		'3': ArmedHomeInstant,
		'4': ArmedNight,
		'5': ArmedNightInstant,
		'6': ArmedVacation
]

@Field final Map elkArmingStatuses = [
		'0': Disarmed,
		'1': ArmingAway,
		'2': ArmingHome,
		'3': ArmingHomeInstant,
		'4': ArmingNight,
		'5': ArmingNightInstant,
		'6': ArmingVacation
]
@Field static final String NotReadytoArm = "Not Ready to Arm"
@Field static final String ReadytoArm = "Ready to Arm"
@Field static final String ReadytoArmBut = "Ready to Arm, but a zone is violated and can be force armed"
@Field static final String ArmedwithExit = "Armed with Exit Timer working"
@Field static final String ArmedFully = "Armed Fully"
@Field static final String ForceArmed = "Force Armed with a force arm zone violated"
@Field static final String ArmedwithaBypass = "Armed with a Bypass"

@Field final Map elkArmUpStates = [
		'0': NotReadytoArm,
		'1': ReadytoArm,
		'2': ReadytoArmBut,
		'3': ArmedwithExit,
		'4': ArmedFully,
		'5': ForceArmed,
		'6': ArmedwithaBypass
]

@Field static final String NoActiveAlarm = "No Active Alarm"
@Field static final String EntranceDelayisActive = "Entrance Delay is Active"
@Field static final String AlarmAbortDelayActive = "Alarm Abort Delay Active"
@Field static final String FireAlarm = "Fire Alarm"
@Field static final String MedicalAlarm = "Medical Alarm"
@Field static final String PoliceAlarm = "Police Alarm"
@Field static final String BurgularAlarm = "Burgular Alarm"
@Field static final String AuxAlarm1 = "Aux Alarm 1"
@Field static final String AuxAlarm2 = "Aux Alarm 2"
@Field static final String AuxAlarm3 = "Aux Alarm 3"
@Field static final String AuxAlarm4 = "Aux Alarm 4"
@Field static final String CarbonMonoxide = "Carbon Monoxide"
@Field static final String EmergencyAlarm = "Emergency Alarm"
@Field static final String FreezeAlarm = "Freeze Alarm"
@Field static final String GasAlarm = "Gas Alarm"
@Field static final String HeatAlarm = "Heat Alarm"
@Field static final String WaterAlarm = "Water Alarm"
@Field static final String FireSupervisory = "Fire Supervisory"
@Field static final String FireVerified = "Fire Verified"

@Field final Map elkAlarmStates = [
		'0': NoActiveAlarm,
		'1': EntranceDelayisActive,
		'2': AlarmAbortDelayActive,
		'3': FireAlarm,
		'4': MedicalAlarm,
		'5': PoliceAlarm,
		'6': BurgularAlarm,
		'7': AuxAlarm1,
		'8': AuxAlarm2,
		'9': AuxAlarm3,
		':': AuxAlarm4,
		';': CarbonMonoxide,
		'<': EmergencyAlarm,
		'=': FreezeAlarm,
		'>': GasAlarm,
		'?': HeatAlarm,
		'@': WaterAlarm,
		'A': FireSupervisory,
		'B': FireVerified,
		'D': BurglarBoxTamper
]

// Zone Status Mapping Readable Text
@Field static final String NormalUnconfigured = "Normal: Unconfigured"
@Field static final String NormalOpen = "Normal: Open"
@Field static final String NormalEOL = "Normal: EOL"
@Field static final String NormalShort = "Normal: Short"
@Field static final String TroubleOpen = "Trouble: Open"
@Field static final String TroubleEOL = "Trouble: EOL"
@Field static final String TroubleShort = "Trouble: Short"
@Field static final String ViolatedOpen = "Violated: Open"
@Field static final String ViolatedEOL = "Violated: EOL"
@Field static final String ViolatedShort = "Violated: Short"
@Field static final String SoftBypassed = "Soft Bypassed"
@Field static final String BypassedOpen = "Bypassed: Open"
@Field static final String BypassedEOL = "Bypassed: EOL"
@Field static final String BypassedShort = "Bypassed: Short"

// Zone Status Mapping
@Field final Map elkZoneStatuses = [
		'0': NormalUnconfigured,
		'1': NormalOpen,
		'2': NormalEOL,
		'3': NormalShort,
		'5': TroubleOpen,
		'6': TroubleEOL,
		'7': TroubleShort,
		'9': ViolatedOpen,
		'A': ViolatedEOL,
		'B': ViolatedShort,
		'C': SoftBypassed,
		'D': BypassedOpen,
		'E': BypassedEOL,
		'F': BypassedShort

]

@Field final Map capabilitiesViolated = [
		"AccelerationSensor"    : [name: "acceleration", value: "active"],
		"Beacon"                : [name: "presence", value: "present"],
		"CarbonMonoxideDetector": [name: "carbonMonoxide", value: "detected"],
		"ContactSensor"         : [name: "contact", value: "open"],
		"DoorControl"           : [name: "door", value: "open"],
		"GarageDoorControl"     : [name: "door", value: "open"],
		"MotionSensor"          : [name: "motion", value: "active"],
		"PresenceSensor"        : [name: "presence", value: "present"],
		"RelaySwitch"           : [name: "switch", value: "on"],
		"ShockSensor"           : [name: "shock", value: "detected"],
		"SleepSensor"           : [name: "sleeping", value: "sleeping"],
		"SmokeDetector"         : [name: "smoke", value: "detected"],
		"SoundSensor"           : [name: "sound", value: "detected"],
		"Switch"                : [name: "switch", value: "on"],
		"TamperAlert"           : [name: "tamper", value: "detected"],
		"TouchSensor"           : [name: "touch", value: "touched", isStateChange: true],
		"Valve"                 : [name: "valve", value: "open"],
		"WaterSensor"           : [name: "water", value: "wet"]
]

@Field final Map capabilitiesNormal = [
		"AccelerationSensor"    : [name: "acceleration", value: "inactive"],
		"Beacon"                : [name: "presence", value: "not present"],
		"CarbonMonoxideDetector": [name: "carbonMonoxide", value: "clear"],
		"ContactSensor"         : [name: "contact", value: "closed"],
		"DoorControl"           : [name: "door", value: "closed"],
		"GarageDoorControl"     : [name: "door", value: "closed"],
		"MotionSensor"          : [name: "motion", value: "inactive"],
		"PresenceSensor"        : [name: "presence", value: "not present"],
		"RelaySwitch"           : [name: "switch", value: "off"],
		"ShockSensor"           : [name: "shock", value: "clear"],
		"SleepSensor"           : [name: "sleeping", value: "not sleeping"],
		"SmokeDetector"         : [name: "smoke", value: "clear"],
		"SoundSensor"           : [name: "sound", value: "not detected"],
		"Switch"                : [name: "switch", value: "off"],
		"TamperAlert"           : [name: "tamper", value: "clear"],
		"TouchSensor"           : [name: "touch", value: null, isStateChange: true],
		"Valve"                 : [name: "valve", value: "closed"],
		"WaterSensor"           : [name: "water", value: "dry"]
]

@Field static final String ZoneName = "Zone Name"
@Field static final String AreaName = "Area Name"
@Field static final String UserName = "User Name"
@Field static final String Keypad = "Keypad"
@Field static final String OutputName = "Output Name"
@Field static final String TaskName = "Task Name"
@Field static final String TelephoneName = "Telephone Name"
@Field static final String LightName = "Light Name"
@Field static final String AlarmDurationName = "Alarm Duration Name"
@Field static final String CustomSettings = "Custom Settings"
@Field static final String CountersNames = "Counters Names"
@Field static final String ThermostatNames = "Thermostat Names"
@Field static final String FunctionKey1Name = "button1"
@Field static final String FunctionKey2Name = "button2"
@Field static final String FunctionKey3Name = "button3"
@Field static final String FunctionKey4Name = "button4"
@Field static final String FunctionKey5Name = "button5"
@Field static final String FunctionKey6Name = "button6"
@Field static final String AudioZoneName = "Audio Zone Name"
@Field static final String AudioSourceName = "Audio Source Name"

@Field final Map elkTextDescriptionsTypes = [
		'00': ZoneName,
		'01': AreaName,
		'02': UserName,
		'03': Keypad,
		'04': OutputName,
		'05': TaskName,
		'06': TelephoneName,
		'07': LightName,
		'08': AlarmDurationName,
		'09': CustomSettings,
		'10': CountersNames,
		'11': ThermostatNames,
		'12': FunctionKey1Name,
		'13': FunctionKey2Name,
		'14': FunctionKey3Name,
		'15': FunctionKey4Name,
		'16': FunctionKey5Name,
		'17': FunctionKey6Name,
		'18': AudioZoneName,
		'19': AudioSourceName
]

@Field final Map elkTextDescriptionsMax = [
		'00': 208, // ZoneName
		'01': 8,   // AreaName
		'02': 199, // UserName
		'03': 16,  // Keypad
		'04': 64,  // OutputName
		'05': 32,  // TaskName
		'06': 8,   // TelephoneName
		'07': 256, // LightName
		'08': 12,  // AlarmDurationName
		'09': 20,  // CustomSettings
		'10': 64,  // CountersNames
		'11': 16,  // ThermostatNames
		'12': 16,  // FunctionKey1Name
		'13': 16,  // FunctionKey2Name
		'14': 16,  // FunctionKey3Name
		'15': 16,  // FunctionKey4Name
		'16': 16,  // FunctionKey5Name
		'17': 16,  // FunctionKey6Name
		'18': 18,  // AudioZoneName
		'19': 12   // AudioSourceName
]

@Field static final String TemperatureProbe = "Temperature Probe"
@Field static final String Keypads = "Keypads"
@Field static final String Thermostats = "Thermostats"

@Field final Map elkTempTypes = [
		0: TemperatureProbe,
		1: Keypads,
		2: Thermostats
]

@Field final Map elkCommands = [
		Disarm                 : "a0",
		ArmAway                : "a1",
		ArmHome                : "a2",
		ArmHomeInstant         : "a3",
		ArmNight               : "a4",
		ArmNightInstant        : "a5",
		ArmVacation            : "a6",
		ArmNextAway            : "a7",
		ArmNextHome            : "a8",
		ArmForceAway           : "a9",
		ArmForceHome           : "a:",
		RequestUserChange      : "cu",
		RequestUserArea        : "ua",
		RequestArmStatus       : "as",
		RequestTemperatureData : "lw",
		RequestTextDescriptions: "sd",
		RequestTroubleStatus   : "ss",
		RequestAlarmStatus     : "az",
		RequestVersionNumber   : "vn",
		RequestZoneDefinitions : "zd",
		RequestZoneStatus      : "zs",
		RequestLightingStatus  : "ps",
		RequestKeypadArea      : "ka",
		RequestKeypadPress     : "kf",
		RequestKeypadStatus    : "kc",
		RequestOutputStatus    : "cs",
		RequestAllCustomValues : "cp",
		RequestCustomValue     : "cr",
		RequestCounterValue    : "cv",
		TaskActivation         : "tn",
		SetCounterValue        : "cx",
		SetCustomValue         : "cw",
		ZoneBypass             : "zb",
		ZoneTrigger            : "zt",
		ZoneVoltage            : "zv",
		ControlOutputOn        : "cn",
		ControlOutputOff       : "cf",
		ControlOutputToggle    : "ct",
		ControlLightingMode    : "pc",
		ControlLightingOn      : "pn",
		ControlLightingOff     : "pf",
		ControlLightingToggle  : "pt",
		SpeakWord              : "sw",
		SpeakPhrase            : "sp",
		ShowTextOnKeypads      : "dm"
]

@Field static final String Disabled = "Disabled"
@Field static final String BurglarEntryExit1 = "Burglar Entry/Exit 1"
@Field static final String BurglarEntryExit2 = "Burglar Entry/Exit 2"
@Field static final String BurglarPerimeterInstant = "Burglar Perimeter Instant"
@Field static final String BurglarInterior = "Burglar Interior"
@Field static final String BurglarInteriorFollower = "Burglar Interior Follower"
@Field static final String BurglarInteriorNight = "Burglar Interior Night"
@Field static final String BurglarInteriorNightDelay = "Burglar Interior Night Delay"
@Field static final String Burglar24Hour = "Burglar 24 Hour"
@Field static final String BurglarBoxTamper = "Burglar Box Tamper"
@Field static final String Keyfob = "Key fob"
@Field static final String NonAlarm = "Non Alarm"
@Field static final String PoliceNoIndication = "Police No Indication"
@Field static final String KeyMomentaryArmDisarm = "Key Momentary Arm / Disarm"
@Field static final String KeyMomentaryArmAway = "Key Momentary Arm Away"
@Field static final String KeyMomentaryArmHome = "Key Momentary Arm Home"
@Field static final String KeyMomentaryDisarm = "Key Momentary Disarm"
@Field static final String KeyOnOff = "Key On/Off"
@Field static final String MuteAudibles = "Mute Audibles"
@Field static final String PowerSupervisory = "Power Supervisory"
@Field static final String Temperature = "Temperature"
@Field static final String AnalogZone = "Analog Zone"
@Field static final String PhoneKey = "Phone Key"
@Field static final String IntercomKey = "Intercom Key"

@Field final Map elkZoneDefinitions = [
		'0': Disabled,
		'1': BurglarEntryExit1,
		'2': BurglarEntryExit2,
		'3': BurglarPerimeterInstant,
		'4': BurglarInterior,
		'5': BurglarInteriorFollower,
		'6': BurglarInteriorNight,
		'7': BurglarInteriorNightDelay,
		'8': Burglar24Hour,
		'9': BurglarBoxTamper,
		':': FireAlarm,
		';': FireVerified,
		'<': FireSupervisory,
		'=': AuxAlarm1,
		'>': AuxAlarm2,
		'?': Keyfob,
		'@': NonAlarm,
		'A': CarbonMonoxide,
		'B': EmergencyAlarm,
		'C': FreezeAlarm,
		'D': GasAlarm,
		'E': HeatAlarm,
		'F': MedicalAlarm,
		'G': PoliceAlarm,
		'H': PoliceNoIndication,
		'I': WaterAlarm,
		'J': KeyMomentaryArmDisarm,
		'K': KeyMomentaryArmAway,
		'L': KeyMomentaryArmHome,
		'M': KeyMomentaryDisarm,
		'N': KeyOnOff,
		'O': MuteAudibles,
		'P': PowerSupervisory,
		'Q': Temperature,
		'R': AnalogZone,
		'S': PhoneKey,
		'T': IntercomKey
]

//Not currently using this
//@Field static final String Disabled = "Disabled"
@Field static final String ContactBurglarEntryExit1 = "Contact"
@Field static final String ContactBurglarEntryExit2 = "Contact"
@Field static final String ContactBurglarPerimeterInstant = "Contact"
@Field static final String MotionBurglarInterior = "Motion"
@Field static final String MotionBurglarInteriorFollower = "Motion"
@Field static final String MotionBurglarInteriorNight = "Motion"
@Field static final String MotionBurglarInteriorNightDelay = "Motion"
@Field static final String AlertBurglar24Hour = "Alert"
@Field static final String AlertBurglarBoxTamper = "Alert"
@Field static final String AlertFireAlarm = "Alert"
@Field static final String AlertFireVerified = "Alert"
@Field static final String AlertFireSupervisory = "Alert"
@Field static final String AlertAuxAlarm1 = "Alert"
@Field static final String AlertAuxAlarm2 = "Alert"
@Field static final String AlertCarbonMonoxide = "Alert"
@Field static final String AlertEmergencyAlarm = "Alert"
@Field static final String AlertFreezeAlarm = "Alert"
@Field static final String AlertGasAlarm = "Alert"
@Field static final String AlertHeatAlarm = "Alert"
@Field static final String AlertMedicalAlarm = "Alert"
@Field static final String AlertPoliceAlarm = "Alert"
@Field static final String AlertPoliceNoIndication = "Alert"
@Field static final String AlertWaterAlarm = "Alert"

//Not currently using this
@Field final Map elkZoneTypes = [
//'0': Disabled,
'1': ContactBurglarEntryExit1,
'2': ContactBurglarEntryExit2,
'3': ContactBurglarPerimeterInstant,
'4': MotionBurglarInterior,
'5': MotionBurglarInteriorFollower,
'6': MotionBurglarInteriorNight,
'7': MotionBurglarInteriorNightDelay,
'8': AlertBurglar24Hour,
'9': AlertBurglarBoxTamper,
':': AlertFireAlarm,
';': AlertFireVerified,
'<': AlertFireSupervisory,
'=': AlertAuxAlarm1,
'>': AlertAuxAlarm2,
'A': AlertCarbonMonoxide,
'B': AlertEmergencyAlarm,
'C': AlertFreezeAlarm,
'D': AlertGasAlarm,
'E': AlertHeatAlarm,
'F': AlertMedicalAlarm,
'G': AlertPoliceAlarm,
'H': AlertPoliceNoIndication,
'I': AlertWaterAlarm
]

@Field static final String ACFailTrouble = "AC Fail Trouble"
@Field static final String BoxTamperTrouble = "Box Tamper Trouble"
@Field static final String FailToCommunicateTrouble = "Fail To Communicate Trouble"
@Field static final String EEPromMemoryErrorTrouble = "EEProm Memory Error Trouble"
@Field static final String LowBatteryControlTrouble = "Low Battery Control Trouble"
@Field static final String TransmitterLowBatteryTrouble = "Transmitter Low Battery Trouble"
@Field static final String OverCurrentTrouble = "Over Current Trouble"
@Field static final String TelephoneFaultTrouble = "Telephone Fault Trouble"
@Field static final String Output2Trouble = "Output 2 Trouble"
@Field static final String MissingKeypadTrouble = "Missing Keypad Trouble"
@Field static final String ZoneExpanderTrouble = "Zone Expander Trouble"
@Field static final String OutputExpanderTrouble = "Output Expander Trouble"
@Field static final String ELKRPRemoteAccess = "ELKRP Remote Access"
@Field static final String CommonAreaNotArmed = "Common Area Not Armed"
@Field static final String FlashMemoryErrorTrouble = "Flash Memory Error Trouble"
@Field static final String SecurityAlert = "Security Alert"
@Field static final String SerialPortExpanderTrouble = "Serial Port Expander Trouble"
@Field static final String LostTransmitterTrouble = "Lost Transmitter Trouble"
@Field static final String SmokeCleanMeTrouble = "Smoke Clean Me Trouble"
@Field static final String EthernetTrouble = "Ethernet Trouble"
@Field static final String DisplayMessageInKeypadLine1 = "Display Message In Keypad Line 1"
@Field static final String DisplayMessageInKeypadLine2 = "Display Message In Keypad Line 2"
@Field static final String FireTrouble = "Fire Trouble"


@Field final Map elkTroubleCodes = [
		0 : ACFailTrouble,
		1 : BoxTamperTrouble,
		2 : FailToCommunicateTrouble,
		3 : EEPromMemoryErrorTrouble,
		4 : LowBatteryControlTrouble,
		5 : TransmitterLowBatteryTrouble,
		6 : OverCurrentTrouble,
		7 : TelephoneFaultTrouble,
		9 : Output2Trouble,
		10: MissingKeypadTrouble,
		11: ZoneExpanderTrouble,
		12: OutputExpanderTrouble,
		14: ELKRPRemoteAccess,
		16: CommonAreaNotArmed,
		17: FlashMemoryErrorTrouble,
		18: SecurityAlert,
		19: SerialPortExpanderTrouble,
		20: LostTransmitterTrouble,
		21: SmokeCleanMeTrouble,
		22: EthernetTrouble,
		31: DisplayMessageInKeypadLine1,
		32: DisplayMessageInKeypadLine2,
		33: FireTrouble
]

@Field final Map elkLogData = [
		1111: "Code Lockout, Any Keypad",
		1112: "Keypad 01 Code-Lockout",
		1113: "Keypad 02 Code-Lockout",
		1114: "Keypad 03 Code-Lockout",
		1115: "Keypad 04 Code-Lockout",
		1116: "Keypad 05 Code-Lockout",
		1117: "Keypad 06 Code-Lockout",
		1118: "Keypad 07 Code-Lockout",
		1119: "Keypad 08 Code-Lockout",
		1120: "Keypad 09 Code-Lockout",
		1121: "Keypad 10 Code-Lockout",
		1122: "Keypad 11 Code-Lockout",
		1123: "Keypad 12 Code-Lockout",
		1124: "Keypad 13 Code-Lockout",
		1125: "Keypad 14 Code-Lockout",
		1126: "Keypad 15 Code-Lockout",
		1127: "Keypad 16 Code-Lockout",
		1131: "RF Sensor Low Battery",
		1132: "Lost Anc Module Trouble",
		1141: "Expansion Module Trouble",
		1161: "Expansion Module Restore",
		1293: "Automatic Closing",
		1294: "Early Closing",
		1295: "Closing Time Extended",
		1296: "Fail To Close",
		1297: "Late To Close",
		1298: "Keyswitch Closing",
		1299: "Duress",
		1300: "Exception Opening",
		1301: "Early Opening",
		1302: "Fail To Open",
		1303: "Late To Open",
		1304: "Keyswitch Opening",
		1313: "Access Keypad 01",
		1314: "Access Keypad 02",
		1315: "Access Keypad 03",
		1316: "Access Keypad 04",
		1317: "Access Keypad 05",
		1318: "Access Keypad 06",
		1319: "Access Keypad 07",
		1321: "Access Keypad 09",
		1322: "Access Keypad 10",
		1323: "Access Keypad 11",
		1324: "Access Keypad 12",
		1325: "Access Keypad 13",
		1326: "Access Keypad 14",
		1327: "Access Keypad 15",
		1328: "Access Keypad 16",
		1329: "Access Any Keypad",
		1338: "Area 1 Exit Error",
		1339: "Area 2 Exit Error",
		1340: "Area 3 Exit Error",
		1341: "Area 4 Exit Error",
		1342: "Area 5 Exit Error",
		1343: "Area 6 Exit Error",
		1344: "Area 7 Exit Error",
		1345: "Area 8 Exit Error",
		1351: "Dialer Abort",
		1352: "Dialer Cancel",
		1353: "Dialer Auto Test",
		1356: "Keyswitch Zone Tamper Alert",
		1358: "Telephone Line Is Ringing",
		1359: "Telephone Line Seize",
		1360: "Telephone Line Off/On Hook",
		1361: "Telephone Local Access",
		1362: "Telephone Remote Access",
		1365: "AC Fail Trouble Zone",
		1366: "Low Battery Trouble Zone",
		1367: "System Start Up",
		1368: "Control Low Voltage Shutdown",
		1369: "RF Keyfob Button 1",
		1370: "RF Keyfob Button 2",
		1371: "RF Keyfob Button 3",
		1372: "RF Keyfob Button 4",
		1373: "RF Keyfob Button 5",
		1374: "RF Keyfob Button 6",
		1375: "RF Keyfob Button 7",
		1376: "RF Keyfob Button 8",
		1378: "Rule Triggered Voice Telephone Dial",
		1379: "Dialer Report Cleared",
		1380: "Central Station Kissoff",
		1381: "Transmitter Supervision Loss",
		1385: "Restore AC Power Zone",
		1386: "Restore Battery Zone"
]

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.2.6
 * Fixed issue with Zone Bypass not working.
 * Fixed issue with arming while armed caused panel to disarm.
 * Fixed issue with armState briefly showing "Armed Fully" during an exit delay.  It comes from the Elk M1 this way
 *       so a work-around was put in place.
 * Fixed issue with descriptionText not always having the device name in it.
 * Fixed issue with the alarm attribute not always getting set.
 * Fixed issue with showTextOnKeypad not showing a line of text if it was exactly 16 characters long.
 * Changed alarm and trouble attributes from on/off to detected/clear.
 * Changed alarm, armState, armStatus, beep, chime, chimeMode, trouble and the Function key LED attributes to ENUM so
 *       the values can be selected in the rules engine.
 * Changed invalidUser event to show on parent Elk device regardless of keypad it was entered on just like lastUser.
 * Changed Switch Capability to Lock Capability for Dashboard arming/disarming and for the Amazon Alexa skill.
 * Improved armStatus, lastUser, lock and securityKeypad events descriptionText to contain user name.
 * Renamed Stay mode to Home mode to align with Hubitat terminology.
 * Moved Set Location Mode to Elk Application for full user control.
 * Added SecurityKeypad Capability.
 * Added getCodes, setCode and deleteCode commands for compatibility with Lock Code Manager.
 * Added armingIn attribute to show exit delay.
 * Added missing invalidUser attribute.
 * Added ability to shut down connection to the Elk M1 by setting the keypad number to 0.
 * Added smart refresh feature controlled from the application to refresh child devices in a more controlled way
 *       to address issues when reconnecting to the panel especially after the panel has been powered down and back up.
 * Removed thermostat commands and moved them to the thermostat driver.
 * Removed armMode attribute.  Between securityKeypad and armStatus, it should be unnecessary.
 * Removed TemperatureMeasurement capability.  It was conflicting with the Amazon Echo skill.
 * Now retrieves Fahrenheit or Celsius from the Elk instead of a user preference setting.
 *
 * 0.2.5
 * Fixed an issue with log data and troubles not always showing.
 * Changed trouble attribute from true/false to on/off.
 * Changed invalidUser attribute to show 6 digit user code or 12 digit access credentials for prox cards/iButton.
 * Removed limits with zone, output and task reporting only able to report to one child device.
 *
 * 0.2.4
 * Fixed an issue with a keypad device that is a Virtual Temperature Probe.
 *
 * 0.2.3
 * Added Counter and Custom value device types.
 * Fixed long standing issue with checksums on Elk commands.  1% of the checksums were generating wrong causing
 *       those commands to quietly fail.
 * Fixed incorrectly named command refreshSystemTroubleStatus.  Renamed it to refreshTroubleStatus.
 * Fixed errors with a keypad device that is a Virtual Temperature Probe.
 *
 * 0.2.2
 * Added setting to control if switch is turned on when arming or when fully armed.
 *
 * 0.2.1
 * Added setThermostatTemperature to thermostat.
 * Fixed bug with trouble code.
 * Changed thermostatHoldMode from true and false to yes and no.
 *
 * 0.2.0
 * Added keypad functionality such as chime and temperature measurement by assigning a keypad number
 *       to this device.  Device area is derived from the area the keypad is assigned to.
 * Added Function 1-6 keys as pushable buttons and retrieving of their description.
 * Added Function key LED status.
 * Added keypad beep, chime and chime mode status.
 * Added alarm and system trouble status.
 * Added request of zone voltage to be written to info log.
 * Added speak word/phrase using the Elk word or phrase number.
 * Added showTextOnKeypads to display a message on all keypads.
 * Added zone bypass and zone trigger commands.
 * Added setting if temperatures from the panel is in C or F.
 * Added setting to control if you want the hub's location set when the panel is armed/disarmed.
 * Added settings to control what arming or disarming happens when the switch assigned to this
 *       device is turned on or off.  Primarily for use by the dashboard or Google/Alexa.
 * Enhanced logging setting to allow control of whether Area or Keypad events get logged.
 * Added sendEmail event when an email is sent by the panel.
 * Added warning log when certain data is written to the panel log.
 * Added warning log on zone devices when a zone is in alarm.
 * Added status event on zone devices when a zone has a trouble code.
 * Added pushed status when certain keys are pressed a the keypad.
 * Added invalidUser status when an invalid user code is entered on a keypad.  This could be used
 *       to trigger rules based on a specific invalid user code being entered.
 * Added refresh if Elk RP programming occurs and warning log of Elk RP connections.
 * Added descriptions to events for clarity.
 * Added "arming" armStatus event when panel is armed and the exit delay has not yet expired.
 * Added calls to Elk application when arm/disarm and exit delay events occur for HSM, TTS and notifications.
 * Added warning log when alarm reporting occurs.
 * Added hooks for separate custom Elk Keypad and Speech drivers.
 * Changed task to be a momentary pushable button instead of a switch.
 *
 * 0.1.34
 * Added a telnet timeout feature to re-initialize device if the panel's IP Test message is not received when expected
 * Added armMode attribute
 * Rename armHome command to armStay
 * Removed processing of any Log Data Update report due to not being reliable for real time results
 *
 * 0.1.33
 * Added import of lighting devices for use with new drivers.  If lighting text has "dim" in it, it will be assigned the dimmer
 *   driver.  Otherwise, it will be assigned the switch driver.
 * Fixed issue with M1 Touch Pro App sync conflict that caused this device to crash
 * Fixed issue with thermostats not working
 * Removed redundant output and task command buttons.  The individual devices now have them.
 * Renamed the Request xxx Status command buttons to Refresh xxx Status for accuracy
 * Stopped zone statuses from writing info log when the status didn't change
 * Added retrieval of Elk M1 Version after initialization
 *
 * 0.1.32
 * Fixed issue with temperature readings.
 *
 * 0.1.31
 * Added handling of Temperature Data automatically sent starting with M1 Ver. 4.2.8
 * Added info logging only of Zone Definitions
 * Simplified thermostat code
 * Simplified Output and Task logging and events
 * Removed unneeded thermostat capability from this device since the child thermostat device has it
 *
 * 0.1.30
 * Added polling of device status once connected or reconnected to the panel.
 * Added Enable debug logging and Enable descriptionText logging to the main device.  Debug is no longer set for the
 *   device from within the application.  Info logging can now be turned on or off for the main device.
 * Added zone, output and task reporting so child devices can register to be updated via the report command when another
 *   device status has changed.
 *   I am using this for a door control driver assigned to an output that needs to be aware of the state of the contact
 *   attached to the door.
 *
 * 0.1.29
 * Strongly typed variables for performance
 *
 * 0.1.28
 * Added ability to handle a zone with a Temperature Probe attached to the main board. Automatically assigned when
 *   the zone's description has the word "Temperature" in it or zone device can be manually changed.
 * Added the device type Keypad for temperature readings.
 * Both devices are assigned the Virtual Temperature Sensor driver.  This is an experimental feature.  Since the panel
 *   does not volunteer temperature data, it must be requested either manually for all devices using the Request Temperature
 *   Data button on the main device or by setting up a rule to periodically execute the refreshTemperatureStatus command.
 * Added "ContactSensor" as a capability for HSM monitoring.  The contact will open if a Burglar Alarm or Police Alarm is triggered.
 * Changed the method of importing devices to greatly improve performance and reduce panel communication.
 * Fixed an issue not deleting child devices when the main device is uninstalled.
 * Fixed an issue with the name when importing a device with the "Show On Keypad" flag set.
 *
 * 0.1.27
 * You can now change the port on the main device page.  You must click "Initialize" after you save preferences to take effect
 * Changed info logging to only happen if Enable descriptionText logging is turned on the device
 * Re-enabled Request Zone Definition and Request Zone Status
 * Added Request Output Status
 *
 * 0.1.26
 * Added info logging when output or task status changes or Arm Mode changes
 * Added switch capability to main Elk M1 device
 * Improved armStatus and alarmState events to fire only when changed
 * Adding missing alarmStates
 * Small tweaks to improve performance
 *
 * 0.1.25
 * Added sync of Elk M1 modes to Location Modes: Disarmed, Armed Away, Night, Stay, Vacation synced to modes Home, Away, Night,
 *    Stay (if available and Home if not), Vacation (if available and Away if not), respectively.
 * Added sync of hsmSetArm modes of disarm, armAway, armNight, ArmStay
 * Retooled zone-device creation to always create a device of type Virtual Contact unless "motion" is in the description.
 * Fixed issue that would create a lot of bogus devices when you connect to the panel with the M1 Touch Pro app
 * Added auto zone-device capability detection to support virtual devices with the following capabilities:
 *     ContactSensor, MotionSensor, SmokeDetector, CarbonMonoxideDetector, WaterSensor, TamperAlert, AccelerationSensor,
 *     Beacon, PresenceSensor, RelaySwitch, ShockSensor, SleepSensor, SoundSensor, Switch, TouchSensor and Valve
 * Changed registration of arm mode event to happen upon entry/exit timer expiration
 * Fixed issue of extra arm mode events firing when the panel is armed
 * Fixed status of EOL terminated zones
 * Added ability to read task status from the panel and set Contact device to open temporarily.
 * Tested use of the Elk C1M1 dual path communicator over local ethernet instead of using the M1XEP
 * Added setting of lastUser event which contains the user number who last triggered armed or disarmed
 *
 * 0.1.24
 * Rerouted some code for efficiency
 * Turned off some of the extra debugging
 *
 * 0.1.23
 * Outputs are now functional with Rule Machine
 * Change switch case to else if statements
 *
 * 0.1.22
 * Fixed code to show operating state and thermostat set point on dashboard tile
 * Reorder some code to see if it helps with some delay issues
 * Consolidated code for zone open and zone closed to see if it helps with some delay issues (need to check if this has any other impact elsewhere)
 *
 * 0.1.21
 * Updated mapping for output reporting code
 * Changed Reply Arming Status Report Data to work as Area 1 only and to report current states
 *
 * 0.1.20
 * Added back some code for 'Reply Arming Status Report Data (AS)' to clean up logging
 *
 * 0.1.19
 * Removed some code for 'Reply Arming Status Report Data (AS)' to clean up logging
 *
 * 0.1.18
 * Add support for Occupancy Sensors - this will be a work in progress since not sure how to code it
 *
 * 0.1.17
 * Changed devices 'isComponent' to 'False' - this will allow the removal of devices and changing of drivers
 *
 * 0.1.16
 * Changed the one import to be not case sensitive
 *
 * 0.1.15
 * Added support for manual inclusion of Elk M1 outputs and tasks
 * Added support for importing Elk M1 outputs, tasks and thermostats
 * Added better support for child devices (all communication goes through Elk M1 device)
 * Cleaned up some descriptions and instructions
 *
 * 0.1.14
 * Added support for importing Elk M1 zones
 * Fixed erroneous error codes
 * Added actuator capability to allow custom commands to work in dashboard and rule machine
 * Added command to request temperature data
 *
 * 0.1.13
 * Elk M1 Code - No longer requires a 6 digit code (Add leading zeroes to 4 digit codes)
 * Outputs and tasks can now be entered as a number
 * Code clean up - removed some unused code
 *
 * 0.1.12
 * Added support for outputs
 *
 * 0.1.11
 * Built separate thermostat child driver should allow for multiple thermostats
 *
 * 0.1.10
 * Ability to control thermostat 1
 *
 * 0.1.9
 * Minor changes
 *
 * 0.1.8
 * Ability to read thermostat data (haven't confirmed multiple thermostat support)
 * Added additional mappings for thermostat support
 * Additional code clean up
 *
 * 0.1.7
 * Rewrite of the various commands
 *
 * 0.1.6
 * Changed text description mapping to string characters
 *
 * 0.1.5
 * Added zone types
 * Added zone definitions
 *
 * 0.1.4
 * Added additional command requests
 *
 * 0.1.3
 * Receive status messages and interpret data
 *
 * 0.1.2
 * Minor changes to script nomenclature and formatting
 *
 * 0.1.1
 * Ability to connect Elk M1 and see data
 * Ability to send commands to Elk M1
 * Changed code to input parameter
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 * I - Unable to use the secure port on the Elk M1XEP.  A non-secure port is required.
 * I - Fan Circulate and set schedule not supported.
 * I - Elk does not support changing Keypad Temp, User Name Text, code length, Entry Delay and Exit Delay from external device.
 * I - Elk does not support retrieving of User PIN codes.  getCodes will return 0000 for pins of users not previously changed.
 * I - Elk does not report when a custom value or counter value is changed.  Changes within the panel will not be
 *     automatically reflected in these types of child devices.
 * F - Request text descriptions for zone setup, tasks and outputs (currently this must be done via the app).
 * I - A device with the same device network ID exists (this is really not an issue).
 *
 ***********************************************************************************************************************/
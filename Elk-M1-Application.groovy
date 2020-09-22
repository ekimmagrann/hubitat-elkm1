/***********************************************************************************************************************
 *
 *  A Hubitat App for managing Elk M1 Integration
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
 *  Name: Elk M1 Application
 *
 *  Special Thanks to Doug Beard for the framework of this application!
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.2.6" }

import groovy.transform.Field

definition(
		name: "Elk M1 Application",
		namespace: "belk",
		author: "Mike Magrann",
		description: "Integrate an Elk M1 Security System",
		category: "Security",
		iconUrl: "",
		iconX2Url: "",
		iconX3Url: "",
		importUrl: "https://raw.githubusercontent.com/thecaptncode/hubitat-elkm1/master/Elk-M1-Application.groovy",
		documentationLink: "https://github.com/thecaptncode/hubitat-elkm1/wiki"
)

preferences {
	page(name: "mainPage", nextPage: "deviceMapsPage")
	page(name: "deviceMapsPage", nextPage: "mainPage")
	page(name: "notificationPage", nextPage: "mainPage")
	page(name: "lockPage", nextPage: "mainPage")
	page(name: "hsmPage", nextPage: "mainPage")
	page(name: "locationModePage", nextPage: "mainPage")
	page(name: "defineDeviceMap", nextPage: "deviceMapsPage")
	page(name: "defineLightMap", nextPage: "deviceMapsPage")
	page(name: "defineDeviceMapImport", nextPage: "deviceMapsPage")
	page(name: "importingDevices", nextPage: "deviceMapsPage")
	page(name: "editDeviceMapPage", nextPage: "deviceMapsPage")
}

//App Pages/Views
Map mainPage() {
	if (getChildDevices().size() != 0) {
		subscriptionCleanup()
	}
	app.removeSetting("elkM1Password")
	app.removeSetting("isDebug")
	state.remove("ElkM1DeviceName")
	state.remove("ElkM1IP")
	state.remove("ElkM1Port")
	state.remove("ElkM1Code")
	state.remove("ElkM1Password")
	state.remove("allZones")
	state.remove("ElkM1Installed")
	state.remove("isDebug")
	state.remove("creatingZone")
	state.remove("editedZoneDNI")
	state.remove("creatingDevice")
	state.remove("editedDeviceDNI")
	if (dbgEnable)
		log.debug app.name + ": Showing mainPage"
	dynamicPage(name: "mainPage", title: "", install: false, uninstall: true) {
		if (getChildDevices().size() == 0) {
			section("Define your Elk M1 device") {
				input name: "elkM1Name", type: "text", title: "Elk M1 Name", required: true, defaultValue: "Elk M1"
				input name: "elkM1IP", type: "text", title: "Elk M1 IP Address", required: true
				input name: "elkM1Port", type: "number", title: "Elk M1 Port", range: "1..65535", required: true, defaultValue: 2101
				input name: "elkM1Keypad", type: "number", title: "Elk M1 Keypad", range: "1..16", required: true, defaultValue: 1
				input name: "elkM1Code", type: "text", title: "Elk M1 User Code", required: true
			}
		} else {
			section("<h1>Device Mapping and Integration</h1>") {
				href(name: "deviceMapsPage", title: "Devices",
						description: "Create Virtual Devices and Map them to Existing Zone, Output, Task, Lighting, Thermostat, Keypad, " +
								"Custom, Counter and/or Speech Devices in your Elk M1 setup.\nIntegrate virtual Elk M1 devices with physical devices.",
						page: "deviceMapsPage")
			}

			section("<h1>Notifications</h1>") {
				href(name: "notificationPage", title: "Notifications",
						description: "Enable Push and TTS Messages",
						page: "notificationPage")
			}

			section("<h1>Integration</h1>") {
				href(name: "lockPage", title: "Locks",
						description: "Integrate Locks",
						page: "lockPage")
				href(name: "hsmPage", title: "HSM",
						description: "Integrate Hubitat Safety Monitor",
						page: "hsmPage")
				href(name: "locationModePage", title: "Location Mode",
						description: "Integrate Hub Location Mode",
						page: "locationModePage")
			}

		}
		section("") {
			input name: "dbgEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false, submitOnChange: true
		}

	}
}

Map notificationPage() {
	dynamicPage(name: "notificationPage", title: none) {
		section("<h1>Notifications</h1>") {
			paragraph "Enable TTS and Notification integration will announcing arming and disarming over your supported audio " +
					"and/or push enabled device"
			paragraph "<h3><b>Notification Text</b></h2>"

			input name: "disarmedBool", type: "bool", title: "Enable Disarmed Notification", defaultValue: false, submitOnChange: true
			if (disarmedBool) {
				input name: "disarmedText", type: "text", title: "Notification for Disarmed", defaultValue: "Disarmed"
			}
			input name: "armingAwayBool", type: "bool", title: "Enable Arming Away Notification", defaultValue: false, submitOnChange: true
			if (armingAwayBool) {
				input name: "armingAwayText", type: "text", title: "Notification for Arming Away", defaultValue: "Arming Away"
			}
			input name: "armingHomeBool", type: "bool", title: "Enable Arming Home Notification", defaultValue: false, submitOnChange: true
			if (armingHomeBool) {
				input name: "armingHomeText", type: "text", title: "Notification for Arming Home", defaultValue: "Arming Home"
			}
			input name: "armingNightBool", type: "bool", title: "Enable Arming Night Notification", defaultValue: false, submitOnChange: true
			if (armingNightBool) {
				input name: "armingNightText", type: "text", title: "Notification for Arming Night", defaultValue: "Arming Night"
			}
			input name: "armingVacationBool", type: "bool", title: "Enable Arming Vacation Notification", defaultValue: false, submitOnChange: true
			if (armingVacationBool) {
				input name: "armingVacationText", type: "text", title: "Notification for Arming Vacation", defaultValue: "Arming Vacation"
			}
			input name: "armedBool", type: "bool", title: "Enable Armed Notification", defaultValue: false, submitOnChange: true
			if (armedBool) {
				input name: "armedText", type: "text", title: "Notification for Armed", defaultValue: "Armed"
			}
			input name: "entryDelayAlarmBool", type: "bool", title: "Enable Entry Delay Notification", defaultValue: false, submitOnChange: true
			if (entryDelayAlarmBool) {
				input name: "entryDelayAlarmText", type: "text", title: "Notification for Entry Delay",
						defaultValue: "Entry Delay in Progress, Alarm eminent"
			}
			input name: "alarmBool", type: "bool", title: "Enable Alarm Notification", defaultValue: false, submitOnChange: true
			if (alarmBool) {
				input name: "alarmText", type: "text", title: "Notification for Alarm", defaultValue: "Alarm, Alarm, Alarm, Alarm, Alarm"
			}
			paragraph "<h3><b>Notification Devices</b></h2>"
			input name: "speechDevices", type: "capability.speechSynthesis", title: "Which speech devices?", multiple: true, submitOnChange: true
			input name: "notificationDevices", type: "capability.notification", title: "Which notification devices?", multiple: true,
					submitOnChange: true
		}
	}
}

Map lockPage() {
	if (dbgEnable)
		log.debug app.name + ": Showing lockPage"

	dynamicPage(name: "lockPage", title: none) {
		section("<h1>Locks</h1>") {
			paragraph "Enable Lock Integration, selected locks will lock when armed and/or unlock when disarmed"
			input name: "armLocks", type: "capability.lock", title: "Which locks to lock when armed?", multiple: true, submitOnChange: true
			input name: "disarmLocks", type: "capability.lock", title: "Which locks to unlock when disarmed?", multiple: true, submitOnChange: true
		}
	}
}

Map hsmPage() {
	if (dbgEnable)
		log.debug app.name + ": Showing hsmPage"
	if (enableHSMtoElk)
		subscribe(location, "hsmStatus", statusHandler)
	else
		unsubscribe(location, "hsmStatus")
	dynamicPage(name: "hsmPage", title: none) {
		section("<h1>Hubitat Safety Monitor</h1>") {
			paragraph "Hubitat Safety Monitor Integration will tie your Elk M1 Arm Modes to the status of HSM. " +
					" Enable both switches for full integration."
			paragraph "HSM Status will be set to Arm Away, Arm Home, Arm Night or Disarm when the Elk M1 Arm Mode changes."
			input name: "enableElktoHSM", type: "bool", title: "Enable Elk M1 to HSM Integration", defaultValue: false, submitOnChange: true
			paragraph "Your Elk M1 will receive the Arm Away, Arm Home, Arm Night or Disarm commands when the HSM Status changes."
			input name: "enableHSMtoElk", type: "bool", title: "Enable HSM to Elk M1 Integration", defaultValue: false, submitOnChange: true
		}
	}
}

Map locationModePage() {
	if (dbgEnable)
		log.debug app.name + ": Showing locationModePage"

	dynamicPage(name: "locationModePage", title: none) {
		section("<h1>Location Modes</h1>") {
			paragraph "Location Mode Integration will set your hub's location mode based on your Elk M1 Arm Mode."
			input name: "enableLocationMode", type: "bool", title: "Enable Location Mode Integration", defaultValue: false, submitOnChange: true
			if (enableLocationMode) {
				Map<String> modes = ['': "(unchanged)"]
				location.getModes().each {
					modes.put(it.name, it.name)
				}
				input name: "modeDisarmed", type: "enum", title: "Location Mode when Disarmed", required: true, defaultValue: "", options: modes
				input name: "modeArmedAway", type: "enum", title: "Location Mode when Armed Away", required: true, defaultValue: "", options: modes
				input name: "modeArmedHome", type: "enum", title: "Location Mode when Armed Home", required: true, defaultValue: "", options: modes
				input name: "modeArmedNight", type: "enum", title: "Location Mode when Armed Night", required: true, defaultValue: "", options: modes
				input name: "modeArmedVacation", type: "enum", title: "Location Mode when Armed Vacation", required: true, defaultValue: "",
						options: modes
			}
		}
	}
}

Map deviceMapsPage() {
	app.removeSetting("deviceNumber")
	state.remove("buttonClicked")
	if (dbgEnable)
		log.debug app.name + ": Showing deviceMapsPage"
	if (getChildDevices().size() == 0) {
		createElkM1ParentDevice()
	}

	state.editedDeviceDNI = null
	Map<List<String>> integrationMap = [:]
	settings.findAll { it.key.startsWith("integrate:") }.each {
		String[] integrations = it.key.split(':')

		if (integrations.size() > 2) {
			if (integrationMap[integrations[1]] == null) {
				integrationMap[integrations[1]] = [integrations[2]]
			} else {
				integrationMap[integrations[1]] << integrations[2]
			}
		}
	}
	dynamicPage(name: "deviceMapsPage", title: "", install: true, uninstall: false) {

		section("<h1>Device Maps</h1>") {
			paragraph "The partition of your Elk M1 Installation may consist of Zone, Output, Task, Lighting, Thermostat, Keypad, " +
					"Custom, Counter and Speech Devices.  You can choose to map the devices manually or use the import method. "
			paragraph "You'll want to determine the device number as it is defined in your Elk M1 setup. " +
					" Define a new device in Elk M1 application and the application will then create either a Virtual sensor component device or" +
					" an Elk Child device, which will report the state of the Elk M1 device to which it is mapped. " +
					" The devices can be used in Rule Machine or any other application that is capable of leveraging the devices capability."
		}
		section("<h2>Create New Devices</h2>") {
			href(name: "createDeviceImportPage", title: "Import Elk Devices",
					description: "Click to import Elk devices",
					page: "defineDeviceMapImport")
			href(name: "createDeviceMapPage", title: "Create a Device Map",
					description: "Create a Virtual Device Manually",
					page: "defineDeviceMap")
			href(name: "createLightMapPage", title: "Create a Lighting Device Map",
					description: "Create a Virtual Lighting Device Manually",
					page: "defineLightMap")
		}

		com.hubitat.app.DeviceWrapper deviceInfo = getChildDevice(state.ElkM1DNI)
		section("<h2>Existing Devices</h2>") {
			paragraph "View or change the integration of existing Elk M1 devices with physical devices."
			href(name: "editDeviceMapPage", title: "${deviceInfo.label}",
					description: integrationOptions(integrationMap[deviceInfo.deviceNetworkId], deviceInfo),
					params: [deviceNetworkId: deviceInfo.deviceNetworkId],
					page: "editDeviceMapPage")
			deviceInfo.getChildDevices().sort { it.deviceNetworkId }.each {
				href(name: "editDeviceMapPage", title: "${it.label}",
						description: integrationOptions(integrationMap[it.deviceNetworkId], it),
						params: [deviceNetworkId: it.deviceNetworkId],
						page: "editDeviceMapPage")
			}
		}
	}
}

String integrationOptions(List<String> integrationList, com.hubitat.app.DeviceWrapper deviceInfo) {
	boolean hasCapabilities = false
	deviceInfo.getCapabilities().each {
		if (capabilityMap[it.name] != null)
			hasCapabilities = true
	}
	String description
	if (integrationList != null && (integrationList.size() > 1 || integrationList.first() != "smart")) {
		boolean hasSmart = integrationList.indexOf("smart") >= 0
		List<String> smartList = getSmartList(deviceInfo.deviceNetworkId, integrationList)
		integrationList.each {
			if (it != "smart") {
				if (description == null)
					description = it
				else
					description += ", " + it
				if (hasSmart && smartList.indexOf(it) >= 0)
					description += " (smart refresh)"
			}
		}
		description = "Integrated using " + description
	} else if (hasCapabilities) {
		description = "Add Integrations"
	} else {
		description = "Device Details"
	}
}

Map defineDeviceMap() {
	String message = null
	if (state.buttonClicked == "btnCreateDevice")
		message = createDevice()
	if (message != null && message.length() == 0) {
		importingDevices()
	} else {
		if (dbgEnable)
			log.debug app.name + ": Showing defineDeviceMap"
		dynamicPage(name: "defineDeviceMap", title: "") {
			section("<h1>Create a Device Map</h1>") {
				paragraph "Create a Map for a device in Elk M1"
				input name: "deviceName", type: "text", title: "Device Name", defaultValue: "Zone x"
				input name: "deviceNumber", type: "number", title: "Which Device 1 - 208", defaultValue: 1, range: "1..208"
				input name: "deviceType", type: "enum", title: "Zone, Output, Task, Thermostat, Keypad, Custom, Counter or Speech Device?",
						options: [['00': "Zone (1 - 208)"], ['04': "Output (1 - 208)"], ['05': "Task (1 - 32)"], ['11': "Thermostat (1 - 16)"],
								  ['03': "Keypad (1 - 16)"], ['09': "Custom (1 - 20)"], ['10': "Counter (1 - 64)"], ['SP': "Speech (1)"]]
				input name: "btnCreateDevice", type: "button", title: "Create device", submitOnChange: true
				if (message) {
					paragraph "<span style=\"color:#d86917;\">" + message + "</span>"
				}
			}
		}
	}
}

Map defineLightMap() {
	String message = null
	if (state.buttonClicked == "btnCreateDevice")
		message = createDevice()
	if (message != null && message.length() == 0) {
		importingDevices()
	} else {
		if (dbgEnable)
			log.debug app.name + ": Showing defineLightMap"
		dynamicPage(name: "defineLightMap", title: "") {
			section("<h1>Create a Device Map</h1>") {
				paragraph "Create a Map for a lighting device in Elk M1"
				input name: "deviceName", type: "text", title: "Device Name", defaultValue: "Zone x"
				input name: "deviceType", type: "enum", title: "Which Lighting Group?",
						options: [['A': "A"], ['B': "B"], ['C': "C"], ['D': "D"], ['E': "E"], ['F': "F"], ['G': "G"], ['H': "H"],
								  ['I': "I"], ['J': "J"], ['K': "K"], ['L': "L"], ['M': "M"], ['N': "N"], ['O': "O"], ['P': "P"]]
				input name: "deviceNumber", type: "number", title: "Which Device 1 - 16", defaultValue: 1, range: "1..16"
				input name: "btnCreateDevice", type: "button", title: "Create device", submitOnChange: true
				if (message) {
					paragraph "<span style=\"color:#d86917;\">" + message + "</span>"
				}
			}
		}
	}
}

Map defineDeviceMapImport() {
	String message = null
	if (state.buttonClicked == "btnImportDevices")
		message = importDevices()
	if (message != null && message.length() == 0) {
		importingDevices()
	} else {
		if (dbgEnable)
			log.debug app.name + ": Showing defineDeviceMapImport"
		dynamicPage(name: "defineDeviceMapImport", title: "") {
			section("<h1>Import Elk Devices</h1>") {
				paragraph "Create a Map for a device in Elk M1"
				input name: "deviceType", type: "enum", title: "Select Device Type",
						options: [['00': "Zones"], ['04': "Output"], ['05': "Task"], ['07': "Lighting"], ['11': "Thermostat"], ['03': "Keypad"],
								  ['09': "Custom"], ['10': "Counter"]]
				input name: "btnImportDevices", type: "button", title: "Import devices", submitOnChange: true
				if (message) {
					paragraph "<span style=\"color:#d86917;\">" + message + "</span>"
				}
			}
		}
	}
}

Map importingDevices() {
	state.remove("buttonClicked")
	if (dbgEnable)
		log.debug app.name + ": Showing importingDevices"
	dynamicPage(name: "defineDeviceMapImport", title: "") {
		section("<h1>Import Elk Devices</h1>") {
			paragraph "Starting import of devices for Elk M1 - Click Next to continue"
		}
	}
}

Map editDeviceMapPage(message) {
	if (message != null) {
		state.editedDeviceDNI = message.deviceNetworkId
	}
	if (dbgEnable)
		log.debug app.name + ": Showing editDeviceMapPage for ${state.editedDeviceDNI}"
	com.hubitat.app.DeviceWrapper deviceInfo
	if (state.editedDeviceDNI == state.ElkM1DNI)
		deviceInfo = getChildDevice(state.ElkM1DNI)
	else
		deviceInfo = getChildDevice(state.ElkM1DNI).getChildDevice(state.editedDeviceDNI)
	String paragraphText = "No device capabilities exist that can be integrated.  Current capabilities are:\n"
	List<String> capList = subscribeDevice(deviceInfo)
	List<String> smartList = []
	if (capList.size() == 0) {
		deviceInfo.getCapabilities().each {
			if (it.name != "Actuator")
				paragraphText = paragraphText + it.name + "\n"
		}
	} else {
		smartList = getSmartList(deviceInfo.deviceNetworkId, capList)
		paragraphText = "Choose any devices you want to integrate with ${deviceInfo.label}"
	}

	dynamicPage(name: "editDeviceMapPage", title: "") {
		section("<h1>${deviceInfo.label}</h1>") {
			href(name: "editChild", title: "Edit Device",
					description: "Click to edit this device",
					url: "/device/edit/${deviceInfo.id}")
			if (smartList.size() > 0) {
				input name: "integrate:$state.editedDeviceDNI:smart", type: "bool", title: "Use smart refresh for ${smartList.join(', ')} *",
						defaultValue: false, submitOnChange: true
			}
			paragraph paragraphText
			capList.each { cap ->
				input name: "integrate:$state.editedDeviceDNI:$cap", type: "${capabilityMap[cap]}", title: "$cap device to integrate",
						submitOnChange: true
			}
			if (smartList.size() > 0)
				paragraph "* Smart refresh will refresh the integrated device first before refreshing certain child device capabilities.  " +
						"This prevents an Elk refresh from changing the physical device's settings."
		}
	}
}

List<String> subscribeDevice(com.hubitat.app.DeviceWrapper deviceInfo) {
	String targetDNID = deviceInfo.deviceNetworkId
	String targetDevID = deviceInfo.id.toString()
	String attribute
	String triggerValue
	String attributeValue
	List<String> capList = []
	deviceInfo.getCapabilities().each {
		if (capabilityMap[it.name] != null)
			capList << it.name
	}
	List<String> smartList = []
	if (settings["integrate:$targetDNID:smart"])
		smartList = getSmartList(targetDNID, capList)
	Map<List<String>> mySubscription = [:]
	atomicState.subscriptionMap.each { k, v ->
		List<String> subscriptions = []
		v.findAll { !it.startsWith(targetDNID + ":") }.each {
			subscriptions << it
		}
		if (subscriptions.size() > 0)
			mySubscription[k] = subscriptions
	}
	Map<List<String>> myRevSubscription = atomicState.RevSubscriptionMap
	myRevSubscription.remove(targetDevID)
	boolean stateSaved = false
	capList.each { capability ->
		if (settings["integrate:$targetDNID:$capability"] instanceof com.hubitat.app.DeviceWrapper) {
			com.hubitat.app.DeviceWrapper integratedDevice = settings["integrate:$targetDNID:$capability"]
			if (integratedDevice != null && integratedDevice.deviceNetworkId != targetDNID) {
				if (dbgEnable)
					log.debug app.name + ": Subscribing ${integratedDevice.label} capability ${capability} to target ${deviceInfo.label}"
				if (mySubscription[integratedDevice.deviceId.toString()] == null) {
					mySubscription[integratedDevice.deviceId.toString()] = [targetDNID + ":" + capability]
				} else {
					mySubscription[integratedDevice.deviceId.toString()] << (targetDNID + ":" + capability)
					mySubscription[integratedDevice.deviceId.toString()] = mySubscription[integratedDevice.deviceId.toString()].unique()
				}
				if (myRevSubscription[targetDevID] == null) {
					myRevSubscription[targetDevID] = [targetDNID + ":" + capability]
				} else {
					myRevSubscription[targetDevID] << (targetDNID + ":" + capability)
					myRevSubscription[targetDevID] = myRevSubscription[targetDevID].unique()
				}
				atomicState.subscriptionMap = mySubscription
				atomicState.RevSubscriptionMap = myRevSubscription
				stateSaved = true
				attributeMap.each { key, value ->
					if (key.startsWith(capability + ":")) {
						attribute = key.substring(capability.length() + 1)
						subscribe(integratedDevice, attribute, integrationHandler)
						subscribe(deviceInfo, attribute, revIntegrationHandler)
						int ndx = attribute.indexOf('.')
						if (ndx < 1) {
							triggerValue = ""
						} else {
							triggerValue = attribute.substring(ndx + 1)
							attribute = attribute.substring(0, ndx)
						}
						if (smartList.indexOf(capability) < 0) {
							attributeValue = deviceInfo.currentState(attribute)?.value
							if (attributeValue != null && (triggerValue == "" || triggerValue == attributeValue))
								deviceInfo.sendEvent(name: attribute, value: attributeValue, isStateChange: true,
										descriptionText: integratedDevice.label + " was subscribed to " + app.name + " for " + deviceInfo.label)
						} else {
							attributeValue = integratedDevice.currentState(attribute)?.value
							if (attributeValue != null && (triggerValue == "" || triggerValue == attributeValue))
								integratedDevice.sendEvent(name: attribute, value: attributeValue, isStateChange: true,
										descriptionText: integratedDevice.label + " was subscribed to " + app.name + " for " + deviceInfo.label)
						}
					}
				}
			}
		}
	}
	if (!stateSaved) {
		atomicState.subscriptionMap = mySubscription
		atomicState.RevSubscriptionMap = myRevSubscription
	}
	return capList
}

void subscriptionCleanup() {
	boolean fixSubscriptions = false
	com.hubitat.app.DeviceWrapper deviceInfo
	settings.findAll { it.key.startsWith("integrate:") }.each {
		String[] integrations = it.key.split(':')
		if (integrations.size() < 3 || (integrations[2] != "smart" && !(it.value instanceof com.hubitat.app.DeviceWrapper))) {
			log.trace app.name + ": Removing malformed integration setting $it.key"
			app.removeSetting(it.key)
			fixSubscriptions = true
		} else {
			if (integrations[1] == state.ElkM1DNI)
				deviceInfo = getChildDevice(state.ElkM1DNI)
			else
				deviceInfo = getChildDevice(state.ElkM1DNI).getChildDevice(integrations[1])
			if (deviceInfo == null) {
				log.trace app.name + ": Removing integration setting for missing child device $it.key"
				app.removeSetting(it.key)
				fixSubscriptions = true
			} else if (integrations[2] == "smart") {
				if (!it.value || getSmartList(deviceInfo.deviceNetworkId, deviceInfo.getCapabilities().collect { return it.name }).size() == 0)
					app.removeSetting(it.key)
			} else if (!deviceInfo.hasCapability(integrations[2])) {
				log.trace app.name + ": Removing integration setting for child device no longer supporting capability $it.key"
				app.removeSetting(it.key)
				fixSubscriptions = true
			} else {
				if (!it.value?.hasCapability(integrations[2])) {
					log.trace app.name + ": ${it.value?.label} no longer supports capability ${integrations[2]}"
					fixSubscriptions = true
				}
			}
		}
	}
	if (fixSubscriptions) {
		resubscribe()
	} else {
		if (atomicState.subscriptionMap == null)
			atomicState.subscriptionMap = [:]
		if (atomicState.RevSubscriptionMap == null)
			atomicState.RevSubscriptionMap = [:]
	}
}

int resubscribe() {
	int cnt = 0
	String lastDNID = ""
	com.hubitat.app.DeviceWrapper deviceInfo
	log.trace app.name + ": Rebuilding integration subscriptions"
	unsubscribe()
	if (enableHSMtoElk)
		subscribe(location, "hsmStatus", statusHandler)
	atomicState.subscriptionMap = [:]
	atomicState.RevSubscriptionMap = [:]
	settings.findAll { it.key.startsWith("integrate:") }.each {
		String[] integrations = it.key.split(':')
		if (integrations[1] != lastDNID) {
			if (integrations[1] == state.ElkM1DNI)
				deviceInfo = getChildDevice(state.ElkM1DNI)
			else
				deviceInfo = getChildDevice(state.ElkM1DNI).getChildDevice(integrations[1])
			if (deviceInfo != null) {
				subscribeDevice(deviceInfo)
				cnt++
			}
			lastDNID = integrations[1]
		}
	}
	return cnt
}

List<String> getSmartList(String deviceNetworkId, List<String> capabilityList) {
	String deviceType = deviceNetworkId == state.ElkM1DNI ? "__" : deviceNetworkId.substring(state.ElkM1DNI.length()).take(3)
	List<String> smartList = []
	capabilityList.each {
		if (smartMap[it] != null && (smartMap[it] == "" || smartMap[it].indexOf(deviceType) < 0))
			smartList << it
	}
	return smartList
}

void createElkM1ParentDevice() {
	if (dbgEnable)
		log.debug app.name + ": Creating Parent ElkM1 Device"
	if (getChildDevice(state.ElkM1DNI) == null) {
		state.ElkM1DNI = UUID.randomUUID().toString()
		if (dbgEnable)
			log.debug app.name + ": Setting state.ElkM1DNI ${state.ElkM1DNI}"
		addChildDevice("belk", "Elk M1 Driver", state.ElkM1DNI, null, [name: elkM1Name, isComponent: true, label: elkM1Name])
		getChildDevice(state.ElkM1DNI).updateSetting("ip", [type: "text", value: elkM1IP])
		getChildDevice(state.ElkM1DNI).updateSetting("port", [type: "number", value: elkM1Port])
		getChildDevice(state.ElkM1DNI).updateSetting("keypad", [type: "number", value: elkM1Keypad])
		getChildDevice(state.ElkM1DNI).updateSetting("code", [type: "text", value: elkM1Code])
		sendEvent(name: "createElkM1ParentDevice", value: state.ElkM1DNI, descriptionText: "${app.name} created parent device ${state.ElkM1DNI}")
		pauseExecution(10000)
		if (dbgEnable) {
			if (getChildDevice(state.ElkM1DNI))
				log.debug app.name + ": Found a Child Elk M1 ${getChildDevice(state.ElkM1DNI).label}"
			else
				log.debug app.name + ": Did not find a Parent Elk M1"
		}
	}
}

String createDevice() {
	if (deviceType == "SP")
		deviceNumber = 1
	String DeviceType = deviceType
	int DeviceNumber = deviceNumber
	if (DeviceType != null && DeviceType.length() == 1 && DeviceType >= 'A' && DeviceType <= 'P') {
		DeviceNumber = "ABCDEFGHIJKLMNOP".indexOf(DeviceType) * 16 + DeviceNumber
		DeviceType = '07'
	}
	if (DeviceType == null || elkTextDescriptionsTypes[DeviceType] == null) {
		return "Please select a device type."
	} else if (DeviceNumber == null || DeviceNumber < 1 || DeviceNumber > elkTextDescriptionsMax[DeviceType]) {
		return "Please enter a device number between 1 and ${elkTextDescriptionsMax[DeviceType]} for ${elkTextDescriptionsTypes[DeviceType]}."
	} else if (deviceName == null || deviceName.trim().length() == 0) {
		return "Please enter a device name."
	} else {
		sendEvent(name: "createDevice", value: elkTextDescriptionsTypes[DeviceType] + " " + DeviceNumber + ":" + deviceName,
				descriptionText: "${app.name} starting import of type ${elkTextDescriptionsTypes[DeviceType]}, " +
						"number ${DeviceNumber}, name ${deviceName}")
		String deviceText = null
		if (dbgEnable)
			log.debug app.name + ": Starting validation of ${deviceName} DeviceType: ${DeviceType} DeviceNumber: ${DeviceNumber}"
		getChildDevice(state.ElkM1DNI).createDevice([deviceNumber: String.format("%03d", DeviceNumber), deviceName: deviceName,
													 deviceType  : DeviceType, deviceText: deviceText])
		return ""
	}
}

String importDevices() {
	if (deviceType == null || elkTextDescriptionsTypes[deviceType] == null) {
		return "Please select a device type."
	} else {
		sendEvent(name: "importDevices", value: elkTextDescriptionsTypes[deviceType],
				descriptionText: "${app.name} starting import of type ${elkTextDescriptionsTypes[deviceType]}")
		getChildDevice(state.ElkM1DNI).startCreatingDevice(deviceType)
		return ""
	}
}

//General App Events
void appButtonHandler(btn) {
	state.buttonClicked = btn
}

void installed() {
	initialize()
}

void updated() {
	log.info app.name + "updated"
	initialize()
}

void initialize() {
	log.info app.name + "initialize"
}

void uninstalled() {
	getChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

void setArmMode(String armMode) {
	if (state.armMode == null || state.armMode != armMode) {
		if (dbgEnable)
			log.debug "${app.name}: Arm Mode changing from ${state.armMode} to ${armMode}"
		String hsmSetArm
		String locationMode
		switch (armMode) {
			case "Disarmed":
				hsmSetArm = "disarm"
				locationMode = modeDisarmed
				break
			case "Away":
				hsmSetArm = "armAway"
				locationMode = modeArmedAway
				break
			case "Home":
				hsmSetArm = "armHome"
				locationMode = modeArmedHome
				break
			case "Night":
				hsmSetArm = "armNight"
				locationMode = modeArmedNight
				break
			case "Vacation":
				hsmSetArm = "armAway"
				locationMode = modeArmedVacation
				break
		}
		if (hsmSetArm == "disarm") {
			unlockIt()
			speakDisarmed()
		} else {
			lockIt()
			speakArmed()
		}

		String hsmStatus = location.hsmStatus
		if (dbgEnable && enableElktoHSM)
			log.debug "${app.name}: HSM arm changing from ${hsmStatus} to ${hsmSetArm}"
		if (enableElktoHSM && ((hsmSetArm == "armAway" && hsmStatus != "armedAway" && hsmStatus != "armingAway") ||
				(hsmSetArm == "armHome" && hsmStatus != "armedHome" && hsmStatus != "armingHome") ||
				(hsmSetArm == "armNight" && hsmStatus != "armedNight" && hsmStatus != "armingNight") ||
				(hsmSetArm == "disarm" && hsmStatus != "disarmed" && hsmStatus != "allDisarmed")))
			sendLocationEvent(name: "hsmSetArm", value: hsmSetArm, descriptionText: getChildDevice(state.ElkM1DNI).label + " was armed " + armMode)

		if (enableLocationMode && locationMode != null && locationMode != "") {
			if (location.getModes().findIndexOf { it.name == locationMode } != -1) {
				String curmode = location.currentMode.name
				if (dbgEnable)
					log.debug "${app.name}: Location Mode changing from $curmode to $locationMode"
				location.setMode(locationMode)
			}
		}
		state.armMode = armMode
	}
}

void smartRefresh() {
	String[] smartArr
	com.hubitat.app.DeviceWrapper childDevice
	String deviceType
	String[] integrationArr
	String[] attributeArr
	String attributes
	String triggerValue
	String attributeValue
	int integerValue
	String cmd
	atomicState.smartRefresh = now()
	if (dbgEnable)
		log.debug "${app.name}: Starting smartRefresh ${atomicState.smartRefresh}"
	runIn(10, stopSmartRefresh)
	// Find smart refresh child devices
	settings.findAll { k, v -> k.startsWith("integrate:") && k.endsWith(":smart") && v }.each { smartKey, smartValue ->
		smartArr = smartKey.split(':')
		if (smartArr.size() > 2) {
			if (smartArr[1] == state.ElkM1DNI) {
				childDevice = getChildDevice(state.ElkM1DNI)
				deviceType = "__"
			} else {
				childDevice = getChildDevice(state.ElkM1DNI).getChildDevice(smartArr[1])
				deviceType = smartArr[1].substring(state.ElkM1DNI.length()).take(3)
			}
			// Find devices integrated with child device
			if (childDevice != null) {
				settings.findAll { k, v -> k.startsWith("integrate:" + smartArr[1] + ":") && !k.endsWith(":smart") }.each {
					integrationKey, deviceInfo ->
						integrationArr = integrationKey.split(':')
						// If this integration's capability is a smart refresh capability...
						if (smartMap[integrationArr[2]] != null && (smartMap[integrationArr[2]] == "" ||
								smartMap[integrationArr[2]].indexOf(deviceType) < 0)) {
							if (deviceInfo?.hasCapability(integrationArr[2]) && !deviceInfo?.deviceNetworkId.startsWith(state.ElkM1DNI)) {
								// Find all attributes integrated for this capability
								attributeMap.findAll { k, v -> k.startsWith(integrationArr[2] + ":") }.each { attributeKey, attributeCmd ->
									attributeArr = attributeKey.split(":")
									if (attributeArr.size() > 1 && attributeCmd != "attributeonly" && childDevice.hasCommand(attributeCmd)) {
										int ndx = attributeArr[1].indexOf('.')
										if (ndx < 1) {
											triggerValue = ""
											attributes = attributeArr[1]
										} else {
											triggerValue = attributeArr[1].substring(ndx + 1)
											attributes = attributeArr[1].substring(0, ndx)
										}
										// Refresh child device with the attribute's current value
										attributeValue = deviceInfo.currentState(attributes)?.value
										if (attributeValue != null && (triggerValue == "" || triggerValue == attributeValue)) {
											integerValue = attributeValue.isInteger() ? attributeValue.toInteger() : 0
											if (dbgEnable)
												log.debug app.name + " refreshing " + deviceInfo.label + " " +
														attributes + " value " + attributeValue + " on " + childDevice.label
											triggerCmd(childDevice, attributeCmd, attributeValue, integerValue)
										}
									}
								}
							}
						}
				}
			}
		}
	}
}

void stopSmartRefresh() {
	if (dbgEnable)
		log.debug "${app.name}: smartRefresh complete ${atomicState.smartRefresh}"
	atomicState.remove("smartRefresh")
}

void integrationHandler(com.hubitat.hub.domain.Event evt) {
	//log.debug "Unhandled integration event:"
	//evt?.properties?.each { item -> log.debug "$item.key = $item.value" }
	//log.debug "Device $evt.displayName, attribute $evt.name = $evt.value"
	int triggerCnt = 0
	com.hubitat.app.DeviceWrapper deviceInfo
	String cmd
	List<String> targetList = atomicState.subscriptionMap[evt.deviceId.toString()]
	if (targetList != null) {
		targetList.each {
			String[] targetArr = it.split(':')
			if (targetArr.size() > 1) {
				cmd = attributeMap[targetArr[1] + ":" + evt.name + "." + evt.value]
				if (cmd == null)
					cmd = attributeMap[targetArr[1] + ":" + evt.name]
				if (cmd != null) {
					if (targetArr[0] == state.ElkM1DNI)
						deviceInfo = getChildDevice(state.ElkM1DNI)
					else
						deviceInfo = getChildDevice(state.ElkM1DNI).getChildDevice(targetArr[0])
					if (deviceInfo != null && deviceInfo.hasCapability(targetArr[1])) {
						triggerCnt += updateDevice(evt, deviceInfo, cmd, true)
					}
				}
			}
		}
	}
	if (triggerCnt == 0) { // This subscription is no longer needed
		unsubscribe(evt.getDevice(), evt.name, integrationHandler)
		unsubscribe(evt.getDevice(), evt.name + "." + evt.value, integrationHandler)
	}
}

void revIntegrationHandler(com.hubitat.hub.domain.Event evt) {
	//log.debug "Device $evt.displayName, attribute $evt.name = $evt.value"
	int triggerCnt = 0
	com.hubitat.app.DeviceWrapper deviceInfo
	String cmd
	List<String> targetList = atomicState.RevSubscriptionMap[evt.deviceId.toString()]
	if (targetList != null) {
		targetList.each {
			String[] targetArr = it.split(':')
			if (targetArr.size() > 1) {
				cmd = attributeMap[targetArr[1] + ":" + evt.name + "." + evt.value]
				if (cmd == null)
					cmd = attributeMap[targetArr[1] + ":" + evt.name]
				if (cmd != null) {
					deviceInfo = settings["integrate:${targetArr[0]}:${targetArr[1]}"]
					if (deviceInfo?.hasCapability(targetArr[1])) {
						triggerCnt += updateDevice(evt, deviceInfo, cmd, false)
					}
				}
			}
		}
	}
	if (triggerCnt == 0) { // This subscription is no longer needed
		unsubscribe(evt.getDevice(), evt.name, revIntegrationHandler)
		unsubscribe(evt.getDevice(), evt.name + "." + evt.value, revIntegrationHandler)
	}
}

int updateDevice(com.hubitat.hub.domain.Event evt, com.hubitat.app.DeviceWrapper deviceInfo, String cmd, boolean fromIntegrated) {
	//log.debug "Device $evt.displayName, attribute $evt.name = $evt.value ($evt.integerValue), todevice $deviceInfo.label, command: $cmd"
	int triggerCnt = 0
	boolean isSmartRefresh = (atomicState.smartRefresh != null && now() - (long) (atomicState.smartRefresh) < 10000)
	if (cmd != "attributeonly" && deviceInfo.hasCommand(cmd)) {
		triggerCnt = 1
		if ((deviceInfo.currentState(evt.name)?.value == null || deviceInfo.currentState(evt.name).value != evt.value) &&
				(!isSmartRefresh || fromIntegrated)) {
			if (dbgEnable)
				log.debug app.name + ": ${evt.displayName} triggering command ${cmd}(${evt.value}) on ${deviceInfo.label}" +
						" isSmartRefresh ${isSmartRefresh} (${atomicState.smartRefresh}) fromIntegrated ${fromIntegrated}"
			triggerCmd(deviceInfo, cmd, evt.value, evt.integerValue)
		}
	} else if (deviceInfo.hasAttribute(evt.name)) {
		triggerCnt = 1
		if ((deviceInfo.currentState(evt.name)?.value == null || deviceInfo.currentState(evt.name).value != evt.value) &&
				(!isSmartRefresh || fromIntegrated)) {
			if (dbgEnable)
				log.debug app.name + ": ${evt.displayName} setting attribute ${evt.name} = ${evt.value} on ${deviceInfo.label}" +
						" isSmartRefresh ${isSmartRefresh} (${atomicState.smartRefresh}) fromIntegrated ${fromIntegrated}"
			String descriptionText = evt.descriptionText
			if (descriptionText == null || descriptionText == "")
				descriptionText = deviceInfo.label + " was updated by " + app.name + " with event from " + evt.displayName
			deviceInfo.sendEvent(name: evt.name, source: evt.source, type: evt.type, unit: evt.unit, value: evt.value,
					descriptionText: descriptionText)
		}
	}
	return triggerCnt
}

void triggerCmd(deviceInfo, String cmd, String value, int integerValue) {
	switch (cmd) {
		case "off":
			deviceInfo.off()
			break
		case "open":
			deviceInfo.open()
			break
		case "close":
			deviceInfo.close()
			break
		case "on":
			deviceInfo.on()
			break
		case "lock":
			deviceInfo.lock()
			break
		case "unlock":
			deviceInfo.unlock()
			break
		case "push":
			deviceInfo.push(integerValue)
			break
		case "setLevel":
			deviceInfo.setLevel(integerValue)
			break
		case "setCoolingSetpoint":
			deviceInfo.setCoolingSetpoint(integerValue)
			break
		case "setHeatingSetpoint":
			deviceInfo.setHeatingSetpoint(integerValue)
			break
		case "setThermostatFanMode":
			deviceInfo.setThermostatFanMode(value)
			break
		case "setThermostatMode":
			deviceInfo.setThermostatMode(value)
			break
		case "setTemperature":
			deviceInfo.setTemperature(integerValue)
			break
		case "disarm":
			deviceInfo.disarm()
			break
		case "armAway":
			deviceInfo.armAway()
			break
		case "armHome":
			deviceInfo.armHome()
			break
		case "armNight":
			deviceInfo.armNight()
			break
	}
}

void statusHandler(com.hubitat.hub.domain.Event evt) {
	if (evt?.source == "LOCATION" && evt?.name == "hsmStatus" && evt?.value != null) {
		boolean lock
		if (enableHSMtoElk && !lock && evt?.value && evt.value != state.hsmStatus) {
			lock = true
			log.info "HSM Alert: $evt.value"
			if (dbgEnable)
				log.debug app.name + ": HSM is enabled"
			com.hubitat.app.DeviceWrapper parentDevice = getChildDevice(state.ElkM1DNI)
			switch (evt.value) {
				case "armedAway":
				case "armingAway":
					if (dbgEnable)
						log.debug app.name + ": Sending Arm Away"
					if (parentDevice.currentValue("armState") == "Ready to Arm") {
						parentDevice.armAway()
					}
					break
				case "armedHome":
				case "armingHome":
					if (dbgEnable)
						log.debug app.name + ": Sending Arm Home"
					if (parentDevice.currentValue("armState") == "Ready to Arm") {
						parentDevice.armHome()
					}
					break
				case "armedNight":
				case "armingNight":
					if (dbgEnable)
						log.debug app.name + ": Sending Arm Night"
					if (parentDevice.currentValue("armState") == "Ready to Arm") {
						parentDevice.armNight()
					}
					break
				case "disarmed":
				case "allDisarmed":
					if (dbgEnable)
						log.debug app.name + ": Sending Disarm"
					if (parentDevice.currentValue("armStatus") != "Disarmed") {
						parentDevice.disarm()
					}
					break
			}
			lock = false;
		}
		state.hsmStatus = evt.value
	} else {
		log.debug "Unhandled event:"
		evt?.properties?.each { item -> log.debug "$item.key = $item.value" }
	}
}

void speakArmed() {
	if (!armedText)
		armedText = "Armed"
	speakIt(armedText, armedBool == null ? false : armedBool)
}

void speakArmingAway() {
	if (!armingAwayText)
		armingAwayText = "Arming Away"
	speakIt(armingAwayText, armingAwayBool == null ? false : armingAwayBool)
}

void speakArmingVacation() {
	if (!armingVacationText)
		armingVacationText = "Arming Vacation"
	speakIt(armingVacationText, armingVacationBool == null ? false : armingVacationBool)
}

void speakArmingHome() {
	if (!armingHomeText)
		armingHomeText = "Arming Home"
	speakIt(armingHomeText, armingHomeBool == null ? false : armingHomeBool)
}

void speakArmingNight() {
	if (!armingNightText)
		armingNightText = "Arming Night"
	speakIt(armingNightText, armingNightBool == null ? false : armingNightBool)
}

void speakDisarmed() {
	if (!disarmedText)
		disarmedText = "Disarmed"
	speakIt(disarmedText, disarmedBool == null ? false : disarmedBool)
}

void speakEntryDelay() {
	if (!entryDelayAlarmText)
		entryDelayAlarmText = "Entry Delay in Progress, Alarm eminent"
	speakIt(entryDelayAlarmText, entryDelayAlarmBool == null ? false : entryDelayAlarmBool)
}

void speakAlarm() {
	if (!alarmText)
		alarmText = "Alarm, Alarm, Alarm, Alarm, Alarm"
	speakIt(alarmText, alarmBool == null ? false : alarmBool)
}

void speakIt(String str, boolean isEnabled = true) {
	if (dbgEnable)
		log.debug app.name + ": TTS: $str"
	if (isEnabled && speechDevices) {
		if (atomicState.speaking) {
			if (dbgEnable)
				log.debug app.name + ": Already Speaking"
			runOnce(new Date(now() + 10000), speakRetry, [overwrite: false, data: [str: str]])
		} else {
			if (dbgEnable)
				log.debug app.name + ": Found Speech Devices"

			atomicState.speaking = true
			speechDevices.speak(str)
			atomicState.speaking = false

			if (notificationDevices) {
				if (dbgEnable)
					log.debug app.name + ": Found Notification Devices"
				notificationDevices.deviceNotification(str)
			}
		}
	}
}

void lockIt() {
	if (dbgEnable)
		log.debug app.name + ": Lock"
	if (armLocks) {
		if (dbgEnable)
			log.debug app.name + ": Found Lock"
		armLocks.lock()
	}
}

void unlockIt() {
	if (dbgEnable)
		log.debug app.name + ": Unlock"
	if (disarmLocks) {
		if (dbgEnable)
			log.debug app.name + ": Found Lock"
		disarmLocks.unlock()
	}
}

void speakRetry(data) {
	if (data.str) speakIt(data.str);
}

@Field static final String ZoneName = "Zone"
@Field static final String AreaName = "Area"
@Field static final String UserName = "User"
@Field static final String Keypad = "Keypad"
@Field static final String OutputName = "Output"
@Field static final String TaskName = "Task"
@Field static final String TelephoneName = "Telephone"
@Field static final String LightName = "Light"
@Field static final String AlarmDurationName = "Alarm Duration"
@Field static final String CustomSettings = "Custom Setting"
@Field static final String CountersNames = "Counter"
@Field static final String ThermostatNames = "Thermostat"
@Field static final String FunctionKey1Name = "Keypad button 1"
@Field static final String FunctionKey2Name = "Keypad button 2"
@Field static final String FunctionKey3Name = "Keypad button 3"
@Field static final String FunctionKey4Name = "Keypad button 4"
@Field static final String FunctionKey5Name = "Keypad button 5"
@Field static final String FunctionKey6Name = "Keypad button 6"
@Field static final String AudioZoneName = "Audio Zone"
@Field static final String AudioSourceName = "Audio Source"
@Field static final String Speech = "Speech"

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
		'19': AudioSourceName,
		'SP': Speech
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
		'19': 12,  // AudioSourceName
		'SP': 1    // Speech
]

@Field final Map smartMap = [        // Does not apply to which device types. Double understore = parent.
									 "PushableButton"             : "__,_P_",
									 "RelativeHumidityMeasurement": "",
									 "Switch"                     : "",
									 "SwitchLevel"                : "",
									 "TemperatureMeasurement"     : "",
									 "Thermostat"                 : ""
]

@Field final Map capabilityMap = [
		"Lock"                       : "capability.lock",
		"PushableButton"             : "capability.pushableButton",
		"RelativeHumidityMeasurement": "capability.relativeHumidityMeasurement",
		"SecurityKeypad"             : "capability.securityKeypad",
		"Switch"                     : "capability.switch",
		"SwitchLevel"                : "capability.switchLevel",
		"TemperatureMeasurement"     : "capability.temperatureMeasurement",
		"Thermostat"                 : "capability.thermostat"
]

@Field final Map attributeMap = [
		"Lock:lock.locked"                          : "lock",
		"Lock:lock.unlocked"                        : "unlock",
		"PushableButton:pushed"                     : "push",
		"RelativeHumidityMeasurement:humidity"      : "attributeonly",
		"SecurityKeypad:securityKeypad.disarmed"    : "disarm",
		"SecurityKeypad:securityKeypad.armed away"  : "armAway",
		"SecurityKeypad:securityKeypad.armed home"  : "armHome",
		"SecurityKeypad:securityKeypad.armed night" : "armNight",
		"SecurityKeypad:securityKeypad.arming away" : "armAway",
		"SecurityKeypad:securityKeypad.arming home" : "armHome",
		"SecurityKeypad:securityKeypad.arming night": "armNight",
		"Switch:switch.on"                          : "on",
		"Switch:switch.off"                         : "off",
		"SwitchLevel:level"                         : "setLevel",
		"TemperatureMeasurement:temperature"        : "setTemperature",
		"Thermostat:coolingSetpoint"                : "setCoolingSetpoint",
		"Thermostat:heatingSetpoint"                : "setHeatingSetpoint",
		"Thermostat:supportedThermostatFanModes"    : "attributeonly",
		"Thermostat:supportedThermostatModes"       : "attributeonly",
		"Thermostat:temperature"                    : "setTemperature",
		"Thermostat:thermostatFanMode"              : "setThermostatFanMode",
		"Thermostat:thermostatMode"                 : "setThermostatMode",
		"Thermostat:thermostatOperatingState"       : "attributeonly",
		"Thermostat:thermostatSetpoint"             : "attributeonly"
]

/***********************************************************************************************************************
 *
 * Release Notes
 *
 * Version: 0.2.6
 * Fixed issue with the app hanging when it is first installed.
 *
 * Version: 0.2.5
 * Renamed setThermostatTemperature to setTemperature to match other Hubitat drivers.
 *
 * Version: 0.2.4
 * Fixed a bug not properly managing integrated device subscriptions.
 * Fixed long standing issue that caused the start of importing devices when you used the back button.
 * Renamed Stay mode to Home mode to align with Hubitat terminology.
 * Added Hub Location Mode integration.
 * Added integration of main Elk device, no longer just child devices, to physical devices.
 * Added Smart Refresh to refresh integrated devices before refreshing Elk child devices.
 *
 * Version: 0.2.3
 * Fixed null variable issue.
 *
 * Version: 0.2.2
 * Added import of Custom and Counter value devices.
 *
 * Version: 0.2.1
 * Added integration of virtual child devices with physical HE devices.
 *
 * Version: 0.2.0
 * Added import of Speech device.  Also finished TTS, Notification and HSM Integration code.
 *
 * Version: 0.1.11
 * Removed unused password setting.
 *
 * Version: 0.1.10
 * Added import of Lighting devices.
 *
 * Version: 0.1.9
 * Added Thermostat to list of automatic import devices.
 *
 * Version: 0.1.8
 * Added Keypad device type to import for their built in temperature sensor.
 *
 * Version: 0.1.7
 * Combined Zone Type creation "Contact" and "Motion" and made "Zone" to fix a zone creation bug.
 *
 * Version: 0.1.6
 * Moved some zone creation code to Elk Driver
 *
 * Version: 0.1.5
 * Added support for manual inclusion of Elk M1 outputs and tasks
 * Added support for importing Elk M1 outputs, tasks and thermostats
 * Cleaned up app and related code
 *
 * Version: 0.1.4
 * Added support for importing Elk M1 zones
 *
 * Version: 0.1.3
 * Removed any unsupported features for now
 *
 * Version: 0.1.2
 * Added thermostat support (data receipt only)
 *
 * Version: 0.1.1
 * Ported code from Doug Beard Envisalink Integration
 * HSM integration not currently supported
 * All functionality not fully tested
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 * I - Must initialize the Elk M1 Device prior to using the Elk M1 App Import Zone functions
 *
 */

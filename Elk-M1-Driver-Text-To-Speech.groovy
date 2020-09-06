/***********************************************************************************************************************
 *
 *  A Hubitat Child Driver supporting Text To Speech using Elk M1's limited vocabulary
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
 *  Name: Elk M1 Driver Text To Speech
 *  Description: This will take a line of text and send the individual words to the Elk M1 to speak provided the
 *               words are in the M1's limited vocabulary of around 400 words.  The list of valid words are at the
 *               bottom of this driver.  It will also handle multi-digit numbers and the Elk M1's special "vm" phrases.
 *               Phrases vm1 - vm208 are the zone descriptions and vm209 - vm319 are the customizable phrases within
 *               Elk RP under Automation -> Voice.
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.1" }

import groovy.transform.Field

metadata {
	definition(name: "Elk M1 Driver Text To Speech", namespace: "captncode", author: "captncode") {
		capability "Actuator"
		capability "SpeechSynthesis"
	}
	preferences {
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

void updated() {
	log.warn device.label + " Updated..."
	log.warn "${device.label} description logging is: ${txtEnable == true}"
}

void installed() {
	log.warn device.label + " Installed..."
	device.updateSetting("txtEnable", [type: "bool", value: true])
}

void uninstalled() {
}

void parse(String description) {
	log.warn device.label + " parse(String description) received ${description}"
}

void parse(List description) {
	log.warn device.label + " parse(List description) received ${description}"
}

void speak(String text) {
	String sentence = ""
	String word
	int code, major
	BigDecimal testNbr
	boolean isNumber, isPhrase
	// Preparse text for special characters, numbers and temperatures
	text.replace("˚F", " degrees ").replace("˚C", " degrees ").replace("˚", " degrees ").replaceAll(/[^\w. ]+/, " ").split(" ").each {
		word = it.trim()
		if (word.length() > 1 && word.isNumber() && word ==~ /[\d.-]+/) { // Does this look like a number?
			testNbr = word.toBigDecimal()
			if (testNbr > -1000000 && testNbr <= 2147483647) {
				testNbr = testNbr.toInteger()
				if (testNbr <= 999999) { // Number can be spoken as-is.
					word = testNbr.toString()
				} else { // Add a space between digits so each one is spoken separately.
					word = testNbr.toString().replaceAll('.(?=.)', '$0 ')
				}
			}
		} else {
			word = word.replace(".", " ").replace("-", " ")
		}
		sentence += " " + word
	}
	text = sentence.trim()
	sentence = ""
	text.split(" ").each {
		isNumber = false
		isPhrase = false
		word = it.toLowerCase()
		if (word.length() > 0) {
			sentence += " " + it.trim()
			// Test to see if this is a number the M1 can speak
			if (word.isInteger() && word.length() > 1) {
				code = word.toInteger()
				if (code >= -999999 && code <= 999999)
					isNumber = true
			}
			// Test to see if this is a phrase ID starting with VM that the M1 can speak
			if (word.startsWith("vm") && word.length() > 2 && word.substring(2).isInteger()) {
				code = word.substring(2).toInteger()
				if (code > 0 && code < 320)
					isPhrase = true
			}
			// Speak what it can
			if (isNumber) {
				if (code < 0) {
					parent.speakWord(elkWords["low"]) // Best word I could find to denote negative
					code = 0 - code
				}
				if (code > 999) {
					major = code / 1000
					speakNumber(major)
					parent.speakWord(elkWords["thousand"])
					code = code - (major * 1000)
				}
				speakNumber(code)
			} else if (isPhrase) {
				parent.speakPhrase(code)
			} else {
				code = elkWords[word] ?: 0
				if (code == 0) {
					sentence += "(unknown)"
					log.trace "${device.label} ${word} not found in vocabulary"
				} else {
					parent.speakWord(code)
				}
			}
		}
	}
	if (sentence.length() > 0) {
		String descriptionText = device.label + " spoke" + sentence
		if (txtEnable)
			log.info descriptionText
		sendEvent(name: "speak", value: sentence.trim(), descriptionText: descriptionText)
	}
}

void speakNumber(int number) {
	int digit
	boolean hasHundred = false
	if (number > 99) {
		digit = number / 100
		parent.speakWord(elkWords[digit.toString()])
		parent.speakWord(elkWords["hundred"])
		number = number - (digit * 100)
		hasHundred = true
	}
	if (number > 0 || !hasHundred) {
		if (number > 20) {
			digit = number % 10
			number = number - digit
		} else {
			digit = 0
		}
		parent.speakWord(elkWords[number.toString()])
		if (digit > 0)
			parent.speakWord(elkWords[digit.toString()])
	}
}

@Field final Map elkWords = [
		"0"                 : 21,
		"1"                 : 22,
		"2"                 : 23,
		"3"                 : 24,
		"4"                 : 25,
		"5"                 : 26,
		"6"                 : 27,
		"7"                 : 28,
		"8"                 : 29,
		"9"                 : 30,
		"10"                : 31,
		"11"                : 32,
		"12"                : 33,
		"13"                : 34,
		"14"                : 35,
		"15"                : 36,
		"16"                : 37,
		"17"                : 38,
		"18"                : 39,
		"19"                : 40,
		"20"                : 41,
		"30"                : 42,
		"40"                : 43,
		"50"                : 44,
		"60"                : 45,
		"70"                : 46,
		"80"                : 47,
		"90"                : 48,
		"200ms_silence"     : 51,
		"500ms_silence"     : 52,
		"800hz_tone"        : 53,
		"a"                 : 54,
		"ac_power"          : 57,
		"access"            : 55,
		"acknowledged"      : 56,
		"activate"          : 58,
		"activated"         : 59,
		"active"            : 60,
		"adjust"            : 61,
		"air"               : 62,
		"alarm"             : 63,
		"alert"             : 64,
		"all"               : 65,
		"am"                : 66,
		"an"                : 67,
		"and"               : 68,
		"answer"            : 69,
		"any"               : 70,
		"are"               : 71,
		"area"              : 72,
		"arm"               : 73,
		"armed"             : 74,
		"at"                : 75,
		"attic"             : 76,
		"audio"             : 77,
		"authorized"        : 79,
		"auto"              : 78,
		"automatic"         : 80,
		"automation"        : 81,
		"auxiliary"         : 82,
		"away"              : 83,
		"b"                 : 84,
		"back"              : 85,
		"barn"              : 86,
		"basement"          : 87,
		"bathroom"          : 88,
		"battery"           : 89,
		"bedroom"           : 90,
		"been"              : 91,
		"bell"              : 92,
		"bottom"            : 93,
		"break"             : 94,
		"breakfast"         : 95,
		"bright"            : 96,
		"building"          : 97,
		"burglar"           : 98,
		"button"            : 99,
		"by"                : 100,
		"bypassed"          : 101,
		"cabinet"           : 102,
		"call"              : 103,
		"camera"            : 104,
		"cancel"            : 105,
		"carbon_monoxide"   : 106,
		"card"              : 107,
		"center"            : 108,
		"central"           : 109,
		"change"            : 110,
		"check"             : 111,
		"chime"             : 112,
		"circuit"           : 113,
		"clear"             : 114,
		"closed"            : 115,
		"closet"            : 116,
		"code"              : 117,
		"cold"              : 118,
		"condition"         : 119,
		"connect"           : 120,
		"control"           : 121,
		"cool"              : 122,
		"cooling"           : 123,
		"corner"            : 124,
		"crawlspace"        : 125,
		"custom1"           : 1,
		"custom10"          : 10,
		"custom2"           : 2,
		"custom3"           : 3,
		"custom4"           : 4,
		"custom5"           : 5,
		"custom6"           : 6,
		"custom7"           : 7,
		"custom8"           : 8,
		"custom9"           : 9,
		"danger"            : 126,
		"day"               : 127,
		"deck"              : 128,
		"decrease"          : 129,
		"defective"         : 130,
		"degrees"           : 131,
		"delay"             : 132,
		"den"               : 133,
		"denied"            : 134,
		"detected"          : 135,
		"detector"          : 136,
		"device"            : 137,
		"dial"              : 138,
		"dialing"           : 139,
		"dim"               : 140,
		"dining_room"       : 141,
		"disable"           : 142,
		"disarm"            : 143,
		"disarmed"          : 144,
		"dock"              : 145,
		"door"              : 146,
		"doors"             : 147,
		"down"              : 148,
		"driveway"          : 149,
		"east"              : 150,
		"eight"             : 29,
		"eighteen"          : 39,
		"eighty"            : 47,
		"eleven"            : 32,
		"emergency"         : 151,
		"enable"            : 152,
		"end"               : 153,
		"energy"            : 154,
		"enrollment"        : 155,
		"enter"             : 156,
		"enter_the"         : 159,
		"entering"          : 157,
		"entertainment"     : 158,
		"entry"             : 160,
		"environment"       : 161,
		"equipment"         : 162,
		"error"             : 163,
		"evacuate"          : 164,
		"event"             : 165,
		"exercise"          : 166,
		"exit"              : 168,
		"expander"          : 167,
		"exterior"          : 169,
		"f"                 : 170,
		"fail"              : 171,
		"failure"           : 172,
		"family_room"       : 173,
		"fan"               : 174,
		"feed"              : 175,
		"fence"             : 176,
		"fifteen"           : 36,
		"fifty"             : 44,
		"fire"              : 177,
		"first"             : 178,
		"five"              : 26,
		"flood"             : 179,
		"floor"             : 180,
		"followed"          : 181,
		"force"             : 182,
		"fountain"          : 183,
		"four"              : 25,
		"fourteen"          : 35,
		"fourty"            : 43,
		"foyer"             : 184,
		"freeze"            : 185,
		"front"             : 186,
		"full"              : 187,
		"furnace"           : 188,
		"fuse"              : 189,
		"game"              : 190,
		"garage"            : 191,
		"gas"               : 192,
		"gate"              : 193,
		"glass"             : 194,
		"go"                : 195,
		"good"              : 196,
		"goodbye"           : 197,
		"great"             : 198,
		"group"             : 199,
		"guest"             : 200,
		"gun"               : 201,
		"hall"              : 202,
		"hallway"           : 203,
		"hang_up"           : 205,
		"hanging_up"        : 204,
		"has"               : 206,
		"has_expired"       : 207,
		"have"              : 208,
		"hear_menu_options" : 209,
		"heat"              : 210,
		"help"              : 211,
		"high"              : 212,
		"hold"              : 213,
		"home"              : 214,
		"hot"               : 215,
		"hottub"            : 216,
		"house"             : 217,
		"humidity"          : 218,
		"hundred"           : 49,
		"hvac"              : 219,
		"if"                : 220,
		"immediately"       : 221,
		"in"                : 222,
		"in_the"            : 230,
		"inches"            : 223,
		"increase"          : 224,
		"inner"             : 225,
		"input"             : 226,
		"inside"            : 227,
		"instant"           : 228,
		"interior"          : 229,
		"intruder"          : 231,
		"intruder_message"  : 473,
		"intrusion"         : 232,
		"invalid"           : 233,
		"is"                : 234,
		"is_about_to_expire": 235,
		"is_active"         : 236,
		"is_armed"          : 237,
		"is_canceled"       : 238,
		"is_closed"         : 239,
		"is_disarmed"       : 240,
		"is_low"            : 241,
		"is_off"            : 242,
		"is_ok"             : 243,
		"is_on"             : 244,
		"is_open"           : 245,
		"jacuzzi"           : 246,
		"jewelry"           : 247,
		"keep"              : 248,
		"key"               : 249,
		"keypad"            : 250,
		"kitchen"           : 251,
		"lamp"              : 252,
		"laundry"           : 253,
		"lawn"              : 254,
		"leak"              : 255,
		"leave"             : 256,
		"left"              : 257,
		"less"              : 258,
		"level"             : 259,
		"library"           : 260,
		"light"             : 261,
		"lights"            : 262,
		"line"              : 263,
		"living_room"       : 264,
		"loading"           : 265,
		"lobby"             : 266,
		"location"          : 267,
		"lock"              : 268,
		"low"               : 269,
		"lower"             : 270,
		"m"                 : 271,
		"machine"           : 272,
		"mail"              : 273,
		"main"              : 274,
		"mains"             : 275,
		"manual"            : 276,
		"master"            : 277,
		"max"               : 278,
		"media"             : 279,
		"medical"           : 280,
		"medicine"          : 281,
		"memory"            : 282,
		"menu"              : 283,
		"message"           : 284,
		"middle"            : 285,
		"minute"            : 286,
		"missing"           : 287,
		"mode"              : 288,
		"module"            : 289,
		"monitor"           : 290,
		"more"              : 291,
		"motion"            : 292,
		"motor"             : 293,
		"next"              : 294,
		"night"             : 295,
		"nine"              : 30,
		"nineteen"          : 40,
		"ninety"            : 48,
		"no"                : 296,
		"normal"            : 297,
		"north"             : 298,
		"not"               : 299,
		"notified"          : 300,
		"now"               : 301,
		"number"            : 302,
		"nursery"           : 303,
		"of"                : 304,
		"off"               : 305,
		"office"            : 306,
		"oh"                : 307,
		"ok"                : 308,
		"on"                : 309,
		"one"               : 22,
		"online"            : 310,
		"only"              : 311,
		"open"              : 312,
		"operating"         : 313,
		"option"            : 314,
		"or"                : 315,
		"other"             : 316,
		"out"               : 317,
		"outlet"            : 318,
		"output"            : 319,
		"outside"           : 320,
		"over"              : 321,
		"overhead"          : 322,
		"panel"             : 323,
		"panic"             : 324,
		"parking"           : 325,
		"partition"         : 326,
		"patio"             : 327,
		"pause"             : 328,
		"perimeter"         : 329,
		"personal"          : 330,
		"phone"             : 331,
		"place"             : 332,
		"play"              : 333,
		"please"            : 334,
		"plus"              : 335,
		"pm"                : 336,
		"police"            : 337,
		"pool"              : 338,
		"porch"             : 339,
		"port"              : 340,
		"pound"             : 341,
		"pounds"            : 342,
		"power"             : 343,
		"press"             : 344,
		"pressure"          : 345,
		"problem"           : 346,
		"program"           : 347,
		"protected"         : 348,
		"pump"              : 349,
		"radio"             : 350,
		"raise"             : 351,
		"ready"             : 352,
		"rear"              : 353,
		"receiver"          : 354,
		"record"            : 355,
		"recreation"        : 356,
		"relay"             : 357,
		"remain_calm"       : 358,
		"remote"            : 359,
		"repeat"            : 360,
		"report"            : 361,
		"reporting"         : 362,
		"reset"             : 363,
		"restored"          : 364,
		"return"            : 365,
		"right"             : 366,
		"roof"              : 367,
		"room"              : 368,
		"running"           : 369,
		"safe"              : 370,
		"save"              : 371,
		"screen"            : 372,
		"second"            : 373,
		"secure"            : 374,
		"security"          : 375,
		"select"            : 376,
		"sensor"            : 377,
		"serial"            : 378,
		"service"           : 379,
		"set"               : 380,
		"setback"           : 381,
		"setpoint"          : 382,
		"setting"           : 383,
		"seven"             : 28,
		"seventeen"         : 38,
		"seventy"           : 46,
		"shed"              : 384,
		"shipping"          : 385,
		"shock"             : 386,
		"shop"              : 387,
		"shorted"           : 388,
		"shunted"           : 389,
		"side"              : 390,
		"silence"           : 391,
		"siren"             : 392,
		"six"               : 27,
		"sixteen"           : 37,
		"sixty"             : 45,
		"sliding"           : 393,
		"smoke"             : 394,
		"someone"           : 395,
		"south"             : 396,
		"spare"             : 397,
		"speaker"           : 398,
		"sprinkler"         : 399,
		"stairs"            : 400,
		"stairway"          : 401,
		"star"              : 402,
		"start"             : 403,
		"status"            : 404,
		"stay"              : 405,
		"stock"             : 406,
		"stop"              : 407,
		"storage"           : 408,
		"storm"             : 409,
		"studio"            : 410,
		"study"             : 411,
		"sump"              : 412,
		"sun"               : 413,
		"switch"            : 414,
		"system"            : 415,
		"tamper"            : 416,
		"tank"              : 417,
		"task"              : 418,
		"telephone"         : 419,
		"television"        : 420,
		"temperature"       : 421,
		"ten"               : 31,
		"test"              : 422,
		"thank_you"         : 423,
		"that"              : 424,
		"the"               : 425,
		"theater"           : 426,
		"thermostat"        : 427,
		"third"             : 428,
		"thirteen"          : 34,
		"thirty"            : 42,
		"thousand"          : 50,
		"three"             : 24,
		"time"              : 429,
		"toggle"            : 430,
		"top"               : 431,
		"transformer"       : 432,
		"transmitter"       : 433,
		"trespassing"       : 434,
		"trouble"           : 435,
		"turn"              : 436,
		"twelve"            : 33,
		"twenty"            : 41,
		"twice"             : 437,
		"two"               : 23,
		"type"              : 438,
		"under"             : 439,
		"unit"              : 440,
		"unlocked"          : 441,
		"unoccupied"        : 442,
		"up"                : 443,
		"user"              : 444,
		"utility"           : 445,
		"vacation"          : 446,
		"valve"             : 447,
		"video"             : 448,
		"violated"          : 449,
		"visitor"           : 450,
		"wake_up"           : 451,
		"walk"              : 452,
		"wall"              : 453,
		"warehouse"         : 454,
		"warning"           : 455,
		"water"             : 456,
		"way"               : 457,
		"welcome"           : 458,
		"west"              : 459,
		"what"              : 460,
		"when"              : 461,
		"where"             : 462,
		"will"              : 463,
		"window"            : 464,
		"windows"           : 465,
		"with"              : 466,
		"work"              : 467,
		"yard"              : 468,
		"year"              : 469,
		"you"               : 470,
		"zero"              : 21,
		"zone"              : 471,
		"zones"             : 472
]

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.1
 * Strongly typed commands
 *
 * 0.1.0
 * New child driver for Elk M1 Text To Speech
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 *
 ***********************************************************************************************************************/

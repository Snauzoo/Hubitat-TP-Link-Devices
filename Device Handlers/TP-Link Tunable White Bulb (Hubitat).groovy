/*	TP Link Bulbs Device Handler, 2018 Version 3.5

	Copyright 2018 Dave Gutheinz and Anthony Ramirez

Licensed under the Apache License, Version 2.0(the "License");
you may not use this  file except in compliance with the
License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, 
software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific 
language governing permissions and limitations under the 
License.

Discalimer:  This Service Manager and the associated Device 
Handlers are in no way sanctioned or supported by TP-Link.  
All  development is based upon open-source data on the 
TP-Link devices; primarily various users on GitHub.com.

===== History ================================================
11.11.18	Update to Version 3.5:
			a.  Single Drivers for manual Hub Installation, 
				automated Hub Installation, and for Kasa
				Account based installation.
            b.	Update to match the Hubitat Driver input - 
				output paradigm.
11.28.18	a.	Fixed color naming for lowRez Hue.
			b.	Added Level to the color map from the 
				Device page.
			c.	Change scale of Transition Time to match
				the Device page definition.
//	===== Device Type Identifier ===========================*/
	def driverVer() { return "3.5.02" }
//	def deviceType() { return "Soft White Bulb" }
	def deviceType() { return "Tunable White Bulb" }
//	def deviceType() { return "Color Bulb" }
//	==========================================================
metadata {
	definition (name: "TP-Link/Kasa ${deviceType()}", 
    			namespace: "davegut", 
                author: "Dave Gutheinz") {
        capability "Light"
		capability "Switch"
		capability "Switch Level"
        capability "Actuator"
		capability "Refresh"
		if (deviceType() != "Soft White Bulb") {
			capability "Color Temperature"
			command "setCircadian"
			attribute "circadianState", "string"
		}
		if (deviceType() == "Color Bulb") {
			capability "Color Control"
			capability "Color Mode"
		}
	}

    def hueScale = [:]
    hueScale << ["highRez": "High Resolution (0 - 360)"]
    hueScale << ["lowRez": "Low Resolution (0 - 100)"]

    preferences {
		if (deviceType() == "Color Bulb") {
	        input ("hue_Scale", "enum", title: "High or Low Res Hue", options: hueScale)
        }
        input ("transition_Time", "num", title: "Default Transition time (seconds)")
		if (getDataValue("installType") != "Kasa Account")  {
			input ("device_IP", "text", title: "Device IP (Hub Only, NNN.NNN.N.NNN)")
			input ("gateway_IP", "text", title: "Gateway IP (Hub Only, NNN.NNN.N.NNN)")
		}
	}
}

//	===== Update when installed or setting changed =====
def installed() {
	log.info "Installing ${device.label}..."
    setHueScale("lowRez")
	if(getDataValue("installType") == null) { setInstallType("Node Applet") }
	updated()
}

def ping() {
	refresh()
}

def updated() {
	log.info "Updating ${device.label}..."
	unschedule()
    runEvery5Minutes(refresh)
    if (device_IP) { setDeviceIP(device_IP) }
    if (gateway_IP) { setGatewayIP(gateway_IP) }
    if (hue_Scale) { setHueScale(hue_Scale) }
//	Account for transition time scale change due to Hubitat devices page scale note.
	if (!transition_Time) { setLightTransTime(0) }
	def trans_Time = getDataValue("transTime").toInteger()
	if (transition_Time) { trans_Time = transition_Time.toInteger() }
	if (trans_Time > 100) { trans_Time = (trans_Time / 1000).toInteger() }
	setLightTransTime(trans_Time)
	runIn(2, refresh)
}

void uninstalled() {
	try {
		def alias = device.label
		log.info "Removing device ${alias} with DNI = ${device.deviceNetworkId}"
		parent.removeChildDevice(alias, device.deviceNetworkId)
	} catch (ex) {
		log.info "${device.name} ${device.label}: Either the device was manually installed or there was an error"
        log.info "No Parent Application"
	}
}

//	===== Basic Bulb Control/Status =====
def on() {
	def transTime = 1000*getDataValue("transTime").toInteger()
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"on_off":1,"transition_period":${transTime}}}}""", "deviceCommand", "commandResponse")
}

def off() {
	def transTime = 1000*getDataValue("transTime").toInteger()
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"on_off":0,"transition_period":${transTime}}}}""", "deviceCommand", "commandResponse")
}

def setLevel(percentage) {
	def transTime = getDataValue("transTime")
	setLevel(percentage, transTime)
}

def setLevel(percentage, rate) {
	if (percentage < 0 || percentage > 100) {
		log.error "$device.name $device.label: Entered brightness is not from 0...100"
		percentage = 50
	}
	percentage = percentage as int
	rate = 1000*rate.toInteger()
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"ignore_default":1,"on_off":1,"brightness":${percentage},"transition_period":${rate}}}}""", "deviceCommand", "commandResponse")
}

def setColorTemperature(kelvin) {
	if (kelvin == null) kelvin = state.lastColorTemp
	switch(deviceType()) {
		case "Tunable White Bulb" :
			if (kelvin < 2700) kelvin = 2700
			if (kelvin > 6500) kelvin = 6500
			break
		defalut:
			if (kelvin < 2500) kelvin = 2500
			if (kelvin > 9000) kelvin = 9000
	}
	kelvin = kelvin as int
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"ignore_default":1,"on_off":1,"color_temp": ${kelvin},"hue":0,"saturation":0}}}""", "deviceCommand", "commandResponse")
}

def setCircadian() {
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"mode":"circadian"}}}""", "deviceCommand", "commandResponse")
}

def setHue(hue) {
	if (hue == null) hue = state.lastHue
	saturation = state.lastSaturation
	setColor([hue: hue, saturation: saturation])
}

def setSaturation(saturation) {
	if (saturation == null) saturation = state.lastSaturation
	hue = state.lastHue
	setColor([hue: hue, saturation: saturation])
}

def setColor(Map color) {
	if (color == null) color = [hue: state.lastHue, saturation: state.lastSaturation, level: device.currentValue("level")]
	def percentage = 100
	if (!color.level) { 
		percentage = device.currentValue("level")
	} else {
		percentage = color.level
	}
    def hue = color.hue.toInteger()
    if (getDataValue("hueScale") == "lowRez") { hue = (hue * 3.6).toInteger() }
	def saturation = color.saturation as int
    if (hue < 0 || hue > 360 || saturation < 0 || saturation > 100) {
        log.error "$device.name $device.label: Entered hue or saturation out of range!"
        return
    }
	sendCmdtoServer("""{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"ignore_default":1,"on_off":1,"brightness":${percentage},"color_temp":0,"hue":${hue},"saturation":${saturation}}}}""", "deviceCommand", "commandResponse")
}

def refresh(){
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "refreshResponse")
}

def refreshResponse(cmdResponse){
	def status = cmdResponse.system.get_sysinfo.light_state
	def onOff = status.on_off
	if (onOff == 1) {
		onOff = "on"
	} else {
		onOff = "off"
		status = status.dft_on_state
	}
	sendEvent(name: "switch", value: onOff)
	def level = status.brightness
	sendEvent(name: "level", value: level)
	switch(deviceType()) {
		case "Soft White Bulb" :
			log.info "$device.name $device.label: Power: ${onOff} / Brightness: ${level}%"
			break
		case "Tunable White Bulb" :
			def circadianMode = status.mode
			def color_temp = status.color_temp
			sendEvent(name: "circadianMode", value: circadianMode)
			sendEvent(name: "colorTemperature", value: color_temp)
	        state.lastColorTemp = color_temp
	        setColorTempData(color_temp)
			log.info "$device.name $device.label: Power: ${onOff} / Brightness: ${level}% / Circadian Mode: ${circadianMode} / Color Temp: ${color_temp}K"
			break
		default:	//	Color Bulb
			def circadianMode = status.mode
			def color_temp = status.color_temp
			def hue = status.hue.toInteger()
			def saturation = status.saturation
			def color = [:]
	        if (getDataValue("hueScale") == "lowRez") { hue = (hue / 3.6).toInteger() }
			color << ["hue" : hue]
			color << ["saturation" : status.saturation]
			sendEvent(name: "circadianMode", value: circadianMode)
			sendEvent(name: "colorTemperature", value: color_temp)
			sendEvent(name: "hue", value: hue)
			sendEvent(name: "saturation", value: saturation)
			sendEvent(name: "color", value: color)
			if (color_temp.toInteger() == 0) {
				state.lastHue = hue
				state.lastSaturation = saturation
		        setRgbData(hue)
			} else {
				state.lastColorTemp = color_temp
		        setColorTempData(color_temp)
			}
			log.info "$device.name $device.label: Power: ${onOff} / Brightness: ${level}% / Circadian Mode: ${circadianMode} / Color Temp: ${color_temp}K / Color: ${color}"
	}
}

def setColorTempData(temp){
    def value = temp.toInteger()
    def genericName
    if (value < 2400) genericName = "Sunrise"
    else if (value < 2800) genericName = "Incandescent"
    else if (value < 3300) genericName = "Soft White"
    else if (value < 3500) genericName = "Warm White"
    else if (value < 4150) genericName = "Moonlight"
    else if (value <= 5000) genericName = "Horizon"
    else if (value < 5500) genericName = "Daylight"
    else if (value < 6000) genericName = "Electronic"
    else if (value <= 6500) genericName = "Skylight"
    else if (value < 20000) genericName = "Polar"
	descriptionText = "${device.getDisplayName()} Color Mode is CT"
	log.info "${descriptionText}"
 	sendEvent(name: "colorMode", value: "CT" ,descriptionText: descriptionText)
    def descriptionText = "${device.getDisplayName()} color is ${genericName}"
	log.info "${descriptionText}"
    sendEvent(name: "colorName", value: genericName ,descriptionText: descriptionText)
}

def setRgbData(hue){
    def colorName
    hue = hue.toInteger()
	if (getDataValue("hueScale") == "lowRez") { hue = (hue * 3.6).toInteger() }
    switch (hue.toInteger()){
        case 0..15: colorName = "Red"
            break
        case 16..45: colorName = "Orange"
            break
        case 46..75: colorName = "Yellow"
            break
        case 76..105: colorName = "Chartreuse"
            break
        case 106..135: colorName = "Green"
            break
        case 136..165: colorName = "Spring"
            break
        case 166..195: colorName = "Cyan"
            break
        case 196..225: colorName = "Azure"
            break
        case 226..255: colorName = "Blue"
            break
        case 256..285: colorName = "Violet"
            break
        case 286..315: colorName = "Magenta"
            break
        case 316..345: colorName = "Rose"
            break
        case 346..360: colorName = "Red"
            break
    }
	descriptionText = "${device.getDisplayName()} Color Mode is RGB"
	log.info "${descriptionText}"
 	sendEvent(name: "colorMode", value: "RGB" ,descriptionText: descriptionText)
    def descriptionText = "${device.getDisplayName()} color is ${colorName}"
	log.info "${descriptionText}"
    sendEvent(name: "colorName", value: colorName ,descriptionText: descriptionText)
}

//	===== Send the Command =====
private sendCmdtoServer(command, hubCommand, action) {
	try {
    	def installType = getDataValue("installType")
		if (installType == "Kasa Account") {
			sendCmdtoCloud(command, hubCommand, action)
		} else {
	    	def deviceIP = getDataValue("deviceIP")
	        def gatewayIP = getDataValue("gatewayIP")
			if (deviceIP =~ null && gatewayIP =~ null) {
				sendEvent(name: "switch", value: "unavailable", descriptionText: "Please input Device IP / Gateway IP")
				sendEvent(name: "deviceError", value: "No Hub Address Data")
			} else {
				sendCmdtoHub(command, hubCommand, action)
			}
		}
	} catch (ex) {
		log.error "Sending Command Exception: ", ex
	}
}

private sendCmdtoCloud(command, hubCommand, action){
	def appServerUrl = getDataValue("appServerUrl")
	def deviceId = getDataValue("deviceId")
	def cmdResponse = parent.sendDeviceCmd(appServerUrl, deviceId, command)
	String cmdResp = cmdResponse.toString()
	if (cmdResp.substring(0,5) == "ERROR"){
		def errMsg = cmdResp.substring(7,cmdResp.length())
		log.error "${device.name} ${device.label}: ${errMsg}"
		sendEvent(name: "switch", value: "unavailable", descriptionText: errMsg)
		sendEvent(name: "deviceError", value: errMsg)
		action = ""
	} else {
		sendEvent(name: "deviceError", value: "OK")
	}
	actionDirector(action, cmdResponse)
}

private sendCmdtoHub(command, hubCommand, action){
    def gatewayIP = getDataValue("gatewayIP")
    def deviceIP = getDataValue("deviceIP")
	def headers = [:] 
	headers.put("HOST", "$gatewayIP:8082")	//	Same as on Hub.
	headers.put("tplink-iot-ip", deviceIP)
	headers.put("tplink-command", command)
	headers.put("action", action)
	headers.put("command", hubCommand)
	sendHubCommand(new hubitat.device.HubAction([headers: headers], null, [callback: hubResponseParse]))
}

def hubResponseParse(response) {
	def action = response.headers["action"]
	def cmdResponse = parseJson(response.headers["cmd-response"])
	if (cmdResponse == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine in hubResponseParse")
		sendEvent(name: "deviceError", value: "TCP Timeout in Hub")
	} else {
		sendEvent(name: "deviceError", value: "OK")
		actionDirector(action, cmdResponse)
	}
}

def actionDirector(action, cmdResponse) {
	switch(action) {
		case "commandResponse" :
			refresh()
			break
		case "refreshResponse" :
			refreshResponse(cmdResponse)
			break
		default:
			log.info "Interface Error. See SmartApp and Device error message."
	}
}

//	===== Child / Parent Interchange =====
def setAppServerUrl(newAppServerUrl) {
	updateDataValue("appServerUrl", newAppServerUrl)
	log.info "Updated appServerUrl for ${device.name} ${device.label}"
}

def setLightTransTime(newTransTime) {
	updateDataValue("transTime", "${newTransTime}")
	log.info "Light Transition Time for ${device.name} ${device.label} set to ${newTransTime} seconds"
}

def setDeviceIP(deviceIP) { 
	updateDataValue("deviceIP", deviceIP) 	//	gatewayIP must be in form NNN.NNN.N.NNN
	log.info "${device.name} ${device.label} device IP set to ${deviceIP}"
}

def setGatewayIP(gatewayIP) { 
	updateDataValue("gatewayIP", gatewayIP) 	//	gatewayIP must be in form NNN.NNN.N.NNN
	log.info "${device.name} ${device.label} gateway IP set to ${gatewayIP}"
}

def setInstallType(installType) {
	updateDataValue("installType", installType)
	log.info "${device.name} ${device.label} Installation Type set to ${installType}"
}

def setHueScale(hueScale) {
	updateDataValue("hueScale", hueScale)
	log.info "${device.name} ${device.label} Hue Scale set to ${hueScale}"
}
//end-of-file
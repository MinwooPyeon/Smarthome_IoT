# IOT Develop Environment Specification
## 1. Purpose
Make Matter Hub Device & IR Send Device for Remote Control Legacy Electrons.
Analyse Data Signal with Matlab and visiblize with MFC.

## 2. Develop Environment
### 2.1. IoT Hub Device
* Board : Raspberry Pi
* Platform : Ubuntu 22.04
* Protocol : Matter, Mqtt
* Actuator : DHT-11, VS1838, Microphone
### 2.2. IR Send Device
* Board : ESP32 Dev Kit 1
* Protocol : Matter
* Actuator : S346
### 2.3. Person Detect Device
* Board : Arduino Uno
* Protocol : Mqtt
* Actuator : HC-SR501

## 3. Feature
### 3.1. Iot Hub Device
* Communicate with IR Send Device, Server, Matlab, MFC, Person Detect Device
* Sensing Space Environment
* Measure IR Signal
* Get Order from Voice
### 3.2. IR Send Device
* Communicate with Iot Hub Device
* Send IR Signal to Electrons
### 3.3. Person Detect Device
* Communicate with Iot Hub Device
* Sensing Person Movement
### 3.4. Matlab
* Communicate with Iot Hub Device, MFC
* Analyse Sensor Value
### 3.5. MFC
* Communicate with Matlab, Server
* Visualize Sensor Value Analyse Result
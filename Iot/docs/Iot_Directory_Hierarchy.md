# IoT Directory Hierarchy
## 1. IoT Hub Device
* IotHub
    * main.cpp
    * src
        * actuator
            * sensor.cpp
            * dht-11.cpp
            * vs1838.cpp
            * microphone.cpp
        * network
            * mqtt
                * mqttPublisher.cpp
                * mqttSubscriber.cpp
            * matter
                * matterPublisher.cpp
        * ai
        * device
            * deviceState.cpp
            * deviceFunction.cpp
        * util
            * jsonParser.cpp
            * jsonBuilder.cpp
        
    * header
        * actuator
            * sensor.h
            * dht-11.h
            * vs1838.h
            * microphone.h
        * network
            * mqtt
                * mqttPublisher.h
                * mqttSubscriber.h
            * matter
                * matterPublisher.h
        * ai
        * device
            * deviceState.h
            * deviceFunction.h

## 2. IR Send Device
* IRSendDevice
    * main.cpp
    * src
        * actuator
            * s346.cpp
        * network
            * mqtt
                * mqttPublisher.cpp
                * mqttSubscriber.cpp
            * matter
                * matterSubscriber.cpp
        * device
            * functionMapper.cpp
            * deviceState.cpp
        * util
    * header
        * actuator
            * s346.h
        * network
            * mqtt
                * mqttPublisher.h
                * mqttSubscriber.h
            * matter
                * matterSubscriber.h
        * device
            * functionMapper.h
            * deviceState.h

## 3. Person Detect Device
* Main.cpp


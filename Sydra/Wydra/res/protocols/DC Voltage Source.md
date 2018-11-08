# DC Voltage Source

## Feature

A DC Voltage Source provide constant voltages to the loads. 

-------

## Extends

@protocol(Instrument)

-------

## APIs

 ```getChannelNumber(): Int```
 
 return the independent channel number of the Instrument.
 
 ```getVoltageRange(channel: Int): Double```
 
 return the voltage setting range for specific ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded).
 
 ```getCurrentLimitRange(channel: Int): Double```
 
return the current limit setting range for specific ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded).
 
 ```setVoltage(channel:Int, voltage: Double): Unit``` 
 
 set the voltage for specific ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded). ```voltage``` should be in the range of ```getVoltageRange(channel)```, or it will be trim.
 
 ```setVoltages(voltages: List[Double]): Unit```
 
 set the voltages for every channel. The length of ```voltages``` should be equal to ```getChannelNumber()```. Each value in ```voltages``` should be in the range of ```getVoltageRange(channel)```, or it will be trim.

 ```getVoltageSetPoint(channel: Int): Double```

get the voltage set point for ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded). 

 ```setCurrentLimit(channel: Int, current: Double): Unit```
 
  set the current limit for specific ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded). ```current``` should be in the range of ```getCurrentLimitRange(channel)```, or it will be trim.

 ```setCurrentLimits(currents: Double): Unit```
 
  set the current limits for every channel. The length of ```currents``` should be equal to ```getChannelNumber()```. Each value in ```currents``` should be in the range of ```getCurrentLimitRange(channel)```, or it will be trim.

 ```getCurrentLimitSetPoint(channel: Int): Double```
 
 get the current set point for ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded). 

 ```setOutputStatus(channel: Int, status: Boolean): Unit```
 
 set the output status for specific ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded).
 
 ```setOutputStatuses(statuses: List[Boolean]): Unit```
 
 set the output statuses for every channel. The length of ```statuses``` should be equal to ```getChannelNumber()```.
 
 ```getOutputStatus(channel: Int): Boolean```

 get the output status for specific ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded).

 ```measureVoltage(channel: Int): Double```
 
 measure the output voltage for ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded).
 
 ```measureVoltages(): List[Double]```
 
 measure the output voltages for every channel.

 ```measureCurrent(channel: Int): Double```
 
 measure the output current for ```channel```. ```channel``` should be a Int between 0 (included) and ```getChannelNumber()``` (excluded).
 
 ```measureCurrents(): List[Double]```
 
 measure the output currents for every channel.

--------

_Author: Hwaipy_

_Version: 1.0.0_

_Date: 18 Jun. 2018_



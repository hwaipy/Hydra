cd ../../Pydra/Services/MultiMeter/
set PYTHONPATH=%cd%\..\..
python KeySightMultiMeter.py -m 34470A -r TCPIP0::192.168.25.52::inst0::INSTR -a 192.168.25.27 -s KeySightMultiMeter_34470A_1
pause

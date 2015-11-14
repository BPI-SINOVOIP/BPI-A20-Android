<!--
   Copyright 2011 The Android Open Source Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Getevent #

The `getevent` tool runs on the device and provides information about input
devices and a live dump of kernel input events.

It is very useful tool for ensuring that device drivers are reporing the
expected set of capabilities for each input device and are generating the
desired stream of input events.

## Usage ##

### Showing Device Capabilities ###

It is often quite useful to see all of the keys and axes that a device reports.
Use the `-p` option to do that.

Here is a list of all of the Linux key codes and other events that a
particular keyboard says it supports.

    $ adb shell su -- getevent -p

      name:     "Motorola Bluetooth Wireless Keyboard"
      events:
        KEY (0001): 0001  0002  0003  0004  0005  0006  0007  0008 
                    0009  000a  000b  000c  000d  000e  000f  0010 
                    0011  0012  0013  0014  0015  0016  0017  0018 
                    0019  001a  001b  001c  001d  001e  001f  0020 
                    0021  0022  0023  0024  0025  0026  0027  0028 
                    0029  002a  002b  002c  002d  002e  002f  0030 
                    0031  0032  0033  0034  0035  0036  0037  0038 
                    0039  003a  003b  003c  003d  003e  003f  0040 
                    0041  0042  0043  0044  0045  0046  0047  0048 
                    0049  004a  004b  004c  004d  004e  004f  0050 
                    0051  0052  0053  0055  0056  0057  0058  0059 
                    005a  005b  005c  005d  005e  005f  0060  0061 
                    0062  0063  0064  0066  0067  0068  0069  006a 
                    006b  006c  006d  006e  006f  0071  0072  0073 
                    0074  0075  0077  0079  007a  007b  007c  007d 
                    007e  007f  0080  0081  0082  0083  0084  0085 
                    0086  0087  0088  0089  008a  008c  008e  0090 
                    0096  0098  009b  009c  009e  009f  00a1  00a3 
                    00a4  00a5  00a6  00ab  00ac  00ad  00b0  00b1 
                    00b2  00b3  00b4  00b7  00b8  00b9  00ba  00bb 
                    00bc  00bd  00be  00bf  00c0  00c1  00c2  00d9 
                    00f0  0110  0111  0112  01ba 
        REL (0002): 0000  0001  0008 
        ABS (0003): 0028  : value 223, min 0, max 255, fuzz 0, flat 0, resolution 0
                    0029  : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
                    002a  : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
                    002b  : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
        MSC (0004): 0004 
        LED (0011): 0000  0001  0002  0003  0004 
      input props:
        <none>

The `-i` option shows even more information than `-p`, including HID mapping tables
and debugging information.

The `-l` option uses textual labels for all event codes, which is handy.

    $ adb shell su -- getevent -lp /dev/input/event1

      name:     "Melfas MMSxxx Touchscreen"
      events:
        ABS (0003): ABS_MT_SLOT           : value 0, min 0, max 9, fuzz 0, flat 0, resolution 0
                    ABS_MT_TOUCH_MAJOR    : value 0, min 0, max 30, fuzz 0, flat 0, resolution 0
                    ABS_MT_POSITION_X     : value 0, min 0, max 720, fuzz 0, flat 0, resolution 0
                    ABS_MT_POSITION_Y     : value 0, min 0, max 1280, fuzz 0, flat 0, resolution 0
                    ABS_MT_TRACKING_ID    : value 0, min 0, max 65535, fuzz 0, flat 0, resolution 0
                    ABS_MT_PRESSURE       : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
      input props:
        INPUT_PROP_DIRECT

### Showing Live Events ###

This is what a two finger multitouch gesture looks like for a touch screen
that is using the Linux multitouch input protocol "B".  We use the `-l` option
to show textual labels and `-t` to show timestamps.

    $ adb shell su -- getevent -lt /dev/input/event1

    [   78826.389007] EV_ABS       ABS_MT_TRACKING_ID   0000001f
    [   78826.389038] EV_ABS       ABS_MT_PRESSURE      000000ab
    [   78826.389038] EV_ABS       ABS_MT_POSITION_X    000000ab
    [   78826.389068] EV_ABS       ABS_MT_POSITION_Y    0000025b
    [   78826.389068] EV_ABS       ABS_MT_SLOT          00000001
    [   78826.389068] EV_ABS       ABS_MT_TRACKING_ID   00000020
    [   78826.389068] EV_ABS       ABS_MT_PRESSURE      000000b9
    [   78826.389099] EV_ABS       ABS_MT_POSITION_X    0000019e
    [   78826.389099] EV_ABS       ABS_MT_POSITION_Y    00000361
    [   78826.389099] EV_SYN       SYN_REPORT           00000000
    [   78826.468688] EV_ABS       ABS_MT_SLOT          00000000
    [   78826.468688] EV_ABS       ABS_MT_TRACKING_ID   ffffffff
    [   78826.468719] EV_ABS       ABS_MT_SLOT          00000001
    [   78826.468719] EV_ABS       ABS_MT_TRACKING_ID   ffffffff
    [   78826.468719] EV_SYN       SYN_REPORT           00000000

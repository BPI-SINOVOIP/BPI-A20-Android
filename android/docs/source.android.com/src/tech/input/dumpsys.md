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

# Dumpsys #

The `dumpsys` tool runs on the device and dumps interesting information
about the status of system services.

## Usage ##

The input system is part of the window manager.  To dump its state,
run the following command.

    $ adb shell su -- dumpsys window

    WINDOW MANAGER INPUT (dumpsys window input)
    Event Hub State:
      BuiltInKeyboardId: -1
      Devices:
    ...

The set of information that is reported varies depending on the version of Android.

### Event Hub State ###

The `EventHub` component is responsible for communicating with the kernel device
drivers and identifying device capabilities.  Accordingly, its state shows
information about how devices are configured.

    Event Hub State:
      BuiltInKeyboardId: -1
      Devices:
        3: tuna-gpio-keypad
          Classes: 0x00000001
          Path: /dev/input/event2
          Location:
          UniqueId:
          Identifier: bus=0x0000, vendor=0x0000, product=0x0000, version=0x0000
          KeyLayoutFile: /system/usr/keylayout/tuna-gpio-keypad.kl
          KeyCharacterMapFile: /system/usr/keychars/tuna-gpio-keypad.kcm
          ConfigurationFile:
        5: Tuna Headset Jack
          Classes: 0x00000080
          Path: /dev/input/event5
          Location: ALSA
          UniqueId:
          Identifier: bus=0x0000, vendor=0x0000, product=0x0000, version=0x0000
          KeyLayoutFile:
          KeyCharacterMapFile:
          ConfigurationFile:
        6: Melfas MMSxxx Touchscreen
          Classes: 0x00000014
          Path: /dev/input/event1
          Location: 3-0048/input0
          UniqueId:
          Identifier: bus=0x0018, vendor=0x0000, product=0x0000, version=0x0000
          KeyLayoutFile:
          KeyCharacterMapFile:
          ConfigurationFile: /system/usr/idc/Melfas_MMSxxx_Touchscreen.idc
        7: Motorola Bluetooth Wireless Keyboard
          Classes: 0x8000000b
          Path: /dev/input/event6
          Location: 0C:DF:A4:B3:2D:BA
          UniqueId: 00:0F:F6:80:02:CD
          Identifier: bus=0x0005, vendor=0x22b8, product=0x093d, version=0x0288
          KeyLayoutFile: /system/usr/keylayout/Vendor_22b8_Product_093d.kl
          KeyCharacterMapFile: /system/usr/keychars/Generic.kcm
          ConfigurationFile:

#### Things To Look For ####

1.  All of the expected input devices are present.

2.  Each input device has an appropriate key layout file, key character map file
    and input device configuration file.  If the files are missing or contain
    syntax errors, then they will not be loaded.

3.  Each input device is being classified correctly.  The bits in the `Classes`
    field correspond to flags in `EventHub.h` such as `INPUT_DEVICE_CLASS_TOUCH_MT`.

4.  The `BuiltInKeyboardId` is correct.  If the device does not have a built-in keyboard,
    then the id must be `-1`, otherwise it should be the id of the built-in keyboard.

    If you observe that the `BuiltInKeyboardId` is not `-1` but it should be, then
    you are missing a key character map file for a special function keypad somewhere.
    Special function keypad devices should have key character map files that contain
    just the line `type SPECIAL_FUNCTION` (that's what in the `tuna-gpio-keykad.kcm`
    file we see mentioned above).

### Input Reader State ###

The `InputReader` is responsible for decoding input events from the kernel.
Its state dump shows information about how each input device is configured
and recent state changes that occurred, such as key presses or touches on
the touch screen.

This is what a special function keypad looks like:

    Input Reader State:
      Device 3: tuna-gpio-keypad
        IsExternal: false
        Sources: 0x00000101
        KeyboardType: 1
        Keyboard Input Mapper:
          Parameters:
            AssociatedDisplayId: -1
            OrientationAware: false
          KeyboardType: 1
          Orientation: 0
          KeyDowns: 0 keys currently down
          MetaState: 0x0
          DownTime: 75816923828000

Here is a touch screen.  Notice all of the information about the resolution of
the device and the calibration parameters that were used.

      Device 6: Melfas MMSxxx Touchscreen
        IsExternal: false
        Sources: 0x00001002
        KeyboardType: 0
        Motion Ranges:
          X: source=0x00001002, min=0.000, max=719.001, flat=0.000, fuzz=0.999
          Y: source=0x00001002, min=0.000, max=1279.001, flat=0.000, fuzz=0.999
          PRESSURE: source=0x00001002, min=0.000, max=1.000, flat=0.000, fuzz=0.000
          SIZE: source=0x00001002, min=0.000, max=1.000, flat=0.000, fuzz=0.000
          TOUCH_MAJOR: source=0x00001002, min=0.000, max=1468.605, flat=0.000, fuzz=0.000
          TOUCH_MINOR: source=0x00001002, min=0.000, max=1468.605, flat=0.000, fuzz=0.000
          TOOL_MAJOR: source=0x00001002, min=0.000, max=1468.605, flat=0.000, fuzz=0.000
          TOOL_MINOR: source=0x00001002, min=0.000, max=1468.605, flat=0.000, fuzz=0.000
        Touch Input Mapper:
          Parameters:
            GestureMode: spots
            DeviceType: touchScreen
            AssociatedDisplay: id=0, isExternal=false
            OrientationAware: true
          Raw Touch Axes:
            X: min=0, max=720, flat=0, fuzz=0, resolution=0
            Y: min=0, max=1280, flat=0, fuzz=0, resolution=0
            Pressure: min=0, max=255, flat=0, fuzz=0, resolution=0
            TouchMajor: min=0, max=30, flat=0, fuzz=0, resolution=0
            TouchMinor: unknown range
            ToolMajor: unknown range
            ToolMinor: unknown range
            Orientation: unknown range
            Distance: unknown range
            TiltX: unknown range
            TiltY: unknown range
            TrackingId: min=0, max=65535, flat=0, fuzz=0, resolution=0
            Slot: min=0, max=9, flat=0, fuzz=0, resolution=0
          Calibration:
            touch.size.calibration: diameter
            touch.size.scale: 10.000
            touch.size.bias: 0.000
            touch.size.isSummed: false
            touch.pressure.calibration: amplitude
            touch.pressure.scale: 0.005
            touch.orientation.calibration: none
            touch.distance.calibration: none
          SurfaceWidth: 720px
          SurfaceHeight: 1280px
          SurfaceOrientation: 0
          Translation and Scaling Factors:
            XScale: 0.999
            YScale: 0.999
            XPrecision: 1.001
            YPrecision: 1.001
            GeometricScale: 0.999
            PressureScale: 0.005
            SizeScale: 0.033
            OrientationCenter: 0.000
            OrientationScale: 0.000
            DistanceScale: 0.000
            HaveTilt: false
            TiltXCenter: 0.000
            TiltXScale: 0.000
            TiltYCenter: 0.000
            TiltYScale: 0.000
          Last Button State: 0x00000000
          Last Raw Touch: pointerCount=0
          Last Cooked Touch: pointerCount=0

Here is an external keyboard / mouse combo HID device.  (This device doesn't actually
have a mouse but its HID descriptor says it does.)

      Device 7: Motorola Bluetooth Wireless Keyboard
        IsExternal: true
        Sources: 0x00002103
        KeyboardType: 2
        Motion Ranges:
          X: source=0x00002002, min=0.000, max=719.000, flat=0.000, fuzz=0.000
          Y: source=0x00002002, min=0.000, max=1279.000, flat=0.000, fuzz=0.000
          PRESSURE: source=0x00002002, min=0.000, max=1.000, flat=0.000, fuzz=0.000
          VSCROLL: source=0x00002002, min=-1.000, max=1.000, flat=0.000, fuzz=0.000
        Keyboard Input Mapper:
          Parameters:
            AssociatedDisplayId: -1
            OrientationAware: false
          KeyboardType: 2
          Orientation: 0
          KeyDowns: 0 keys currently down
          MetaState: 0x0
          DownTime: 75868832946000
        Cursor Input Mapper:
          Parameters:
            AssociatedDisplayId: 0
            Mode: pointer
            OrientationAware: false
          XScale: 1.000
          YScale: 1.000
          XPrecision: 1.000
          YPrecision: 1.000
          HaveVWheel: true
          HaveHWheel: false
          VWheelScale: 1.000
          HWheelScale: 1.000
          Orientation: 0
          ButtonState: 0x00000000
          Down: false
          DownTime: 0

Here is a joystick.  Notice how all of the axes have been scaled to a normalized
range.  The axis mapping can be configured using key layout files.

    Device 18: Logitech Logitech Cordless RumblePad 2
        IsExternal: true
        Sources: 0x01000511
        KeyboardType: 1
        Motion Ranges:
          X: source=0x01000010, min=-1.000, max=1.000, flat=0.118, fuzz=0.000
          Y: source=0x01000010, min=-1.000, max=1.000, flat=0.118, fuzz=0.000
          Z: source=0x01000010, min=-1.000, max=1.000, flat=0.118, fuzz=0.000
          RZ: source=0x01000010, min=-1.000, max=1.000, flat=0.118, fuzz=0.000
          HAT_X: source=0x01000010, min=-1.000, max=1.000, flat=0.000, fuzz=0.000
          HAT_Y: source=0x01000010, min=-1.000, max=1.000, flat=0.000, fuzz=0.000
        Keyboard Input Mapper:
          Parameters:
            AssociatedDisplayId: -1
            OrientationAware: false
          KeyboardType: 1
          Orientation: 0
          KeyDowns: 0 keys currently down
          MetaState: 0x0
          DownTime: 675270841000
        Joystick Input Mapper:
          Axes:
            X: min=-1.00000, max=1.00000, flat=0.11765, fuzz=0.00000
              scale=0.00784, offset=-1.00000, highScale=0.00784, highOffset=-1.00000
              rawAxis=0, rawMin=0, rawMax=255, rawFlat=15, rawFuzz=0, rawResolution=0
            Y: min=-1.00000, max=1.00000, flat=0.11765, fuzz=0.00000
              scale=0.00784, offset=-1.00000, highScale=0.00784, highOffset=-1.00000
              rawAxis=1, rawMin=0, rawMax=255, rawFlat=15, rawFuzz=0, rawResolution=0
            Z: min=-1.00000, max=1.00000, flat=0.11765, fuzz=0.00000
              scale=0.00784, offset=-1.00000, highScale=0.00784, highOffset=-1.00000
              rawAxis=2, rawMin=0, rawMax=255, rawFlat=15, rawFuzz=0, rawResolution=0
            RZ: min=-1.00000, max=1.00000, flat=0.11765, fuzz=0.00000
              scale=0.00784, offset=-1.00000, highScale=0.00784, highOffset=-1.00000
              rawAxis=5, rawMin=0, rawMax=255, rawFlat=15, rawFuzz=0, rawResolution=0
            HAT_X: min=-1.00000, max=1.00000, flat=0.00000, fuzz=0.00000
              scale=1.00000, offset=0.00000, highScale=1.00000, highOffset=0.00000
              rawAxis=16, rawMin=-1, rawMax=1, rawFlat=0, rawFuzz=0, rawResolution=0
            HAT_Y: min=-1.00000, max=1.00000, flat=0.00000, fuzz=0.00000
              scale=1.00000, offset=0.00000, highScale=1.00000, highOffset=0.00000
              rawAxis=17, rawMin=-1, rawMax=1, rawFlat=0, rawFuzz=0, rawResolution=0

At the end of the input reader dump there is some information about global configuration
parameters such as the mouse pointer speed.

      Configuration:
        ExcludedDeviceNames: []
        VirtualKeyQuietTime: 0.0ms
        PointerVelocityControlParameters: scale=1.000, lowThreshold=500.000, highThreshold=3000.000, acceleration=3.000
        WheelVelocityControlParameters: scale=1.000, lowThreshold=15.000, highThreshold=50.000, acceleration=4.000
        PointerGesture:
          Enabled: true
          QuietInterval: 100.0ms
          DragMinSwitchSpeed: 50.0px/s
          TapInterval: 150.0ms
          TapDragInterval: 300.0ms
          TapSlop: 20.0px
          MultitouchSettleInterval: 100.0ms
          MultitouchMinDistance: 15.0px
          SwipeTransitionAngleCosine: 0.3
          SwipeMaxWidthRatio: 0.2
          MovementSpeedRatio: 0.8
          ZoomSpeedRatio: 0.3

#### Things To Look For ####

1.  All of the expected input devices are present.

2.  Each input device has been configured appropriately.  Especially check the
    touch screen and joystick axes.

### Input Dispatcher State ###

The `InputDispatcher` is responsible for sending input events to applications.
Its state dump shows information about which window is being touched, the
state of the input queue, whether an ANR is in progress, and so on.

    Input Dispatcher State:
      DispatchEnabled: 1
      DispatchFrozen: 0
      FocusedApplication: name='AppWindowToken{41b03a10 token=Token{41bdcf78 ActivityRecord{418ab728 com.android.settings/.Settings}}}', dispatchingTimeout=5000.000ms
      FocusedWindow: name='Window{41908458 Keyguard paused=false}'
      TouchDown: false
      TouchSplit: false
      TouchDeviceId: -1
      TouchSource: 0x00000000
      TouchedWindows: <none>
      Windows:
        0: name='Window{41bd5b18 NavigationBar paused=false}', paused=false, hasFocus=false, hasWallpaper=false, visible=true, canReceiveKeys=false, flags=0x05800068, type=0x000007e3, layer=181000, frame=[0,1184][720,1280], scale=1.000000, touchableRegion=[0,1184][720,1280], inputFeatures=0x00000000, ownerPid=306, ownerUid=1000, dispatchingTimeout=5000.000ms
        1: name='Window{41a19770 RecentsPanel paused=false}', paused=false, hasFocus=false, hasWallpaper=false, visible=false, canReceiveKeys=false, flags=0x01820100, type=0x000007de, layer=151000, frame=[0,0][720,1184], scale=1.000000, touchableRegion=[0,0][720,1184], inputFeatures=0x00000000, ownerPid=306, ownerUid=1000, dispatchingTimeout=5000.000ms
        2: name='Window{41a78768 StatusBar paused=false}', paused=false, hasFocus=false, hasWallpaper=false, visible=true, canReceiveKeys=false, flags=0x00800048, type=0x000007d0, layer=141000, frame=[0,0][720,50], scale=1.000000, touchableRegion=[0,0][720,50], inputFeatures=0x00000000, ownerPid=306, ownerUid=1000, dispatchingTimeout=5000.000ms
        3: name='Window{41877570 StatusBarExpanded paused=false}', paused=false, hasFocus=false, hasWallpaper=false, visible=true, canReceiveKeys=false, flags=0x01811328, type=0x000007e1, layer=131005, frame=[0,-1184][720,-114], scale=1.000000, touchableRegion=[0,-1184][720,-114], inputFeatures=0x00000000, ownerPid=306, ownerUid=1000, dispatchingTimeout=5000.000ms
        4: name='Window{41bedf20 TrackingView paused=false}', paused=false, hasFocus=false, hasWallpaper=false, visible=false, canReceiveKeys=false, flags=0x01020300, type=0x000007e1, layer=131000, frame=[0,-1032][720,102], scale=1.000000, touchableRegion=[0,-1032][720,102], inputFeatures=0x00000000, ownerPid=306, ownerUid=1000, dispatchingTimeout=5000.000ms
        5: name='Window{41908458 Keyguard paused=false}', paused=false, hasFocus=true, hasWallpaper=false, visible=true, canReceiveKeys=true, flags=0x15120800, type=0x000007d4, layer=111000, frame=[0,50][720,1184], scale=1.000000, touchableRegion=[0,50][720,1184], inputFeatures=0x00000000, ownerPid=205, ownerUid=1000, dispatchingTimeout=5000.000ms
        6: name='Window{4192cc30 com.android.phasebeam.PhaseBeamWallpaper paused=false}', paused=false, hasFocus=false, hasWallpaper=false, visible=true, canReceiveKeys=false, flags=0x00000308, type=0x000007dd, layer=21010, frame=[0,0][720,1184], scale=1.000000, touchableRegion=[0,0][720,1184], inputFeatures=0x00000000, ownerPid=429, ownerUid=10046, dispatchingTimeout=5000.000ms
        7: name='Window{41866c00 com.android.settings/com.android.settings.Settings paused=false}', paused=false, hasFocus=false, hasWallpaper=false, visible=false, canReceiveKeys=false, flags=0x01810100, type=0x00000001, layer=21005, frame=[0,0][720,1184], scale=1.000000, touchableRegion=[0,0][720,1184], inputFeatures=0x00000000, ownerPid=19000, ownerUid=1000, dispatchingTimeout=5000.000ms
        8: name='Window{4197c858 com.android.launcher/com.android.launcher2.Launcher paused=false}', paused=false, hasFocus=false, hasWallpaper=false, visible=false, canReceiveKeys=false, flags=0x01910100, type=0x00000001, layer=21000, frame=[0,0][720,1184], scale=1.000000, touchableRegion=[0,0][720,1184], inputFeatures=0x00000000, ownerPid=515, ownerUid=10032, dispatchingTimeout=5000.000ms
      MonitoringChannels: <none>
      InboundQueue: length=0
      ActiveConnections: <none>
      AppSwitch: not pending
      Configuration:
        MaxEventsPerSecond: 90
        KeyRepeatDelay: 50.0ms
        KeyRepeatTimeout: 500.0ms

#### Things To Look For ####

1.  In general, all input events are being processed as expected.

2.  If you touch the touch screen and run dumpsys at the same time, then the `TouchedWindows`
    line should show the window that you are touching.

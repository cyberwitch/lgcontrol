package ch.cyberwit.lgcontrol;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class KeyBindings implements IXposedHookZygoteInit, IXposedHookLoadPackage
{
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable
	{

	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		// For some reason, findAndHookMethod doesn't work for this
		XposedBridge.hookAllMethods(XposedHelpers.findClass("com.android.internal.policy.impl.PhoneWindowManager", lpparam.classLoader), "interceptKeyBeforeDispatching", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                KeyEvent event = (KeyEvent) param.args[1];
                int action = event.getAction();
                int keyCode = event.getKeyCode();
                int repeatCount = event.getRepeatCount();

                if (event.getDeviceId() != KeyCharacterMap.VIRTUAL_KEYBOARD) {
                    if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                        if (action == KeyEvent.ACTION_DOWN) {
                            if (repeatCount > 0) {
                                isLongPress = true;
                                lgClient.sendCommand(keyCode == KeyEvent.KEYCODE_MENU ? 2 : 3);
                            } else {
                                isLongPress = false;
                            }
                        } else if (action == KeyEvent.ACTION_UP) {
                            if (!isLongPress) {
                                injectKey(keyCode);
                            }
                        }
                        param.setResult(-1);
                    }
                }
            }
        });
	}

    protected void injectKey(int keycode) {
        InputManager inputManager = (InputManager) XposedHelpers
                .callStaticMethod(InputManager.class, "getInstance");
        long now = SystemClock.uptimeMillis();
        final KeyEvent downEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                keycode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD,
                0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD);
        final KeyEvent upEvent = KeyEvent.changeAction(downEvent,
                KeyEvent.ACTION_UP);

        Integer INJECT_INPUT_EVENT_MODE_ASYNC = XposedHelpers
                .getStaticIntField(InputManager.class,
                        "INJECT_INPUT_EVENT_MODE_ASYNC");

        XposedHelpers.callMethod(inputManager, "injectInputEvent", downEvent,
                INJECT_INPUT_EVENT_MODE_ASYNC);
        XposedHelpers.callMethod(inputManager, "injectInputEvent", upEvent,
                INJECT_INPUT_EVENT_MODE_ASYNC);

    }

    private boolean isLongPress = false;

    private LGClient lgClient = new LGClient("192.168.1.119", new XSharedPreferences("ch.cyberwit.lgcontrol", "user_settings").getString("pair_code", null));

    private static final String LOG = KeyBindings.class.getName();
}
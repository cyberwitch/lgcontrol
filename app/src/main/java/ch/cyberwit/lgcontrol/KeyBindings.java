package ch.cyberwit.lgcontrol;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.util.Date;

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
		XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.internal.policy.impl.PhoneWindowManager",
                        lpparam.classLoader), "interceptKeyBeforeDispatching", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                KeyEvent event = (KeyEvent) param.args[1];
                int action = event.getAction();
                int keyCode = event.getKeyCode();

                if (event.getDeviceId() != KeyCharacterMap.VIRTUAL_KEYBOARD) {
                    if (keyCode == KeyEvent.KEYCODE_MENU) {
                        if (action == KeyEvent.ACTION_DOWN) {
                            menuKeyPressed = true;
                        } else if (menuKeyPressed && action == KeyEvent.ACTION_UP) {
                            if (!secondKeyPressed) injectKey(keyCode);
                            menuKeyPressed = false;
                            secondKeyPressed = false;
                            lastKeyPressed = 0;
                            timeLastKeyPressed = 0;
                        }
                        param.setResult(-1);
                    } else if (menuKeyPressed && (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                            keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                            keyCode == KeyEvent.KEYCODE_DPAD_CENTER) &&
                            action == KeyEvent.ACTION_DOWN) {
                        long now = new Date().getTime();
                        if (keyCode != lastKeyPressed || timeLastKeyPressed + 100 <= now) {
                            lastKeyPressed = keyCode;
                            timeLastKeyPressed = now;
                            secondKeyPressed = true;

                            if (preferences.hasFileChanged()) {
                                preferences.reload();
                                lgClient.setPairCode(preferences.getString("pair_code", null));
                            }

                            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                lgClient.volumeUp();
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
                                lgClient.volumeDown();
                            } else {
                                lgClient.toggleTV();
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

    private boolean menuKeyPressed = false;
    private boolean secondKeyPressed = false;

    private int lastKeyPressed = 0;
    private long timeLastKeyPressed = 0;

    private XSharedPreferences preferences =
            new XSharedPreferences(KeyBindings.class.getPackage().getName(), "user_settings");
    private LGClient lgClient =
            new LGClient("192.168.1.119", preferences.getString("pair_code", null));
}
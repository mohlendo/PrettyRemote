package com.mohleno.prettyremote.services;

/**
 * Created by moh on 27.07.14.
 */
public enum LGKey {
    MUTE(26), CHANNEL_UP(27), CHANNEL_DOWN(28), VOLUME_UP(24), VOLUME_DOWN(25), OK(20);

    private final int code;

    public int code() {
        return code;
    }

    LGKey(int code) {
        this.code = code;
    }
}

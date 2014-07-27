package com.mohleno.prettyremote.services;

/**
 * Created by moh on 27.07.14.
 */
public enum LGCommand {
    MUTE(26), CHANNEL_UP(27), CHANNEL_DOWN(28);

    private final int code;

    public int code() {
        return code;
    }

    LGCommand(int code) {
        this.code = code;
    }
}

package de.grnx.fftlogger;

/** enum for permission request codes because someone on SO said so: https://stackoverflow.com/a/43392256/22134545 */
enum RequestEnum {
    AudioPermission(69);
    final int code;

private RequestEnum(int reqCode) {
    code = reqCode;
}
}

public final class RequestCodes {
    public static final int AUDIO = 69;
}

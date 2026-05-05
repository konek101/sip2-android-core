package org.telon.sip2;

public class PjActions {
    public static final String ACTION_START = "start";
    public static final String ACTION_CREATE_ACCOUNT = "account_create";
    public static final String ACTION_CHANGE_CODEC_SETTINGS = "change_codec_settings'";
    public static final String ACTION_REGISTER_ACCOUNT = "account_register";
    public static final String ACTION_DELETE_ACCOUNT = "account_delete";
    public static final String ACTION_MAKE_CALL = "call_make";
    public static final String ACTION_HANGUP_CALL = "call_hangup";
    public static final String ACTION_DECLINE_CALL = "call_decline";
    public static final String ACTION_ANSWER_CALL = "call_answer";
    public static final String ACTION_HOLD_CALL = "call_hold";
    public static final String ACTION_UNHOLD_CALL = "call_unhold";
    public static final String ACTION_MUTE_CALL = "call_mute";
    public static final String ACTION_UNMUTE_CALL = "call_unmute";
    public static final String ACTION_USE_SPEAKER_CALL = "call_use_speaker";
    public static final String ACTION_USE_EARPIECE_CALL = "call_use_earpiece";
    public static final String ACTION_XFER_CALL = "call_xfer";
    public static final String ACTION_XFER_REPLACES_CALL = "call_xfer_replace";
    public static final String ACTION_REDIRECT_CALL = "call_redirect";
    public static final String ACTION_DTMF_CALL = "call_dtmf";
    public static final String ACTION_SET_SERVICE_CONFIGURATION = "set_service_configuration";
    public static final String ACTION_ACTIVATEAUDIOSESSION_CALL = "call_activateaudiosession";
    public static final String ACTION_RINGING_CALL = "call_ringing";
    public static final String ACTION_PROGRESS_CALL = "call_progress";

    public static final String EVENT_STARTED = "one.telefon.account.started";
    public static final String EVENT_ACCOUNT_CREATED = "one.telefon.account.created";
    public static final String EVENT_REGISTRATION_CHANGED = "one.telefon.registration.changed";
    public static final String EVENT_CALL_CHANGED = "one.telefon.call.changed";
    public static final String EVENT_CALL_TERMINATED = "one.telefon.call.terminated";
    public static final String EVENT_CALL_RECEIVED = "one.telefon.call.received";
    public static final String EVENT_CALL_SCREEN_LOCKED = "one.telefon.call.screen.locked";
    public static final String EVENT_MESSAGE_RECEIVED = "one.telefon.message.received";
    public static final String EVENT_HANDLED = "one.telefon.handled";
}

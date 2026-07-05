package com.example.communication

enum class CommunicationState {
    Idle,
    Listening,
    SpeechDetected,
    SpeechEnded,
    Processing,
    SendingRequest,
    WaitingForResponse,
    StreamingResponse,
    Speaking,
    WaitingForNextTurn,
    Paused,
    Recovering,
    PermissionRequired,
    Completed,
    Cancelled,
    Error
}

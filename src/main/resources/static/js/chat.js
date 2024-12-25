let socket;
let sessionChannelId = "";
let nickname = "";

function connectToWebSocket(channelId, userNickname) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        console.log("Closing previous WebSocket connection.");
        socket.close();
    }

    sessionChannelId = channelId;
    nickname = userNickname || "Anonymous";
    const wsUrl = "/ws";

    if (socket && (socket.readyState === WebSocket.CLOSING || socket.readyState === WebSocket.CLOSED)) {
        socket = new WebSocket(`ws://${window.location.host}${wsUrl}`);
    }

    socket = new WebSocket(`ws://${window.location.host}${wsUrl}`);

    socket.onopen = () => {
        console.log("Connected to WebSocket");
    };

    socket.onmessage = (event) => {
        try {
            const message = JSON.parse(event.data);
            console.log("Received message:", message);
        } catch (error) {
            console.error("Error parsing WebSocket message:", error);
        }
    };

    socket.onerror = (error) => {
        console.error("WebSocket error:", error);
    };

    socket.onclose = (event) => {
        console.log("Disconnected from WebSocket. Reason:", event.reason);
    };
}

function sendMessage() {
    if (socket.readyState !== WebSocket.OPEN) {
        console.error("WebSocket is not open. Cannot send message.");
        return;
    }

    const messageInput = document.getElementById("message");
    const messageText = messageInput.value.trim();
    if (!messageText) return;

    const message = {
        channelId: sessionChannelId,
        nickname: nickname,
        message: messageText
    };

    socket.send(JSON.stringify(message));
    messageInput.value = "";
}

function sendUserJoinedEvent() {
    if (socket.readyState === WebSocket.OPEN) {
        const joinEvent = {
            type: "USER_JOINED",
            channelId: sessionChannelId,
            nickname: nickname
        };
        socket.send(JSON.stringify(joinEvent));
    }
}

function typing() {

}

function leaveChat() {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close();
    }
    window.location.href = "/";
}

window.onload = () => {
    const urlParams = new URLSearchParams(window.location.search);
    const channelId = urlParams.get("channelId");
    const userNickname = urlParams.get("nickname");
    connectToWebSocket(channelId, userNickname);
};

window.onbeforeunload = () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close();
    }
};

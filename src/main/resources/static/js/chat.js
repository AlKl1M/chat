let socket;
let channelId = "";
let nickname = "";

function connectToWebSocket(channel, userNickname) {
    channelId = channel;
    nickname = userNickname || "Anonymous";
    const wsUrl = "/ws";

    socket = new WebSocket(`ws://${window.location.host}${wsUrl}`);

    socket.onopen = () => {
        console.log("Connected to WebSocket");
        sendUserJoinedEvent();
    };

    socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        displayMessage(message);
    };

    socket.onerror = (error) => {
        console.error("WebSocket error:", error);
    };

    socket.onclose = () => {
        console.log("Disconnected from WebSocket");
    };
}

function displayMessage(event) {
    const messagesList = document.getElementById("messages");
    const messageElement = document.createElement("li");

    const { nickname, message, type } = event;

    if (type === "CHAT_MESSAGE") {
        messageElement.textContent = `${nickname || "Anonymous"}: ${message}`;
    } else if (type === "USER_JOINED") {
        messageElement.textContent = `${nickname || "Anonymous"} joined the chat.`;
    } else if (type === "USER_LEFT") {
        messageElement.textContent = `${nickname || "Anonymous"} left the chat.`;
    } else if (type === "USER_STATS") {
        messageElement.textContent = `Stats update: ${message}`;
    }

    messagesList.appendChild(messageElement);
}

function sendMessage() {
    if (socket.readyState !== WebSocket.OPEN) {
        console.error("WebSocket is not open. Cannot send message.");
        return;
    }

    const messageInput = document.getElementById("message");
    const messageText = messageInput.value.trim();
    if (!messageText) return;

    const event = {
        id: generateId(),
        channelId,
        type: "CHAT_MESSAGE",
        message: messageText,
        nickname,
    };

    socket.send(JSON.stringify(event));
    messageInput.value = "";
}

function sendUserJoinedEvent() {
    const event = {
        id: generateId(),
        channelId,
        type: "USER_JOINED",
        nickname,
    };
    socket.send(JSON.stringify(event));
}

function typing() {
    const typingIndicator = document.getElementById("typing-indicator");
    typingIndicator.textContent = `${nickname || "Someone"} is typing...`;
    setTimeout(() => (typingIndicator.textContent = ""), 1000);
}

function leaveChat() {
    const event = {
        id: generateId(),
        channelId,
        type: "USER_LEFT",
        nickname,
    };
    socket.send(JSON.stringify(event));
    socket.close();
    window.location.href = "/";
}

function generateId() {
    return Math.random().toString(36).substr(2, 9);
}

window.onload = () => {
    const urlParams = new URLSearchParams(window.location.search);
    const channel = urlParams.get("channelId");
    const userNickname = urlParams.get("nickname");
    connectToWebSocket(channel, userNickname);
};

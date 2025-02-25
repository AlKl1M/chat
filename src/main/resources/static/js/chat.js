let socket;
let channelId = "";
let nickname = "";

function connectToWebSocket(channel, userNickname) {
    channelId = channel;
    nickname = userNickname || "Anonymous";
    const wsUrl = `/ws?channelId=${channelId}`;
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
    const {nickname, message, type, filename} = event;

    const messagesList = document.getElementById("messages");
    const messageElement = document.createElement("li");

    if (type === "CHAT_MESSAGE") {
        messageElement.innerHTML = `
            <span>${nickname || "Anonymous"}</span>: 
            <span>${message}</span>
        `;
    } else if (type === "USER_JOINED") {
        messageElement.innerHTML = `
            <span>${nickname || "Anonymous"}</span> joined the chat.
        `;
    } else if (type === "USER_LEFT") {
        messageElement.innerHTML = `
            <span>${nickname || "Anonymous"}</span> left the chat.
        `;
    } else if (type === "FILE_MESSAGE") {
        messageElement.innerHTML = `
            <span>${nickname || "Anonymous"}</span> sent a file: 
            <a href="${message}" target="_blank">${filename || "Unnamed file"}</a>
        `;
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
    if (messageText) {
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

    const fileInput = document.getElementById("file-input");
    const file = fileInput.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function (event) {
            const base64Data = event.target.result.split(',')[1];

            const fileEvent = {
                id: generateId(),
                channelId,
                type: "FILE_MESSAGE",
                nickname,
                filename: file.name,
                fileData: base64Data
            };
            socket.send(JSON.stringify(fileEvent));
        };
        reader.readAsDataURL(file);
    }
}

function sendUserJoinedEvent() {
    const event = {
        id: generateId(),
        channelId,
        type: "USER_JOINED",
        nickname,
        message: `joined the chat.`,
    };
    socket.send(JSON.stringify(event));
}

function leaveChat() {
    const event = {
        id: generateId(),
        channelId,
        type: "USER_LEFT",
        nickname,
        message: `left the chat.`,
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
let typingTimeout;
const nickname = new URLSearchParams(window.location.search).get('nickname');
const channelId = new URLSearchParams(window.location.search).get('channelId');
let socket = new SockJS('/ws');
let stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);

    stompClient.subscribe(`/topic/messages/${channelId}`, function (response) {
        let message = JSON.parse(response.body);
        appendMessage(message.sender, message.content, message.fileUrl);
    });

    stompClient.subscribe(`/topic/typing/${channelId}`, function (response) {
        let typingData = JSON.parse(response.body);
        displayTyping(typingData);
    });

    fetch(`/app/chat/channels/${channelId}/messages`)
        .then(response => response.json())
        .then(messages => {
            messages.forEach(message => appendMessage(message.sender, message.content, message.fileUrl));
        })
        .catch(error => console.error('Error fetching messages:', error));
});

function sendMessage() {
    let content = document.getElementById('message').value;
    let fileInput = document.getElementById('file-input');
    let file = fileInput.files[0];

    if (!content.trim() && !file) {
        alert("Cannot send empty message or file.");
        return;
    }

    if (file) {
        let reader = new FileReader();
        reader.onload = function (event) {
            let fileUploadDto = {
                fileContentBase64: event.target.result.split(',')[1],
                filename: file.name,
                contentType: file.type
            };

            stompClient.send(`/app/chat/channels/${channelId}/files`, {}, JSON.stringify(fileUploadDto));
        };
        reader.readAsDataURL(file);
    } else {
        let message = {
            sender: nickname,
            channelId: channelId,
            content: content
        };

        stompClient.send(`/app/chat/channels/${channelId}/messages`, {}, JSON.stringify(message));
        document.getElementById('message').value = '';
    }
}

function appendMessage(sender, content, fileUrl) {
    let messages = document.getElementById('messages');
    let newMessage = document.createElement('li');
    if (fileUrl) {
        let link = document.createElement('a');
        link.href = fileUrl;
        link.textContent = `${sender}: File (${fileUrl.split('/').pop()})`;
        link.target = "_blank";
        newMessage.appendChild(link);
    } else {
        newMessage.textContent = `${sender}: ${content}`;
    }
    messages.appendChild(newMessage);
}

function typing() {
    let content = document.getElementById('message').value;
    if (content.trim() !== '') {
        let typingData = {
            sender: nickname,
            channelId: channelId
        };
        stompClient.send(`/app/chat/channels/${channelId}/typing`, {}, JSON.stringify(typingData));
        clearTimeout(typingTimeout);
    } else {
        clearTypingIndicator();
    }

    typingTimeout = setTimeout(() => {
        clearTypingIndicator();
    }, 3000);
}

function displayTyping(typingData) {
    let typingIndicator = document.getElementById('typing-indicator');
    typingIndicator.textContent = `${typingData.sender} is typing...`;
}

function clearTypingIndicator() {
    document.getElementById('typing-indicator').textContent = '';
}

function leaveChat() {
    stompClient.disconnect(() => {
        window.location.href = '/';
    });
}

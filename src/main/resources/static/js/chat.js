const nickname = new URLSearchParams(window.location.search).get('nickname');
const channelId = new URLSearchParams(window.location.search).get('channelId');
let socket = new SockJS('/ws');
let stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    stompClient.subscribe(`/topic/messages/${channelId}`, function (response) {
        let message = JSON.parse(response.body);
        appendMessage(message.sender, message.content);
    });

    fetch(`/api/chat/channels/${channelId}/messages`)
        .then(response => response.json())
        .then(messages => {
            messages.forEach(message => appendMessage(message.sender, message.content));
        })
        .catch(error => console.error('Error fetching messages:', error));
});

function sendMessage() {
    let content = document.getElementById('message').value;
    if (content.trim() !== '') {
        let message = {
            sender: nickname,
            channelId: channelId,
            content: content
        };

        stompClient.send(`/app/chat/channels/${channelId}/messages`, {}, JSON.stringify(message));

        document.getElementById('message').value = '';
    }
}

function appendMessage(sender, content) {
    let messages = document.getElementById('messages');
    let newMessage = document.createElement('li');
    newMessage.textContent = sender + ": " + content;
    messages.appendChild(newMessage);
}

function leaveChat() {
    stompClient.disconnect(() => {
        window.location.href = '/';
    });
}
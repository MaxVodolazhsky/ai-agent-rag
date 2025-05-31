package org.ai.agent.handler;

import org.ai.agent.agent.TierAssistant;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ChatHandler extends TextWebSocketHandler {
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final TierAssistant assistant;

    public ChatHandler(TierAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userMessage = message.getPayload();
        session.sendMessage(new TextMessage("Вы: " + userMessage));

        session.sendMessage(new TextMessage("Ai-Agent: " + assistant.answerQuestion(userMessage)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void sendServerMessageToAllClients(String content) throws Exception {
        TextMessage message = new TextMessage("Бот: " + content);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        }
    }
}

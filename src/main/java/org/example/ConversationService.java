package org.example;

import org.example.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConversationService {
    private final List<Message> history = new ArrayList<>();

    public void load() throws IOException {
        history.addAll(ConversationRepo.load());
    }

    public void add(Message m) throws IOException {
        history.add(m);
        ConversationRepo.append(m);
    }

    public List<Message> getHistory() {
        return new ArrayList<>(history);
    }

    public void clear() throws IOException {
        history.clear();
        ConversationRepo.clear();
    }
}
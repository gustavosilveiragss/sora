package com.sora.backend.service;

import com.sora.backend.dto.MessageDto;
import com.sora.backend.model.Message;
import com.sora.backend.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    
    private final MessageRepository messageRepository;
    
    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }
    
    public Message createMessage(MessageDto messageDto) {
        var message = new Message(messageDto.content());
        return messageRepository.save(message);
    }
    
    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
    
    public MessageDto getHelloWorldMessage() {
        return new MessageDto("Hello World from Sora Backend!");
    }
}
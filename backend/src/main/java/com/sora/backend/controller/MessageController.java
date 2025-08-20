package com.sora.backend.controller;

import com.sora.backend.dto.MessageDto;
import com.sora.backend.model.Message;
import com.sora.backend.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {
    
    private final MessageService messageService;
    
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }
    
    @GetMapping("/hello")
    public ResponseEntity<MessageDto> getHelloWorld() {
        var message = messageService.getHelloWorldMessage();
        return ResponseEntity.ok(message);
    }
    
    @PostMapping
    public ResponseEntity<Message> createMessage(@Valid @RequestBody MessageDto messageDto) {
        var createdMessage = messageService.createMessage(messageDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMessage);
    }
    
    @GetMapping
    public ResponseEntity<List<Message>> getAllMessages() {
        var messages = messageService.getAllMessages();
        return ResponseEntity.ok(messages);
    }
}
package com.sora.backend.controller;

import com.sora.backend.dto.CollectionDto;
import com.sora.backend.model.Collection;
import com.sora.backend.repository.CollectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/collections")
@Tag(name = "Collections", description = "Photo collection categories management")
public class CollectionController {

    private final CollectionRepository collectionRepository;

    public CollectionController(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    @GetMapping
    @Operation(summary = "Get all collections", description = "Get list of all available photo collections")
    @ApiResponse(responseCode = "200", description = "Collections retrieved successfully")
    public ResponseEntity<List<CollectionDto>> getAllCollections() {
        List<Collection> collections = collectionRepository.findAllByOrderBySortOrderAsc();
        List<CollectionDto> collectionDtos = collections.stream().map(this::mapToCollectionDto).toList();
        
        return ResponseEntity.ok(collectionDtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get collection by ID", description = "Get specific collection details")
    @ApiResponse(responseCode = "200", description = "Collection retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    public ResponseEntity<CollectionDto> getCollectionById(@Parameter(description = "Collection ID") @PathVariable Long id) {
        Optional<Collection> collectionOpt = collectionRepository.findById(id);
        if (collectionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Collection collection = collectionOpt.get();
        CollectionDto collectionDto = mapToCollectionDto(collection);
        
        return ResponseEntity.ok(collectionDto);
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Get collection by code", description = "Get collection details by code")
    @ApiResponse(responseCode = "200", description = "Collection retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    public ResponseEntity<CollectionDto> getCollectionByCode(@Parameter(description = "Collection code") @PathVariable String code) {
        Optional<Collection> collectionOpt = collectionRepository.findByCode(code.toUpperCase());
        if (collectionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Collection collection = collectionOpt.get();
        CollectionDto collectionDto = mapToCollectionDto(collection);
        
        return ResponseEntity.ok(collectionDto);
    }

    @GetMapping("/default")
    @Operation(summary = "Get default collection", description = "Get the default collection for new posts")
    @ApiResponse(responseCode = "200", description = "Default collection retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Default collection not found")
    public ResponseEntity<CollectionDto> getDefaultCollection() {
        Optional<Collection> defaultCollectionOpt = collectionRepository.findByIsDefaultTrue();
        if (defaultCollectionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Collection defaultCollection = defaultCollectionOpt.get();
        CollectionDto collectionDto = mapToCollectionDto(defaultCollection);
        
        return ResponseEntity.ok(collectionDto);
    }

    private CollectionDto mapToCollectionDto(Collection collection) {
        return new CollectionDto(
                collection.getId(),
                collection.getCode(),
                collection.getNameKey(),
                collection.getIconName(),
                collection.getSortOrder(),
                collection.getIsDefault()
        );
    }
}
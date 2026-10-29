package dev.stayledger.web;

import dev.stayledger.service.CurrentUserService;
import dev.stayledger.service.PropertyService;
import dev.stayledger.web.Dtos.PropertyRequest;
import dev.stayledger.web.Dtos.PropertyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService properties;
    private final CurrentUserService currentUser;

    public PropertyController(PropertyService properties, CurrentUserService currentUser) {
        this.properties = properties;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<PropertyResponse> list() {
        return properties.list(currentUser.require());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PropertyResponse create(@Valid @RequestBody PropertyRequest request) {
        return properties.create(currentUser.require(), request);
    }

    @GetMapping("/{id}")
    PropertyResponse get(@PathVariable UUID id) {
        return properties.get(currentUser.require(), id);
    }

    @PutMapping("/{id}")
    PropertyResponse update(@PathVariable UUID id, @Valid @RequestBody PropertyRequest request) {
        return properties.update(currentUser.require(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        properties.delete(currentUser.require(), id);
    }
}

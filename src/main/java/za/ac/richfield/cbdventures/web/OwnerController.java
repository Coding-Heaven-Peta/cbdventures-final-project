package za.ac.richfield.cbdventures.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.security.CurrentUser;
import za.ac.richfield.cbdventures.service.BusinessService;
import za.ac.richfield.cbdventures.service.EventService;
import za.ac.richfield.cbdventures.web.dto.BusinessCard;
import za.ac.richfield.cbdventures.web.dto.BusinessDetail;
import za.ac.richfield.cbdventures.web.dto.BusinessRequest;
import za.ac.richfield.cbdventures.web.dto.EventDtos;

import java.util.List;

/**
 * Business owner portal (FR-03): create, edit and remove your own listing and
 * the events hosted at it.
 */
@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    private final BusinessService businessService;
    private final EventService eventService;

    public OwnerController(BusinessService businessService, EventService eventService) {
        this.businessService = businessService;
        this.eventService = eventService;
    }

    @GetMapping("/businesses")
    public List<BusinessCard> myBusinesses() {
        return businessService.myListings(CurrentUser.require());
    }

    @PostMapping("/businesses")
    public ResponseEntity<BusinessDetail> create(@Valid @RequestBody BusinessRequest request) {
        User owner = CurrentUser.require();
        return ResponseEntity.status(HttpStatus.CREATED).body(businessService.create(owner, request));
    }

    @PutMapping("/businesses/{id}")
    public BusinessDetail update(@PathVariable Long id, @Valid @RequestBody BusinessRequest request) {
        return businessService.update(CurrentUser.require(), id, request);
    }

    @DeleteMapping("/businesses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        businessService.delete(CurrentUser.require(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/businesses/{id}/events")
    public List<EventDtos.EventView> events(@PathVariable Long id) {
        return eventService.forBusiness(id);
    }

    @PostMapping("/businesses/{id}/events")
    public ResponseEntity<EventDtos.EventView> createEvent(@PathVariable Long id,
                                                           @Valid @RequestBody EventDtos.EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(CurrentUser.require(), id, request));
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.delete(CurrentUser.require(), eventId);
        return ResponseEntity.noContent().build();
    }
}

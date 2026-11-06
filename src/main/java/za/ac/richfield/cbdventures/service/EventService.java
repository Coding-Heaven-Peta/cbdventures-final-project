package za.ac.richfield.cbdventures.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.richfield.cbdventures.domain.Business;
import za.ac.richfield.cbdventures.domain.EventListing;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.Role;
import za.ac.richfield.cbdventures.error.ApiException;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.EventRepository;
import za.ac.richfield.cbdventures.util.GeoUtils;
import za.ac.richfield.cbdventures.web.dto.EventDtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event rules from Phase 3: events hang off a physical venue on the platform,
 * must start in the future, and disappear with the venue.
 */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final BusinessRepository businessRepository;
    private final BusinessMapper mapper;
    private final ZoneService zoneService;

    public EventService(EventRepository eventRepository,
                        BusinessRepository businessRepository,
                        BusinessMapper mapper,
                        ZoneService zoneService) {
        this.eventRepository = eventRepository;
        this.businessRepository = businessRepository;
        this.mapper = mapper;
        this.zoneService = zoneService;
    }

    @Transactional(readOnly = true)
    public List<EventDtos.EventView> upcoming(String categorySlug, Double maxDistanceKm, Integer limit) {
        List<EventListing> events = eventRepository.findUpcomingForStatus(BusinessStatus.ACTIVE,
                LocalDateTime.now());
        double radius = maxDistanceKm != null ? maxDistanceKm : zoneService.activeRadiusKm();
        int max = limit == null ? 40 : Math.min(Math.max(limit, 1), 100);

        return events.stream()
                .filter(event -> categorySlug == null || categorySlug.isBlank()
                        || event.getBusiness().getCategory().getSlug().equalsIgnoreCase(categorySlug))
                .filter(event -> zoneService.distanceFromAnchor(
                        event.getBusiness().getLatitude().doubleValue(),
                        event.getBusiness().getLongitude().doubleValue()) <= radius)
                .limit(max)
                .map(event -> mapper.toEventView(event, event.getBusiness(), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventDtos.EventView> forBusiness(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
        return eventRepository.findByBusinessIdOrderByEventDateTimeAsc(businessId).stream()
                .map(event -> mapper.toEventView(event, business, null))
                .toList();
    }

    @Transactional
    public EventDtos.EventView create(User actor, Long businessId, EventDtos.EventRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("That business could not be found."));
        if (actor.getRole() != Role.ADMIN && !business.getOwner().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only add events to your own listings.");
        }
        if (!request.eventDateTime().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("The event must be scheduled for a future date and time.");
        }
        if (request.ticketPrice() == null || request.ticketPrice().signum() < 0) {
            throw ApiException.badRequest("The ticket price cannot be negative. Use 0 for a free event.");
        }

        EventListing event = new EventListing();
        event.setBusiness(business);
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDateTime(request.eventDateTime());
        event.setTicketPrice(request.ticketPrice());
        EventListing saved = eventRepository.save(event);
        return mapper.toEventView(saved, business, actor);
    }

    @Transactional
    public void delete(User actor, Long eventId) {
        EventListing event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("That event could not be found."));
        if (actor.getRole() != Role.ADMIN && !event.getBusiness().getOwner().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only remove events from your own listings.");
        }
        eventRepository.delete(event);
    }

    /** Distance from the campus anchor, used by the events screen. */
    @Transactional(readOnly = true)
    public double distanceKm(Business business) {
        return GeoUtils.round2(zoneService.distanceFromAnchor(
                business.getLatitude().doubleValue(), business.getLongitude().doubleValue()));
    }
}

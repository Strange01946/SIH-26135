package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.venue.VenueBookingDto.*;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.impl.VenueBookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/venue-bookings")
public class VenueBookingController {
  private static final String READ = "hasAnyAuthority('venue.booking.request','venue.booking.review','venue.shared.manage')";
  private static final String REQUEST = "hasAuthority('venue.booking.request')";
  private static final String REVIEW = "hasAuthority('venue.booking.review')";
  private static final String MANAGE = "hasAuthority('venue.shared.manage')";

  private final VenueBookingService venues;

  public VenueBookingController(VenueBookingService venues) {
    this.venues = venues;
  }

  @GetMapping("/venues")
  @PreAuthorize(READ)
  public ApiResponse<List<SharedVenue>> sharedVenues(@RequestParam(required = false) @Positive Long stateId,
      @RequestParam(required = false) @Positive Long districtId, @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "25") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(venues.sharedVenues(stateId, districtId, page, size));
  }

  @GetMapping("/venues/{centerId}/availability")
  @PreAuthorize(READ)
  public ApiResponse<List<BusySlot>> availability(@PathVariable @Positive long centerId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.ok(venues.availability(centerId, from, to));
  }

  @PostMapping("/venues")
  @PreAuthorize(MANAGE)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<SharedVenue> createSharedVenue(@Valid @RequestBody NewSharedVenue request) {
    return ApiResponse.ok(venues.createSharedVenue(request));
  }

  @PostMapping("/venues/{centerId}/blackouts")
  @PreAuthorize(MANAGE)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<BusySlot> addBlackout(@PathVariable @Positive long centerId, @Valid @RequestBody Blackout request) {
    return ApiResponse.ok(venues.addBlackout(centerId, request));
  }

  @DeleteMapping("/venues/{centerId}/blackouts/{blackoutId}")
  @PreAuthorize(MANAGE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void releaseBlackout(@PathVariable @Positive long centerId, @PathVariable @Positive long blackoutId) {
    venues.releaseBlackout(centerId, blackoutId);
  }

  @GetMapping
  @PreAuthorize(READ)
  public ApiResponse<List<Booking>> bookings(
      @RequestParam(required = false) @Pattern(regexp = "PENDING|APPROVED|REJECTED|CANCELLED|COMPLETED|SUPERSEDED") String status,
      @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "25") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(venues.bookings(status, page, size));
  }

  @GetMapping("/{id}")
  @PreAuthorize(READ)
  public ApiResponse<Details> details(@PathVariable @Positive long id) {
    return ApiResponse.ok(venues.details(id));
  }

  @PostMapping
  @PreAuthorize(REQUEST)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Details> request(@Valid @RequestBody NewBooking request) {
    return ApiResponse.ok(venues.request(request));
  }

  @PostMapping("/{id}/reschedule")
  @PreAuthorize(REQUEST)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Details> requestReschedule(@PathVariable @Positive long id, @Valid @RequestBody Reschedule request) {
    return ApiResponse.ok(venues.requestReschedule(id, request));
  }

  @PostMapping("/{id}/review")
  @PreAuthorize(REVIEW)
  public ApiResponse<Details> review(@PathVariable @Positive long id, @Valid @RequestBody Review request) {
    return ApiResponse.ok(venues.review(id, request));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize(REQUEST)
  public ApiResponse<Details> close(@PathVariable @Positive long id, @Valid @RequestBody Close request) {
    return ApiResponse.ok(venues.close(id, request));
  }
}

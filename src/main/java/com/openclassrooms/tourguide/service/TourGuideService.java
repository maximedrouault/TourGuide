package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.NearAttractionDTO;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tripPricer.Provider;
import tripPricer.TripPricer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Service

public class TourGuideService {
	private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	// Multi-threading executor for trackUserLocation
	private final ExecutorService executorService = Executors.newFixedThreadPool(10000);


	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) throws ExecutionException, InterruptedException {
        return (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation()
				: trackUserLocation(user).get();
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return new ArrayList<>(internalUserMap.values());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		return CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()), executorService)
				.thenCompose(visitedLocation -> {
					user.addToVisitedLocations(visitedLocation);
					CompletableFuture<Void> futureReward = rewardsService.calculateRewards(user);

					return futureReward.thenApply(aVoid -> visitedLocation);
				})
				.exceptionally(exception -> {
					logger.error("Error tracking user location for user: {}", user.getUserName());
					return null;
				});
	}

	public List<NearAttractionDTO> getNearByAttractions(VisitedLocation visitedLocation, User user) {
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<NearAttractionDTO> nearAttractionDTOS = new ArrayList<>();

		for (Attraction attraction : attractions) {
			nearAttractionDTOS.add(createNearAttractionDTO(visitedLocation, attraction, user));
		}

        return nearAttractionDTOS.stream()
				.sorted(Comparator.comparingDouble(NearAttractionDTO::getDistance))
				.limit(5)
				.toList();
	}

	private NearAttractionDTO createNearAttractionDTO(VisitedLocation visitedLocation, Attraction attraction, User user) {

        return NearAttractionDTO.builder()
				.attractionName(attraction.attractionName)
				.attractionLocationLatitude(attraction.latitude).
				attractionLocationLongitude(attraction.longitude)
				.userLocationLatitude(visitedLocation.location.latitude)
				.userLocationLongitude(visitedLocation.location.longitude)
				.distance(rewardsService.getDistance(visitedLocation.location, attraction))
				.rewardPoints(rewardsService.getRewardPoints(attraction, user))
				.build();
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(tracker::stopTracking));
	}

	/**********************************************************************************
	 * Methods Below: For Internal Testing
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
        logger.debug("Created {} internal test users.", InternalTestHelper.getInternalUserNumber());
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime())));
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}

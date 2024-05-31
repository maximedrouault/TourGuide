package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Setter;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private final int defaultProximityBuffer = 10;
	@Setter
    private int proximityBuffer = defaultProximityBuffer;
    private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<UserReward> userRewards = new CopyOnWriteArrayList<>(user.getUserRewards());

		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {
				boolean alreadyRewarded = userRewards.stream()
						.anyMatch(userReward -> userReward.attraction.attractionName.equals(attraction.attractionName));

				if(!alreadyRewarded) {
					if(nearAttraction(visitedLocation, attraction)) {
						userRewards.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
				}
			}
		}

		user.setUserRewards(userRewards);
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        int attractionProximityRange = 200;
        return !(getDistance(attraction, location) > attractionProximityRange);
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return !(getDistance(attraction, visitedLocation.location) > proximityBuffer);
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

}

package com.openclassrooms.tourguide.user;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserPreferences {
	
	private int attractionProximity = Integer.MAX_VALUE;
	private int tripDuration = 1;
	private int ticketQuantity = 1;
	private int numberOfAdults = 1;
	private int numberOfChildren = 0;
}

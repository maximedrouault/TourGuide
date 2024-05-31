package com.openclassrooms.tourguide.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class NearAttractionDTO {

    private String attractionName;
    private double attractionLocationLatitude;
    private double attractionLocationLongitude;
    private double userLocationLatitude;
    private double userLocationLongitude;
    // Distance inMiles between userLocation and attraction
    private double distance;
    private int rewardPoints;
}
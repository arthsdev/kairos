package br.com.artheus.kairos.weather;

import java.util.List;

public record GeocodingResponse(List<GeocodingResult> results) {}
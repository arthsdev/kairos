package br.com.artheus.kairos.climate;

import java.util.List;

public record GeocodingResponse(List<GeocodingResult> results) {}
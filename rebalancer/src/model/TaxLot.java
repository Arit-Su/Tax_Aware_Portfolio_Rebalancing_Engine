package model;

//import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

//package com.rebalancing.engine.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single purchase of an asset at a specific price and date.
 * This record is immutable, which is a best practice for data objects.
 *
 * @param purchaseDate The date the asset was purchased.
 * @param purchasePrice The price per unit at which the asset was purchased.
 * @param quantity The number of units purchased.
 */
public record TaxLot(
    LocalDate purchaseDate,
    BigDecimal purchasePrice,
    BigDecimal quantity
) {
    public TaxLot {
        if (purchaseDate == null || purchasePrice == null || quantity == null) {
            throw new IllegalArgumentException("Tax lot arguments cannot be null.");
        }
        if (purchasePrice.signum() < 0 || quantity.signum() < 0) {
            throw new IllegalArgumentException("Price and quantity cannot be negative.");
        }
    }
}
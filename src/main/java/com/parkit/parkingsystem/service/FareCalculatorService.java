package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.time.Duration;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inMilliseconde = ticket.getInTime().getTime();
        long outMilliseconde = ticket.getOutTime().getTime();

        // Calcul de la durée en minutes
        double durationInMinutes = (outMilliseconde - inMilliseconde) / (1000.0* 60);

        // Si la durée est inférieure à 30 minutes, les prix est 0
        if (durationInMinutes < 30) {
            ticket.setPrice(0.0);
            return;
        }
         // Conversion de la durée en heures
        double durationInHours = durationInMinutes / 60;




        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(durationInHours * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(durationInHours * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        if (discount) {
            ticket.setPrice(ticket.getPrice() * 0.95);
        }
    }

    public void calculateFare (Ticket ticket) {
        calculateFare(ticket,false);
    }
}
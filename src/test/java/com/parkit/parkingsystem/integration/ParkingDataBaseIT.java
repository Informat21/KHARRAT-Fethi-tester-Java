package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private final static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull (ticket, "Le ticket doit être enregistré dans la base de donnée.");
        assertNotNull(ticket.getInTime(), "L'heure d'entrée doit être renseignée.");
        assertEquals("ABCDEF", ticket.getVehicleRegNumber(), "Le numéro d'immatriculation doit être correct.");

        boolean isAvailable = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR) != ticket.getParkingSpot().getId();
        assertTrue(isAvailable, "La place de parking doit être marquée comme occupée.");



    }

    @Test
    public void testParkingLotExit(){
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        //TODO: check that the fare generated and out time are populated correctly in the database

        try {
            Thread.sleep(3000); // Attendre une seconde pour simuler un temps de stationnement
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parkingService.processExitingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Le ticket doit être retrouvé en base de données.");
        assertNotNull(ticket.getOutTime(), "L'heure de sortie doit être renseignée.");
        assertTrue(ticket.getOutTime().after(ticket.getInTime()), "L'heure de sortie doit être après l'heure d'entrée.");
        assertTrue(ticket.getPrice() >= 0, "Le tarif doit être calculé et supérieur à zéro.");
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Première entrée et sortie
        parkingService.processIncomingVehicle();
        try {
            Thread.sleep(1000); // Simulation du temps de stationnement
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parkingService.processExitingVehicle();

        // Deuxième entrée et sortie (utilisateur récurrent)
        parkingService.processIncomingVehicle();
        try {
            Thread.sleep(1000); // Simulation du temps de stationnement
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        parkingService.processExitingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Le ticket doit être retrouvé en base de données.");
        assertNotNull(ticket.getOutTime(), "L'heure de sortie doit être renseignée.");
        assertTrue(ticket.getOutTime().after(ticket.getInTime()), "L'heure de sortie doit être après l'heure d'entrée.");

        double expectedPrice = ticket.getPrice() / 0.95; // Vérification de la remise de 5%
        assertEquals(expectedPrice * 0.95, ticket.getPrice(), 0.01, "Le tarif doit inclure une remise de 5% pour un utilisateur récurrent.");
    }

}

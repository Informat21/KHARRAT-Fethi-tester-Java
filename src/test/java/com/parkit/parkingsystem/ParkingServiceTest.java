package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;


    @BeforeEach
    public void setUpPerTest() throws Exception {
        reset(inputReaderUtil, ticketDAO, parkingSpotDAO);
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        lenient().when(ticketDAO.getTicket(anyString())).thenReturn(new Ticket());
        lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }
    @Test
    public void processExitingVehicleTest() {
        // Mock de la méthode getTicket
        Ticket ticket = new Ticket();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false); // Exemple de spot de parking
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 heure avant
        ticket.setOutTime(null); // Assurez-vous que outTime est null au départ

        // Configuration du mock pour retourner ce ticket
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);

        // Simuler une mise à jour réussie du ticket
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        // Simuler l'appel à updateParking
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        // Appel de la méthode à tester
        parkingService.processExitingVehicle();

        // Vérification des interactions
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).getTicket("ABCDEF");
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
    }

    @Test
    public void testProcessIncomingVehicle() {
        try {
            // Mock des méthodes nécessaires
            when(inputReaderUtil.readSelection()).thenReturn(1); // Type de véhicule : CAR
            when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

            ParkingSpot expectedParkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            when(parkingSpotDAO.updateParking(expectedParkingSpot)).thenReturn(true);

            // Appel de la méthode à tester
            parkingService.processIncomingVehicle();

            // Vérification des interactions
            verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
            verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
            verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
            verify(ticketDAO, times(1)).getNbTicket("ABCDEF");

        } catch (Exception e) {
            fail("Exception not expected: " + e.getMessage());
        }
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() {
        try {
            // Simulation de la récupération d'un ticket existant
            Ticket ticket = new Ticket();
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 heure avant
            ticket.setOutTime(new Date(System.currentTimeMillis())); // Simulation d'une sortie avec la date actuelle

            when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
            when(ticketDAO.updateTicket(ticket)).thenReturn(false); // Simule un échec d'update
            when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1); // Cas d'un utilisateur non récurrent

            // Appel de la méthode à tester
            parkingService.processExitingVehicle();

            // Vérification des interactions
            verify(ticketDAO, times(1)).getTicket("ABCDEF");
            verify(ticketDAO, times(1)).updateTicket(ticket); // Vérifie que l'update est tenté
            verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class)); // Ne doit pas être appelé si updateTicket échoue

        } catch (Exception e) {
            fail("Exception not expected: " + e.getMessage());
        }
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {


            // Mock de l'entrée utilisateur pour sélectionner un véhicule (par exemple, "1" pour CAR)
            when(inputReaderUtil.readSelection()).thenReturn(1);  // Simule que l'utilisateur a choisi "1"

            // Mock de la méthode getNextAvailableSlot pour retourner un parking avec ID 1
            when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

            // Appel de la méthode à tester
            ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

            // Vérifications
            assertNotNull(parkingSpot); // S'assurer que le parkingSpot n'est pas nul
            assertEquals(1, parkingSpot.getId()); // Vérifier que l'ID est 1
            assertTrue(parkingSpot.isAvailable()); // Vérifier que le parking est disponible

    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        // Préparer le scénario où il n'y a pas de places de parking disponibles.
        // Supposons que la méthode getNextParkingNumberIfAvailable cherche un spot disponible et que
        // tous les spots sont occupés, donc elle doit retourner null.

        // Simuler que tous les spots sont occupés.
        lenient().when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(-1);  // Aucune place disponible

        // Appeler la méthode à tester.
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // Vérifier que le résultat est bien null, ce qui signifie aucun parking disponible.
        assertNull(parkingSpot, "The parking spot should be null when no spots are available");
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        // Simuler un mauvais type de véhicule (par exemple, 3 pour un type non défini)
        try {
            when(inputReaderUtil.readSelection()).thenReturn(3); // Type de véhicule invalide
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Appeler la méthode à tester
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        // Vérifier que le résultat est bien null
        assertNull(result, "The parking spot should be null for an invalid vehicle type");

        // Vérifier que la méthode DAO n'est jamais appelée (aucun slot ne devrait être recherché)
        verify(parkingSpotDAO, never()).getNextAvailableSlot(any());
    }
    @Test
    public void testProcessIncomingVehicle_NoAvailableSlot() {
        lenient().when(parkingSpotDAO.getNextAvailableSlot(any())).thenReturn(-1);
        parkingService.processIncomingVehicle();
        verify(ticketDAO, never()).saveTicket(any());
    }

    @Test
    public void testProcessExitingVehicle_NoTicketFound() {
        when(ticketDAO.getTicket(anyString())).thenReturn(null);
        parkingService.processExitingVehicle();
        verify(ticketDAO, never()).updateTicket(any());
        verify(parkingSpotDAO, never()).updateParking(any());
    }

    @Test
    public void testGetNextParkingNumberIfAvailable_ExceptionHandling() {
        lenient().when(parkingSpotDAO.getNextAvailableSlot(any())).thenThrow(new RuntimeException("DB Error"));
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();
        assertNull(parkingSpot);
    }

    @Test
    public void testGetVehichleType_InvalidInput() {
        when(inputReaderUtil.readSelection()).thenReturn(3);
        assertThrows(IllegalArgumentException.class, () -> parkingService.getVehichleType());
    }

}


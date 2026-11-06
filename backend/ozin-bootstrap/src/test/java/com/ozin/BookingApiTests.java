package com.ozin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorre las historias de usuario a través de HTTP contra un PostgreSQL real. */
class BookingApiTests extends ApiTestSupport {

    private static final String MONDAY = "2026-11-09";
    private static final String NEXT_MONDAY = "2026-11-16";

    private String haircut;
    private TestStylist ana;
    private TestCustomer customer;

    @BeforeEach
    void givenASalon() throws Exception {
        haircut = createService("Corte", 45, "12.00");
        ana = createStylist(haircut);
        customer = createCustomer();
    }

    private String bookedAppointment(String localStart) throws Exception {
        return createdId(book(customer, ana.id(), haircut, localStart));
    }

    private ResultActions availability(String date) throws Exception {
        return mockMvc.perform(get("/api/v1/stylists/{id}/availability", ana.id())
                .param("serviceId", haircut).param("date", date));
    }

    private ResultActions cancel(String appointment, String token) throws Exception {
        return postJson("/api/v1/appointments/" + appointment + "/cancel", "", token);
    }

    @Nested
    class Catalog {

        @Test
        void hu01_showsOnlyActiveServicesToVisitors() throws Exception {
            putJson("/api/v1/services/" + createService("Tinte", 90, "35.00"), """
                    {"name": "Tinte", "durationMinutes": 90, "price": 35.00, "active": false}
                    """, adminToken).andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/services"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Corte"))
                    .andExpect(jsonPath("$[0].durationMinutes").value(45))
                    .andExpect(jsonPath("$[0].price").value(12.00))
                    .andExpect(jsonPath("$[0].currency").value("USD"));

            getWith(adminToken, "/api/v1/services/manage").andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void exposesTheDefaultBranchWithItsTimeZone() throws Exception {
            mockMvc.perform(get("/api/v1/branches"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(DEFAULT_BRANCH))
                    .andExpect(jsonPath("$[0].timeZone").value("America/Guayaquil"));
        }
    }

    @Nested
    class Availability {

        @Test
        void hu02_interpretsWorkingHoursInTheBranchTimeZone() throws Exception {
            availability(MONDAY)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(28)))
                    .andExpect(jsonPath("$[0].start").value("2026-11-09T14:00:00Z"))   // 09:00 en Ecuador
                    .andExpect(jsonPath("$[27].end").value("2026-11-09T23:00:00Z"));   // 18:00 en Ecuador
        }

        @Test
        void hu02_bookedSlotsDisappear() throws Exception {
            bookedAppointment(MONDAY + "T10:00");

            availability(MONDAY).andExpect(jsonPath("$", hasSize(28 - 5)));
        }

        @Test
        void hu12_timeOffRemovesSlots() throws Exception {
            postJson("/api/v1/stylists/" + ana.id() + "/time-offs", """
                    {"start": "%s", "end": "%s", "reason": "Cita médica"}
                    """.formatted(at(MONDAY + "T09:00"), at(MONDAY + "T13:00")), adminToken)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.timeOffs[0].reason").value("Cita médica"));

            availability(MONDAY).andExpect(jsonPath("$", hasSize(14)));
        }

        @Test
        void hu12_rejectsOverlappingTimeOff() throws Exception {
            String body = """
                    {"start": "%s", "end": "%s"}
                    """.formatted(at(MONDAY + "T09:00"), at(MONDAY + "T13:00"));
            postJson("/api/v1/stylists/" + ana.id() + "/time-offs", body, adminToken).andExpect(status().isCreated());

            postJson("/api/v1/stylists/" + ana.id() + "/time-offs", body, adminToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    class Booking {

        @Test
        void hu03_createsPendingAppointmentWithFrozenPrice() throws Exception {
            book(customer, ana.id(), haircut, MONDAY + "T10:00")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.customerId").value(customer.id()))
                    .andExpect(jsonPath("$.price").value(12.00))
                    .andExpect(jsonPath("$.branchId").value(DEFAULT_BRANCH))
                    .andExpect(jsonPath("$.start").value("2026-11-09T15:00:00Z"))
                    .andExpect(jsonPath("$.end").value("2026-11-09T15:45:00Z"));
        }

        @Test
        void hu03_rejectsADoubleBookingWith409() throws Exception {
            bookedAppointment(MONDAY + "T10:00");

            book(createCustomer(), ana.id(), haircut, MONDAY + "T10:30")
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.code").value("RN-01"));
        }

        @Test
        void rn04_rejectsTooShortNoticeWith422() throws Exception {
            clock.set(at(MONDAY + "T09:30"));

            book(customer, ana.id(), haircut, MONDAY + "T10:00")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("RN-04"));
        }

        @Test
        void rn08_rejectsTheFourthActiveAppointment() throws Exception {
            bookedAppointment(MONDAY + "T09:00");
            bookedAppointment(MONDAY + "T10:00");
            bookedAppointment(MONDAY + "T11:00");

            book(customer, ana.id(), haircut, MONDAY + "T14:00")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("RN-08"));
        }

        @Test
        void rn08_pastAppointmentsDoNotCountTowardsTheLimit() throws Exception {
            bookedAppointment(MONDAY + "T09:00");
            bookedAppointment(MONDAY + "T10:00");
            bookedAppointment(MONDAY + "T11:00");
            clock.set(at(MONDAY + "T12:30"));

            book(customer, ana.id(), haircut, MONDAY + "T14:00").andExpect(status().isCreated());
        }

        @Test
        void rn09_keepsTheOriginalPriceAfterTheCatalogChanges() throws Exception {
            bookedAppointment(MONDAY + "T10:00");

            putJson("/api/v1/services/" + haircut, """
                    {"name": "Corte", "durationMinutes": 45, "price": 20.00, "active": true}
                    """, adminToken).andExpect(status().isOk());

            getWith(customer.token(), "/api/v1/customers/me/appointments")
                    .andExpect(jsonPath("$.items[0].price").value(12.00));
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void hu05_customerCanCancelInTimeAndTheSlotBecomesFree() throws Exception {
            String appointment = bookedAppointment(NEXT_MONDAY + "T10:00");

            cancel(appointment, customer.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            book(createCustomer(), ana.id(), haircut, NEXT_MONDAY + "T10:00").andExpect(status().isCreated());
        }

        @Test
        void rn07_customerCannotCancelLate_butAdminCan() throws Exception {
            String appointment = bookedAppointment(NEXT_MONDAY + "T10:00");
            clock.set(at(NEXT_MONDAY + "T09:00"));

            cancel(appointment, customer.token())
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("RN-07"));

            cancel(appointment, adminToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        void hu09_stylistTakesTheAppointmentFromPendingToCompleted() throws Exception {
            String appointment = bookedAppointment(MONDAY + "T10:00");

            postJson("/api/v1/appointments/" + appointment + "/confirm", "", ana.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            postJson("/api/v1/appointments/" + appointment + "/complete", "", ana.token())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

            clock.set(at(MONDAY + "T10:45"));
            postJson("/api/v1/appointments/" + appointment + "/complete", "", ana.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void hu09_confirmedAppointmentCanBeMarkedAsNoShow() throws Exception {
            String appointment = bookedAppointment(MONDAY + "T10:00");
            postJson("/api/v1/appointments/" + appointment + "/confirm", "", ana.token()).andExpect(status().isOk());
            clock.set(at(MONDAY + "T10:20"));

            postJson("/api/v1/appointments/" + appointment + "/no-show", "", ana.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("NO_SHOW"));
        }
    }

    @Nested
    class Queries {

        @Test
        void hu04_listsCustomerAppointmentsWithNamesAndPagination() throws Exception {
            bookedAppointment(MONDAY + "T09:00");
            bookedAppointment(MONDAY + "T10:00");

            getWith(customer.token(), get("/api/v1/customers/me/appointments").param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(2))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].start").value("2026-11-09T15:00:00Z"))
                    .andExpect(jsonPath("$.items[0].stylistName").value("Ana Pérez"))
                    .andExpect(jsonPath("$.items[0].serviceName").value("Corte"));
        }

        @Test
        void hu08_stylistAgendaIsChronologicalAndLimitedToTheRange() throws Exception {
            bookedAppointment(MONDAY + "T10:00");
            bookedAppointment(MONDAY + "T09:00");
            bookedAppointment(NEXT_MONDAY + "T09:00");

            getWith(ana.token(), get("/api/v1/stylists/{id}/agenda", ana.id())
                    .param("from", at(MONDAY + "T00:00").toString())
                    .param("to", at("2026-11-10T00:00").toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].start").value("2026-11-09T14:00:00Z"))
                    .andExpect(jsonPath("$[0].customerName").value("Cliente 1"));
        }

        @Test
        void hu13_branchAgendaRejectsRangesLongerThan31Days() throws Exception {
            bookedAppointment(MONDAY + "T10:00");

            getWith(adminToken, get("/api/v1/branches/{id}/agenda", DEFAULT_BRANCH)
                    .param("from", at(MONDAY + "T00:00").toString())
                    .param("to", at("2026-11-10T00:00").toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            getWith(adminToken, get("/api/v1/branches/{id}/agenda", DEFAULT_BRANCH)
                    .param("from", at(MONDAY + "T00:00").toString())
                    .param("to", at("2026-12-31T00:00").toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Stylists {

        @Test
        void hu11_updateReplacesServicesAndWorkingHours() throws Exception {
            String coloring = createService("Tinte", 90, "35.00");

            putJson("/api/v1/stylists/" + ana.id(), """
                    {"fullName": "Ana María", "active": true, "serviceIds": ["%s"],
                     "workingHours": [{"day": "TUESDAY", "start": "10:00", "end": "12:00"}]}
                    """.formatted(coloring), adminToken).andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/stylists").param("serviceId", coloring))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].fullName").value("Ana María"));

            mockMvc.perform(get("/api/v1/stylists").param("serviceId", haircut))
                    .andExpect(jsonPath("$", hasSize(0)));

            getWith(adminToken, "/api/v1/stylists/manage")
                    .andExpect(jsonPath("$[0].workingHours", hasSize(1)))
                    .andExpect(jsonPath("$[0].workingHours[0].day").value("TUESDAY"));
        }

        @Test
        void hu11_inactiveStylistIsHiddenAndCannotBeBooked() throws Exception {
            putJson("/api/v1/stylists/" + ana.id(), """
                    {"fullName": "Ana Pérez", "active": false, "serviceIds": ["%s"], "workingHours": %s}
                    """.formatted(haircut, mondayShifts()), adminToken).andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/stylists")).andExpect(jsonPath("$", hasSize(0)));
            book(customer, ana.id(), haircut, MONDAY + "T10:00")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("STYLIST_INACTIVE"));
        }

        @Test
        void rejectsUnknownServicesOnCreate() throws Exception {
            postJson("/api/v1/stylists", """
                    {"branchId": "%s", "fullName": "Luis", "email": "luis@example.com", "password": "%s",
                     "serviceIds": ["00000000-0000-0000-0000-00000000dead"], "workingHours": []}
                    """.formatted(DEFAULT_BRANCH, PASSWORD), adminToken)
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Errors {

        @Test
        void missingFieldsAreReportedOneByOne() throws Exception {
            postJson("/api/v1/appointments", """
                    {"serviceId": "%s"}
                    """.formatted(haircut), customer.token())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.stylistId").exists())
                    .andExpect(jsonPath("$.errors.start").exists());
        }

        @Test
        void unknownResourcesReturn404() throws Exception {
            book(customer, "00000000-0000-0000-0000-00000000dead", haircut, MONDAY + "T10:00")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

            postJson("/api/v1/appointments/00000000-0000-0000-0000-00000000dead/confirm", "", adminToken)
                    .andExpect(status().isNotFound());
        }

        @Test
        void malformedRequestsReturn400() throws Exception {
            mockMvc.perform(get("/api/v1/stylists/not-a-uuid/availability")
                            .param("serviceId", haircut).param("date", MONDAY))
                    .andExpect(status().isBadRequest());

            postJson("/api/v1/auth/register", "{not json").andExpect(status().isBadRequest());
        }

        @Test
        void rejectsPricesWithTooManyDecimals() throws Exception {
            postJson("/api/v1/services", """
                    {"name": "Barba", "durationMinutes": 15, "price": 5.005}
                    """, adminToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}

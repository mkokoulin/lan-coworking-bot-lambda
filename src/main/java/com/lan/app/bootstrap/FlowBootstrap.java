package com.lan.app.bootstrap;

import com.lan.app.flows.about.AboutFlowRegistrar;
import com.lan.app.flows.coworking.CoworkingFlowRegistrar;
import com.lan.app.flows.donation.DonationFlowRegistrar;
import com.lan.app.flows.eventconfirm.EventConfirmFlowRegistrar;
import com.lan.app.flows.help.HelpFlowRegistrar;
import com.lan.app.flows.kotolog.KotologFlowRegistrar;
import com.lan.app.flows.language.LanguageFlowRegistrar;
import com.lan.app.flows.meetingroom.MeetingFlowRegistrar;
import com.lan.app.flows.registration.RegistrationFlowRegistrar;
import com.lan.app.flows.start.StartFlowRegistrar;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class FlowBootstrap {

    private final StartFlowRegistrar startFlowRegistrar;
    private final CoworkingFlowRegistrar coworkingFlowRegistrar;
    private final KotologFlowRegistrar kotologFlowRegistrar;
    private final MeetingFlowRegistrar meetingFlowRegistrar;
    private final AboutFlowRegistrar aboutFlowRegistrar;
    private final LanguageFlowRegistrar languageFlowRegistrar;
    private final DonationFlowRegistrar donationFlowRegistrar;
    private final RegistrationFlowRegistrar registrationFlowRegistrar;
    private final EventConfirmFlowRegistrar eventConfirmFlowRegistrar;
    private final HelpFlowRegistrar helpFlowRegistrar;

    @Inject
    public FlowBootstrap(
        StartFlowRegistrar startFlowRegistrar,
        CoworkingFlowRegistrar coworkingFlowRegistrar,
        KotologFlowRegistrar kotologFlowRegistrar,
        MeetingFlowRegistrar meetingFlowRegistrar,
        AboutFlowRegistrar aboutFlowRegistrar,
        LanguageFlowRegistrar languageFlowRegistrar,
        DonationFlowRegistrar donationFlowRegistrar,
        RegistrationFlowRegistrar registrationFlowRegistrar,
        EventConfirmFlowRegistrar eventConfirmFlowRegistrar,
        HelpFlowRegistrar helpFlowRegistrar
    ) {
        this.startFlowRegistrar = startFlowRegistrar;
        this.coworkingFlowRegistrar = coworkingFlowRegistrar;
        this.kotologFlowRegistrar = kotologFlowRegistrar;
        this.meetingFlowRegistrar = meetingFlowRegistrar;
        this.aboutFlowRegistrar = aboutFlowRegistrar;
        this.languageFlowRegistrar = languageFlowRegistrar;
        this.donationFlowRegistrar = donationFlowRegistrar;
        this.registrationFlowRegistrar = registrationFlowRegistrar;
        this.eventConfirmFlowRegistrar = eventConfirmFlowRegistrar;
        this.helpFlowRegistrar = helpFlowRegistrar;
    }

    void onStart(@Observes StartupEvent event) {
        startFlowRegistrar.register();
        coworkingFlowRegistrar.register();
        kotologFlowRegistrar.register();
        meetingFlowRegistrar.register();
        aboutFlowRegistrar.register();
        languageFlowRegistrar.register();
        donationFlowRegistrar.register();
        registrationFlowRegistrar.register();
        eventConfirmFlowRegistrar.register();
        helpFlowRegistrar.register();
    }
}

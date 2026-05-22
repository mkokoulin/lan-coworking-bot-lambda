package com.lan.app.bootstrap;

import com.lan.app.flows.about.AboutFlowRegistrar;
import com.lan.app.flows.news.NewsFlowRegistrar;
import com.lan.app.flows.review.ReviewFlowRegistrar;
import com.lan.app.flows.coworking.CoworkingFlowRegistrar;
import com.lan.app.flows.cwlink.CwLinkFlowRegistrar;
import com.lan.app.flows.wifi.WifiFlowRegistrar;
import com.lan.app.flows.donation.DonationFlowRegistrar;
import com.lan.app.flows.eventconfirm.EventConfirmFlowRegistrar;
import com.lan.app.flows.eventpayment.EventPaymentFlowRegistrar;
import com.lan.app.flows.help.HelpFlowRegistrar;
import com.lan.app.flows.kotolog.KotologFlowRegistrar;
import com.lan.app.flows.language.LanguageFlowRegistrar;
import com.lan.app.flows.meetingroom.MeetingFlowRegistrar;
import com.lan.app.flows.myevents.MyEventsFlowRegistrar;
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
    private final MyEventsFlowRegistrar myEventsFlowRegistrar;
    private final HelpFlowRegistrar helpFlowRegistrar;
    private final CwLinkFlowRegistrar cwLinkFlowRegistrar;
    private final WifiFlowRegistrar wifiFlowRegistrar;
    private final ReviewFlowRegistrar reviewFlowRegistrar;
    private final NewsFlowRegistrar newsFlowRegistrar;
    private final EventPaymentFlowRegistrar eventPaymentFlowRegistrar;

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
        MyEventsFlowRegistrar myEventsFlowRegistrar,
        HelpFlowRegistrar helpFlowRegistrar,
        CwLinkFlowRegistrar cwLinkFlowRegistrar,
        WifiFlowRegistrar wifiFlowRegistrar,
        ReviewFlowRegistrar reviewFlowRegistrar,
        NewsFlowRegistrar newsFlowRegistrar,
        EventPaymentFlowRegistrar eventPaymentFlowRegistrar
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
        this.myEventsFlowRegistrar = myEventsFlowRegistrar;
        this.helpFlowRegistrar = helpFlowRegistrar;
        this.cwLinkFlowRegistrar = cwLinkFlowRegistrar;
        this.wifiFlowRegistrar = wifiFlowRegistrar;
        this.reviewFlowRegistrar = reviewFlowRegistrar;
        this.newsFlowRegistrar = newsFlowRegistrar;
        this.eventPaymentFlowRegistrar = eventPaymentFlowRegistrar;
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
        myEventsFlowRegistrar.register();
        helpFlowRegistrar.register();
        cwLinkFlowRegistrar.register();
        wifiFlowRegistrar.register();
        reviewFlowRegistrar.register();
        newsFlowRegistrar.register();
        eventPaymentFlowRegistrar.register();
    }
}

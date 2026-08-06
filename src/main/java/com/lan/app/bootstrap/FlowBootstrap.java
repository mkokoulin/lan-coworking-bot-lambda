package com.lan.app.bootstrap;

import com.lan.app.flows.about.AboutFlowRegistrar;
import com.lan.app.flows.menu.MenuFlowRegistrar;
import com.lan.app.flows.cwbooking.CwBookingFlowRegistrar;
import com.lan.app.flows.news.NewsFlowRegistrar;
import com.lan.app.flows.review.ReviewFlowRegistrar;
import com.lan.app.flows.coworking.CoworkingFlowRegistrar;
import com.lan.app.flows.cwlink.CwLinkFlowRegistrar;
import com.lan.app.flows.wifi.WifiFlowRegistrar;
import com.lan.app.flows.donation.DonationFlowRegistrar;
import com.lan.app.flows.eventchange.EventChangeFlowRegistrar;
import com.lan.app.flows.eventconfirm.EventConfirmFlowRegistrar;
import com.lan.app.flows.eventnotify.EventNotifyFlowRegistrar;
import com.lan.app.flows.eventpayment.EventPaymentFlowRegistrar;
import com.lan.app.flows.eventslist.EventsListFlowRegistrar;
import com.lan.app.flows.help.HelpFlowRegistrar;
import com.lan.app.flows.kotolog.KotologFlowRegistrar;
import com.lan.app.flows.language.LanguageFlowRegistrar;
import com.lan.app.flows.meetingroom.MeetingFlowRegistrar;
import com.lan.app.flows.printout.PrintFlowRegistrar;
import com.lan.app.flows.myevents.MyEventsFlowRegistrar;
import com.lan.app.flows.registration.RegistrationFlowRegistrar;
import com.lan.app.flows.start.StartFlowRegistrar;
import com.lan.app.flows.tariffs.TariffsFlowRegistrar;
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
    private final EventChangeFlowRegistrar eventChangeFlowRegistrar;
    private final EventNotifyFlowRegistrar eventNotifyFlowRegistrar;
    private final MyEventsFlowRegistrar myEventsFlowRegistrar;
    private final EventsListFlowRegistrar eventsListFlowRegistrar;
    private final HelpFlowRegistrar helpFlowRegistrar;
    private final CwLinkFlowRegistrar cwLinkFlowRegistrar;
    private final WifiFlowRegistrar wifiFlowRegistrar;
    private final ReviewFlowRegistrar reviewFlowRegistrar;
    private final NewsFlowRegistrar newsFlowRegistrar;
    private final TariffsFlowRegistrar tariffsFlowRegistrar;
    private final EventPaymentFlowRegistrar eventPaymentFlowRegistrar;
    private final CwBookingFlowRegistrar cwBookingFlowRegistrar;
    private final PrintFlowRegistrar printFlowRegistrar;
    private final MenuFlowRegistrar menuFlowRegistrar;

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
        EventChangeFlowRegistrar eventChangeFlowRegistrar,
        EventNotifyFlowRegistrar eventNotifyFlowRegistrar,
        MyEventsFlowRegistrar myEventsFlowRegistrar,
        EventsListFlowRegistrar eventsListFlowRegistrar,
        HelpFlowRegistrar helpFlowRegistrar,
        CwLinkFlowRegistrar cwLinkFlowRegistrar,
        WifiFlowRegistrar wifiFlowRegistrar,
        ReviewFlowRegistrar reviewFlowRegistrar,
        NewsFlowRegistrar newsFlowRegistrar,
        TariffsFlowRegistrar tariffsFlowRegistrar,
        EventPaymentFlowRegistrar eventPaymentFlowRegistrar,
        CwBookingFlowRegistrar cwBookingFlowRegistrar,
        PrintFlowRegistrar printFlowRegistrar,
        MenuFlowRegistrar menuFlowRegistrar
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
        this.eventChangeFlowRegistrar = eventChangeFlowRegistrar;
        this.eventNotifyFlowRegistrar = eventNotifyFlowRegistrar;
        this.myEventsFlowRegistrar = myEventsFlowRegistrar;
        this.eventsListFlowRegistrar = eventsListFlowRegistrar;
        this.helpFlowRegistrar = helpFlowRegistrar;
        this.cwLinkFlowRegistrar = cwLinkFlowRegistrar;
        this.wifiFlowRegistrar = wifiFlowRegistrar;
        this.reviewFlowRegistrar = reviewFlowRegistrar;
        this.newsFlowRegistrar = newsFlowRegistrar;
        this.tariffsFlowRegistrar = tariffsFlowRegistrar;
        this.eventPaymentFlowRegistrar = eventPaymentFlowRegistrar;
        this.cwBookingFlowRegistrar = cwBookingFlowRegistrar;
        this.printFlowRegistrar = printFlowRegistrar;
        this.menuFlowRegistrar = menuFlowRegistrar;
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
        eventChangeFlowRegistrar.register();
        eventNotifyFlowRegistrar.register();
        // myevents must be registered BEFORE eventslist so that eventslist can override "events" command
        myEventsFlowRegistrar.register();
        eventsListFlowRegistrar.register();
        helpFlowRegistrar.register();
        cwLinkFlowRegistrar.register();
        wifiFlowRegistrar.register();
        reviewFlowRegistrar.register();
        newsFlowRegistrar.register();
        tariffsFlowRegistrar.register();
        eventPaymentFlowRegistrar.register();
        cwBookingFlowRegistrar.register();
        printFlowRegistrar.register();
        menuFlowRegistrar.register();
    }
}

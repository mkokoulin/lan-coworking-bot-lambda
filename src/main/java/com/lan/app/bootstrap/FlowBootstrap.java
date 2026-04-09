package com.lan.app.bootstrap;

import com.lan.app.flows.coworking.CoworkingFlowRegistrar;
import com.lan.app.flows.kotolog.KotologFlowRegistrar;
import com.lan.app.flows.start.StartFlowRegistrar;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class FlowBootstrap {

    @Inject
    StartFlowRegistrar startFlowRegistrar;

    @Inject
    CoworkingFlowRegistrar coworkingFlowRegistrar;

    @Inject
    KotologFlowRegistrar kotologFlowRegistrar;

    void onStart(@Observes StartupEvent event) {
        startFlowRegistrar.register();
        coworkingFlowRegistrar.register();
        kotologFlowRegistrar.register();
    }
}

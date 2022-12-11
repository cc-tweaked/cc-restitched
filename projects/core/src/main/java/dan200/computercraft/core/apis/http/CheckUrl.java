/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.apis.IAPIEnvironment;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.concurrent.Future;

/**
 * Checks a URL using {@link NetworkUtils#getAddress(String, int, boolean)}}
 * <p>
 * This requires a DNS lookup, and so needs to occur off-thread.
 */
public class CheckUrl extends Resource<CheckUrl> {
    private static final String EVENT = "http_check";

    private @Nullable Future<?> future;

    private final IAPIEnvironment environment;
    private final String address;
    private final URI uri;

    public CheckUrl(ResourceGroup<CheckUrl> limiter, IAPIEnvironment environment, String address, URI uri) {
        super(limiter);
        this.environment = environment;
        this.address = address;
        this.uri = uri;
    }

    public void run() {
        if (isClosed()) return;
        future = NetworkUtils.EXECUTOR.submit(this::doRun);
        checkClosed();
    }

    private void doRun() {
        if (isClosed()) return;

        try {
            var ssl = uri.getScheme().equalsIgnoreCase("https");
            var netAddress = NetworkUtils.getAddress(uri, ssl);
            NetworkUtils.getOptions(uri.getHost(), netAddress);

            if (tryClose()) environment.queueEvent(EVENT, address, true);
        } catch (HTTPRequestException e) {
            if (tryClose()) environment.queueEvent(EVENT, address, false, NetworkUtils.toFriendlyError(e));
        }
    }

    @Override
    protected void dispose() {
        super.dispose();
        future = closeFuture(future);
    }
}

/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

/**
 * Queue an event on a {@link ServerComputer}.
 *
 * @see ServerInputHandler#queueEvent(String)
 */
public class QueueEventServerMessage extends ComputerServerMessage {
    private final String event;
    private final @Nullable Object[] args;

    public QueueEventServerMessage(AbstractContainerMenu menu, String event, @Nullable Object[] args) {
        super(menu);
        this.event = event;
        this.args = args;
    }

    public QueueEventServerMessage(FriendlyByteBuf buf) {
        super(buf);
        event = buf.readUtf(Short.MAX_VALUE);

        var args = buf.readNbt();
        this.args = args == null ? null : NBTUtil.decodeObjects(args);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeUtf(event);
        buf.writeNbt(args == null ? null : NBTUtil.encodeObjects(args));
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        container.getInput().queueEvent(event, args);
    }
}
